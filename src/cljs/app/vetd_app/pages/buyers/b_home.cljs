(ns vetd-app.pages.buyers.b-home
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :nav-buyers
 (fn [{:keys [db]} [_ query]]
   {:nav {:path (:org-id db)
          :query query}}))

(rf/reg-event-db
 :route-b-home
 (fn [db [_ query-params]]
   (assoc db
          :page :b-home
          :query-params query-params)))

(defn c-page []
  (let [x 1]
    (fn []
      [:div "BUYERS' HOME"])))
