(ns com.vetd.app.db
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [com.vetd.app.env :as env]
            [migratus.core :as mig]
            [clojure.java.jdbc :as j]
            [clj-postgresql.core :as pg]
            [clojure.core.async :as a]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]
            [honeysql.format :as hsfmt]
            ;; simply requiring these allows use of e.g. [:ilike] in honeysql forms
            [honeysql-postgres.format :as hspgfmt]
            [honeysql-postgres.helpers :as hspgh]
            [clojure.walk :as w]
            clojure.edn))

(def ^:dynamic *suppress-error-logging* false)

#_(mig/migrate
   {:store :database
    :db env/pg-db})

#_ (mig/reset {:store :database
               :db env/pg-db})


#_
(dotimes [_ 10]
  (mig/rollback {:store :database
                 :db env/pg-db}))

(def pg-db env/pg-db)

;; for running migrations locally
#_ (def pg-db env/pg-db-prod)

#_(j/query pg-db ["select 1"])


(defmethod hsfmt/fn-handler "~*"
  [_ & args]
  (hsfmt/paren-wrap
   (apply format "%s ~* %s"
          (map hsfmt/to-sql args))))

(defn exe! [cmd & [db]]
  (try
    (j/execute! (or db pg-db) cmd)
    (catch Throwable e
      (when-not *suppress-error-logging*
            (com/log-error e))
      (throw e))))

(defn hs-exe! [hsql & [db]]
  (-> hsql
      (hs/format :allow-dashed-names? true
                 :quoting :ansi)
      (exe! db)))

(defn insert!
  [table row]
  (j/insert! pg-db table row {:entities #(format "\"%s\"" %)}))

(defn query [q & [return-ex?]]
  (when-not env/building?
    (try
      (j/query pg-db q)
      (catch Throwable e
        (when-not *suppress-error-logging*
            (com/log-error e))
        (if return-ex?
          (.getMessage e)
          (throw e))))))

(defn hs-query [sql-map & [return-ex?]]
  (-> sql-map
      (hs/format :allow-dashed-names? true
                 :quoting :ansi)
      (query return-ex?)))

(defn select-distinct-col-names []
  (->> (query "
SELECT DISTINCT column_name c
FROM information_schema.columns 
WHERE table_schema = 'vetd' AND table_catalog = 'vetd1';")
       (mapv :c)))


(defn drop-table [tbl]
  (exe! (format "DROP TABLE \"%s\";"
                tbl)))

(defn drop-view [tbl]
  (exe! (format "DROP VIEW \"%s\";"
                tbl)))

(defn select-all-table-names [schema]
  (->> {:select [[:table_name :table-name]]
        :from [:information_schema.tables]
        :where [:= :table_schema schema]}
       hs-query
       (mapv :table-name)))

(def select-all-table-names-MZ (memoize select-all-table-names))

(defn select-all-view-names [schema]
  (->> {:select [[:table_name :table-name]]
        :from [:information_schema.views]
        :where [:= :table_schema schema]}
       hs-query
       (mapv :table-name)))


(defn drop-all [schema]
  (doseq [v (select-all-view-names schema)]
    (println "dropping view " v)
    (drop-view v))
  (doseq [t (select-all-table-names schema)]
    (println "dropping table " t)    
    (drop-table t)))


#_ (drop-all "vetd")

(defn find-entity-table-by-id [id]
  (when id
    (loop [[head & tail] (select-all-table-names-MZ "vetd")]
      (if head
        (let [r (try
                  ;; I don't love this, but it junks up the logs otherwise -- Bill
                  (binding [*suppress-error-logging* true] 
                    (hs-query {:select [:id]
                               :from [(keyword head)]
                               :where [:= :id id]
                               :limit 1}))
                  (catch Throwable t
                    nil))]
          (if (not-empty r)
            head
            (recur tail)))
        nil))))

(defn update-any! [{:keys [id] :as m} & [table-kw]]
  (if-let [tbl (or table-kw
                   (-> id find-entity-table-by-id keyword))]
    (hs-exe! {:update tbl
              :set (-> m
                       (assoc :updated (ut/now-ts))
                       (dissoc :id :idstr :created))
              :where [:= :id id]})
    (throw (Exception. (format "Could not find entity with id %s." id)))))

(defn update-deleted [tbl-kw id]
  (hs-exe! {:update tbl-kw
            :set {:deleted (ut/now-ts)}
            :where [:= :id id]}))

(defn update-deleted-where
  [tbl-kw hny-where]
  (hs-exe! {:update tbl-kw
            :set {:deleted (ut/now-ts)}
            :where hny-where}))

(defn update-where
  [tbl-kw hny-set hny-where]
  (hs-exe! {:update tbl-kw
            :set (assoc hny-set
                        :updated (ut/now-ts))
            :where hny-where}))
