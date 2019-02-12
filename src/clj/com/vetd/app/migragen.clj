(ns com.vetd.app.migragen
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.db-copier :as cp]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [clojure.string :as st]
            [clojure.java.jdbc :as j]
            [taoensso.timbre :as log]
            [honeysql.core :as hny]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            clojure.edn
            clojure.pprint))


(defn drop-table-if-exists [schema table]
  (format "DROP TABLE IF EXISTS %s.%s;"
          schema
          table))

(defn drop-view-if-exists [schema view]
  (format "DROP VIEW IF EXISTS %s.%s;"
          schema
          view))

(defn convert-numeric-types [col-args]
  (mapv (fn [a]
          (if (and (sequential? a)
                   (-> a first (= :numeric)))
            (let [[_ s p] a]
              (keyword (format "numeric(%d,%d)" s p)))
            a))
        col-args))

(defmulti mk-sql (fn [[op]] op))

(defmethod mk-sql :create-table
  [[_ {:keys [schema columns owner grants] table :name}]]
  (let [schema' (name schema)
        table' (name table)
        owner' (name owner)]
    {:name-part (format "create-%s-table" table')
     :ext "sql"     
     :up (->> (concat [(drop-table-if-exists schema'
                                             table')
                       (j/create-table-ddl (format "%s.%s"
                                                   schema'
                                                   table')
                                           (for [[k v] columns]
                                             (into [k]
                                                   (convert-numeric-types v))))
                       (when owner
                         (format "ALTER TABLE %s.%s OWNER TO %s"
                                 schema'
                                 table'
                                 owner'))]
                      (for [[k vs] grants
                            v      vs]
                        (format "GRANT %s ON TABLE %s.%s TO %s;"
                                (name v)
                                schema'
                                table'
                                (name k))))
              (remove nil?))
     :down [(drop-table-if-exists schema'
                                  table')]}))

(defmethod mk-sql :create-view
  [[_ {:keys [schema honey owner grants] view :name}]]
  (let [schema' (name schema)
        view' (name view)
        owner' (name owner)]
    {:name-part (format "create-%s-view" view')
     :ext "sql"
     :up (->> (concat [(drop-view-if-exists schema'
                                            view')
                       (format "CREATE VIEW %s.%s AS %s;"
                               schema'
                               view'
                               (first
                                (hny/format honey
                                            :allow-dashed-names? true
                                            :quoting :ansi)))
                       (when owner
                         (format "ALTER VIEW %s.%s OWNER TO %s"
                                 schema'
                                 view'
                                 owner'))]
                      (for [[k vs] grants
                            v      vs]
                        (format "GRANT %s ON %s.%s TO %s;"
                                (name v)
                                schema'
                                view'
                                (name k))))
              (remove nil?))
     :down [(drop-view-if-exists schema'
                                 view')]}))

(defmethod mk-sql :copy-from
  [[_ {name-kw :name :as m}]]
  {:name-part (format "copy-from-%s" (name name-kw))
   :ext "edn"
   :both
   [(with-out-str
      (clojure.pprint/with-pprint-dispatch clojure.pprint/code-dispatch
        (-> m (dissoc :name) clojure.pprint/pprint)))]})

(defn mk-migration-file-name
  [path ext idx [yr mo da hr mi] name-part up-down]
  (format "%s/%04d%02d%02d%02d%02d%02d-%s%s.%s"
          path
          yr mo da hr mi idx
          name-part
          (if up-down
            (str "." up-down)
            "")
          ext))

(defn mk-migration-file-contents
  [sqls]
  (clojure.string/join "\n--;;\n"
                       sqls))

(defn mk-migration-file
  [path idx [dtime stmt]]
  (let [{:keys [name-part ext up down both]} (mk-sql stmt)]
    (when up
      (spit (mk-migration-file-name path ext idx dtime name-part "up")
            (mk-migration-file-contents up)))
    (when down
      (spit (mk-migration-file-name path ext idx dtime name-part "down")
            (mk-migration-file-contents down)))
    (when both
      (spit (mk-migration-file-name path ext idx dtime name-part nil)
            (mk-migration-file-contents both)))))

(defn mk-migration-files
  [migrations-def dest-path]
  (map-indexed (partial mk-migration-file
                        (-> dest-path
                            io/resource
                            .getPath))
               (for [[dtime & stmts] migrations-def
                     s stmts]
                 [dtime s])))

(defn mk-copy-from-up-fn [filename]
  (fn [{:keys [db]}]
    (cp/copy-from-sql-dump
     (-> (str "migrations/" filename )
         io/resource
#_         .getPath)
     (j/get-connection db))))

(defn mk-exe-honeysql-fn [hsql]
  (fn [{:keys [db]}]
    (db/hs-exe! hsql
                db)))

(defn mk-hasura-rel
  [schema [rel-name {:keys [rem-tbl col-map]}]]
  {:using
   {:manual_configuration
    {:remote_table {:schema schema :name rem-tbl}
     :column_mapping col-map}}
   :name rel-name
   :comment nil})

(defn mk-hasura-table
  [schema [table {:keys [obj-rel arr-rel ins-per sel-per upd-per del-per evt-trg]}]]
  {:table {:schema schema :name table}
   :object_relationships (mapv (partial mk-hasura-rel schema) obj-rel)
   :array_relationships (mapv (partial mk-hasura-rel schema) arr-rel)
   :insert_permissions (or ins-per [])
   :select_permissions (or sel-per [])
   :update_permissions (or upd-per [])
   :delete_permissions (or del-per [])
   :event_triggers (or evt-trg [])})

(defn mk-hasura-schema
  [[schema tables]]
  (mapv (partial mk-hasura-table schema)
        tables))

(defn proc-hasura-meta-cfg [cfg]
  (spit (-> "hasura" io/resource .getPath (str "/metadata-gen.json"))
        (-> cfg
            (update :tables (partial mapv mk-hasura-schema))
            (update :tables #(->> % (apply concat) vec))
            (json/generate-string))))

(defn kw-kebab->snake [kw]
  (-> kw
      name
      (st/replace #"-" "_")
      keyword))

(defn merge-hasura-rels
  [{obj1 :obj-rel arr1 :arr-rel}
   {obj2 :obj-rel arr2 :arr-rel}]
  {:obj-rel (merge obj1 obj2)
   :arr-rel (merge arr1 arr2)})

(defn apply-hasura-rels
  [agg {:keys [tables fields cols rel]}]
  (let [[s1 t1 s2 t2] tables
        [f1 f2] (map kw-kebab->snake fields)
        [c1 c2] cols
        rel-type1 (if (#{:one-many :many-many} rel)
                    :arr-rel :obj-rel)
        rel-type2 (if (#{:many-one :many-many} rel)
                    :arr-rel :obj-rel)
        agg' (assoc-in agg [s1 t1 rel-type1 f1]
                       {:rem-tbl t2
                        :col-map {c1 c2}})
        agg'' (if f2
                (assoc-in agg' [s2 t2 rel-type2 f2]
                          {:rem-tbl t1
                           :col-map {c2 c1}})
                (update-in agg' [s2 t2]
                          #(or % {})))]
    agg''))

(defn apply-hasura-inheritance*
  [agg [to-sch to-tbl from-sch from-tbl]]
  (assoc-in agg
            [to-sch to-tbl]
            (merge-hasura-rels (get-in agg [from-sch from-tbl])
                               (get-in agg [to-sch to-tbl]))))

(defn apply-hasura-inheritance
  [tables inherits]
  (reduce apply-hasura-inheritance*
          tables
          (for [[[to-sch to-tbl] vs] inherits
                [from-sch from-tbl] vs]
            [to-sch to-tbl from-sch from-tbl])))

(defn proc-hasura-meta-cfg2
  [{:keys [rels inherits] :as cfg}]
  (-> cfg
      (assoc :tables
             (reduce apply-hasura-rels
                     {}
                     rels))
      (dissoc :rels :inherits)
      (update :tables apply-hasura-inheritance inherits)
      proc-hasura-meta-cfg))

#_
(-> "/home/bill/repos/vetd-app/resources/hasura/metadata-gen.json"
    slurp
    json/parse-string
    clojure.pprint/pprint )


















































