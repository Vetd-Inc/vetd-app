(ns vetd-admin.pages.a-home
  (:require [vetd-app.util :as ut]   
            [reagent.core :as r]
            [re-frame.core :as rf]))


(rf/reg-event-fx
 :a/nav-home
 (fn [{:keys [db]} _]
   {:nav {:path "/a/home/"}}))

(rf/reg-event-db
 :a/route-home
 (fn [db [_ query-params]]
   (assoc db
          :page :a/home
          :query-params query-params)))

(defn c-page []
  (fn []
    [:div "ADMIN HOME"]))
