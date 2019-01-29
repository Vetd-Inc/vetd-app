(ns com.vetd.app.db
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.env :as env]
            [com.vetd.app.db-schema :as sch]
            [com.vetd.app.path :as path]
            [clojure.java.jdbc :as j]
            [clojure.core.async :as a]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]
            [honeysql.format :as hsfmt]
            [clojure.walk :as w]
            clojure.edn))





;; TODO use Migratus?
;; https://github.com/yogthos/migratus

(def pg-db env/pg-db)

;; for running migrations locally
#_ (def pg-db env/pg-db-prod)

(j/query pg-db ["select 1"])

#_(j/db-do-commands pg-db
                  [(j/create-table-ddl :test1
                                       [[:id "SERIAL" "PRIMARY KEY"]
                                        
                                        [:value :int]])])




(defn restart-seq
  [table start-at]
  (j/db-do-commands  pg-db
                     [
                      (format "ALTER SEQUENCE %s_id_seq RESTART WITH %d;"
                              table start-at)]))



#_(j/insert! pg-db :test1 {:value 4} )


(defmethod hsfmt/fn-handler "~*"
  [_ & args]
  (hsfmt/paren-wrap
   (apply format "%s ~* %s"
          (map hsfmt/to-sql args))))

(defn create-table-if
  [tbl]
  (->> tbl
       (format "
CREATE TABLE IF NOT EXISTS %s (
 id STRING PRIMARY KEY,
 created TIMESTAMP,
 updated TIMESTAMP,
 deleted TIMESTAMP
)")
       (j/execute! pg-db)))

(defn drop-table [tbl]
  (->> tbl
       (format "DROP TABLE IF EXISTS \"%s\" ")
       (j/execute! pg-db)))

(defn truncate-table [tbl]
  (->> tbl
       (format "DELETE FROM \"%s\" ")
       (j/execute! pg-db)))

#_ (create-table-if "test2")

(defn exe! [cmd]
  (try
    (j/execute! pg-db cmd)
    (catch Exception e
      (clojure.pprint/pprint cmd)
      (log/error e)
      (throw e))))

(defn insert!
  [table row]
  (j/insert! pg-db table row {:entities #(format "\"%s\"" %)}))

(defn query [q & [return-ex?]]
#_ (clojure.pprint/pprint q)
  (try
    (j/query pg-db q)
    (catch Exception e
      (def e1 e)
      (clojure.pprint/pprint q)
      (log/error e)
      (if return-ex?
        (.getMessage e)
        (throw e)))))

(defn hs-query [sql-map & [return-ex?]]
  (-> sql-map
      (hs/format :allow-dashed-names? true
                 :quoting :ansi)
      (query return-ex?)))

#_(defn p-query [path-query & [return-ex?]]
  (-> path-query
      (path/->hsql sch/path-cfg)
      (hs-query return-ex?)
      path/process-results))

(defn upsert-schema-rec
  [tbl schema]
  (->  {:delete-from tbl
        :where [:= :id "schema"]}
       hs/format
       exe!)
  (->> schema
       :columns
       sch/schema->prototype
       (insert! tbl))
  (->  {:delete-from tbl
        :where [:= :id "schema"]}
       hs/format
       exe!))

(defn upsert-schema-recs
  [schema]
  (doseq [[k v] schema]
    (upsert-schema-rec k v)))

(defn drop-round-prod-view []
  (j/db-do-commands
   pg-db
   ["DROP VIEW IF EXISTS rounds_by_product;"]))

(defn drop-all [schema]
  (drop-round-prod-view)
  (doseq [k (keys schema)]
    (drop-table (name k))))

#_ (drop-all sch/tables2)

(defn create-round-prod-view []
  (j/db-do-commands
   pg-db
   ["
CREATE VIEW rounds_by_product AS
SELECT 
rp.id rpid, rp.product_id,
r.id, r.created, r.idstr, r.buyer_id, r.status
    FROM round_product rp
    JOIN rounds r on rp.round_id = r.id;
"]))

(defn create-all [schema]
  (doseq [k (keys schema)]
    (create-table-if (name k)))
  (create-round-prod-view))

(defn create-all2 []
  (j/db-do-commands pg-db
                    (for [[t {:keys [columns]}] sch/tables2]
                      (j/create-table-ddl t
                                          (concat [[:id "BIGINT" "PRIMARY KEY"]
                                                   #_[:uuid :uuid]
                                                   [:created :timestamptz]
                                                   [:updated :timestamptz]
                                                   [:deleted :timestamptz]]
                                                  (for [[c ctype] (dissoc columns :id)]
                                                    [c ctype]))
                                          {:entities #(format "\"%s\"" %)})))
  (create-round-prod-view))

(defn migrate []
  (create-all sch/tables2)
  (upsert-schema-recs sch/tables2))

#_ (migrate)
