(ns vetd-app.local-store
  (:require [re-frame.core :as rf]
            [vetd-app.util :as util]))

;; avoid using directly unless necessary
(defn get-item
  "Given clj keyword 'k', return local storage value as clj value."
  [k]
  (js->clj (.getItem js/localStorage (util/kw->str k))))

(defn set-item
  "Given clj keyword 'k' and clj value 'v', set local storage."
  [k v]
  (.setItem js/localStorage (util/kw->str k) (clj->js v)))

(defn remove-item
  "Given clj keyword 'k', remove from local storage."
  [k]
  (.removeItem js/localStorage (util/kw->str k)))

(rf/reg-cofx 
 :local-store
 (fn [cofx local-store-keys]
   (->> (for [k local-store-keys]
          [k (get-item k)])
        (into {})
        (assoc cofx :local-store))))

(rf/reg-fx
 :local-store
 (fn [m] ;; use nil to unset an item
   (doseq [[k v] m]
     (if (nil? v)
       (remove-item k)
       (set-item k v)))))
