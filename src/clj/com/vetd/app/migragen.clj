(ns com.vetd.app.migragen
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.db-copier :as cp]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]            
            [clojure.java.jdbc :as j]
            [taoensso.timbre :as log]
            [honeysql.core :as hny]
            [clojure.java.io :as io]
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
                                             (into [k] v)))
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
                                 owner'))])
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
         .getPath)
     (j/get-connection db))))

(defn mk-exe-honeysql-fn [hsql]
  (fn [{:keys [db]}]
    (db/hs-exe! hsql
                db)))

