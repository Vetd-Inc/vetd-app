(ns vetd-app.pages.buyers.b-home
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :b/nav-home
 (fn [{:keys [db]} _]
   {:nav {:path "/b/home/"}}))

(rf/reg-event-db
 :b/route-home
 (fn [db [_ query-params]]
   (assoc db
          :page :b/home
          :query-params query-params)))

(defn c-page []
  (let [x 1]
    (fn []
      [:div "BUYERS' HOME"])))
