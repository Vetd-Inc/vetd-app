(ns db-sync-test
  (:require [com.vetd.app.db-sync :as dbs]
            [clojure.test :as t]
            [com.vetd.app.db :as db]))

(def test-schema :db_schema1)

(t/deftest test1
  ;; reset db / schema
  (db/exe! (str "DROP SCHEMA IF EXISTS " (name test-schema)) )
  (let [target {:schemas [{:name test-schema
                           :tables [{:name :table1
                                     :columns [[:id :bigint :NOT :NULL]]}]}]}]
    (dbs/diff db target) ;; test
    (dbs/sync db target)
    (dbs/diff db target) ;; test is empty
    ;; compare actual schema to expected
    ;; cleanup
(comment)))
















































