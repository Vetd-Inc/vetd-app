(ns com.vetd.app.migragen2
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

;; TODO make primary keys

(defn date-vec->int
  [[yr mo da hr mi]]
  (+ (* yr 10000000000)
     (* mo 100000000)
     (* da 1000000)
     (* hr 10000)
     (* mi 100)))

(defn ->mig-steps**
  [idx m]
  (update m :date-int + idx))

(defn ->mig-steps*
  [migs]
  (for [[schema entity & inst-groups] migs
        [date-vec & steps] inst-groups
        step steps]
    {:schema schema
     :entity entity
     :date-int (date-vec->int date-vec)
     :step step}))

(defn ->mig-steps
  [migs]
  (->> migs
       ->mig-steps*
       (sort-by :date-int)
       (partition-by :date-int)
       (mapcat (partial map-indexed ->mig-steps**))))

(defmulti mk-migration-file-contents
  (fn [{[cmd] :step}] cmd))


#_(defmethod mk-migration-file-contents :table&view
  )


(defn mk-migration-files
  [migrations-def dest-path]
  (let [outs (->> migrations-def
                  ->mig-steps
                  (mapcat mk-migration-file-contents))]
    (doseq [[filename contents] outs]
      (spit (str dest-path filename)
            contents))))
