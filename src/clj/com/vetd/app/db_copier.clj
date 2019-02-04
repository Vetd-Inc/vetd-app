(ns com.vetd.app.db-copier
  (:require [clojure.java.jdbc :as j]
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
      (.writeToCopy copier data 0 (count data))
      (.endCopy copier))
    true
    (catch Exception e
      (log/error e "copy-from FAILed")
      false)))

(defn copy-from-slurp [source copier]
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


(defn not-found-copy-start?
  [state s]
  (let [state' @state]
    (cond state' false
          
          (= (take 4 s) (seq "COPY"))
          (do (reset! state :found)
              true)

          :else true)))

(defn not-found-copy-end?
  [state s]
  (let [state' @state]
    (cond (= (take 2 s) (seq "\\."))
          (do(reset! state :found)
             false)

          (not state') true
          
          :else false)))

(defn pg-dump->copy-data-lines
  [pg-dump]
  (let [copy-start (atom nil)
        copy-end (atom nil)]
    (->> pg-dump
         st/split-lines
         (drop-while (partial not-found-copy-start? copy-start))
         (take-while (partial not-found-copy-end? copy-end)))))

