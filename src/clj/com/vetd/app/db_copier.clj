(ns com.vetd.app.db-copier
  (:require [com.vetd.app.env :as env]
   [clojure.java.jdbc :as j]
            [clojure.java.io :as io]
            [clojure.string :as st]
            [taoensso.timbre :as log]
            clojure.pprint)
  (:import org.postgresql.copy.CopyManager
           [java.io Writer]
           [java.nio ByteBuffer]))


;; https://clojuredocs.org/clojure.core/slurp
(defn slurp-bytes [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(defn copy-from
  [data copier] ;; TODO hints
  (try
    (do
      (doseq [d data]
        (.writeToCopy copier d 0 (count d)))
      (.endCopy copier))
    true
    (catch Exception e
      (log/error e "copy-from FAILed")
      false)))

#_(defn copy-from-slurp [source copier]
  (copy-from (slurp-bytes source)
             copier))

(defn mk-copier
  [conn table & [{:keys [fmt delim qt]}]]
  (.copyIn (CopyManager. conn)
           (format "COPY \"%s\" FROM STDIN (FORMAT %s, DELIMITER '%s', QUOTE '%s')"
                   table
                   (or fmt "csv")
                   (or delim "\\t")
                   (or qt "\""))))

(defn mk-copier-from-stmt [s conn]
  (.copyIn (CopyManager. conn) s))

#_(defn lines->ba [sv]
  (let [size (->> sv
                  (map count)
                  (reduce +))
        ba (byte-array (+ size (count sv)))
        bb (ByteBuffer/wrap ba)]
    (doseq [s sv]
      (.put bb (.getBytes s))
      (.put bb (byte 10)))
    ba))

(defn lines->ba-vec [sv]
  (map (fn [s]
         (let [bs (.getBytes s)
               size (-> bs count inc)
               ba (byte-array size)
               bb (ByteBuffer/wrap ba)]
           (.put bb bs)
           (.put bb (byte 10))
           ba))
       sv))

(defn copy-from-sql-dump [in conn]
  (time
   (let [[copy-stmt & data] (->> in
                                 slurp
                                 st/split-lines)]
     (doseq [ba-vec (partition-all 1000 (lines->ba-vec data))]
       (copy-from ba-vec
                  (mk-copier-from-stmt copy-stmt conn))))))

(defn not-found-copy-start?
  [state s]
  (let [state' @state]
    (cond state' false
          
          (= (take 4 s) (seq "COPY"))
          (do (reset! state :found)
              false #_true)

          :else true)))

(defn not-found-copy-end?
  [state s]
  (let [state' @state]
    (cond (= (take 2 s) (seq "\\."))
          (do(reset! state :found)
             false)

          (not state') true
          
          :else false)))

(defn parse-pg-dump-str
  [pg-dump]
  (let [copy-start (atom nil)
        copy-end (atom nil)]
    (->> pg-dump
         st/split-lines
         (drop-while (partial not-found-copy-start? copy-start))
         (take-while (partial not-found-copy-end? copy-end)))))

(defn prep-pg-dump-file
  [in out]
  (->> in
       slurp
       parse-pg-dump-str
       (st/join "\n")
       (spit out)))

#_(prep-pg-dump-file "/home/bill/categories.sql" "/home/bill/repos/vetd-app/resources/migrations/data/categories.sql")

#_(prep-pg-dump-file "/home/bill/orgs.sql" "/home/bill/repos/vetd-app/resources/migrations/data/orgs.sql")

#_(prep-pg-dump-file "/home/bill/products.sql" "/home/bill/repos/vetd-app/resources/migrations/data/productes.sql")

#_(prep-pg-dump-file "/home/bill/Downloads/product-categories.sql" "/home/bill/repos/vetd-app/resources/migrations/data/product-categories.sql")
