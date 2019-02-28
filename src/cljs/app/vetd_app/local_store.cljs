(ns vetd-app.local-store
  (:require [re-frame.core :as rf]
            [vetd-app.util :refer [kw->str]]))

(rf/reg-cofx 
 :local-store
 (fn [cofx local-store-keys]
   (->> (for [k local-store-keys]
          [k (js->clj (.getItem js/localStorage (kw->str k)))])
        (into {})
        (assoc cofx :local-store))))

(rf/reg-fx
 :local-store
 (fn [m]
   (doseq [[k v] m]
     (.setItem js/localStorage (kw->str k) (clj->js v)))))

