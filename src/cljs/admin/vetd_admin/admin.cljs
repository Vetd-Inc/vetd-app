(ns vetd-admin.admin
  (:require [vetd-app.hooks :as hks]   
            [vetd-app.util :as ut]
            [vetd-app.a-home :as p-a-home]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as sec]))

(println "START ADMIN")

(sec/defroute admin-path "/a" []
  (do (.log js/console "nav admin")
      (rf/dispatch [:a/route-home])))

(rf/reg-event-db
 :a/route-home
 (fn [db [_ query-params]]
   (assoc db
          :page :a/home)))

(defn c-admin-buyer []
  [:div "ADMIN BUYER"])

(defn c-admin-overlay-container [p]
  [:div "ADMIN OVERLAY CONTAINER " p])

(defn c-admin-container [p]
  [:div "ADMIN CONTAINER " p])

(defn init! []
  (println "init! ADMIN"))

(hks/reg-hook! hks/init! :admin init!)

(hks/reg-hook! hks/c-page :a/home p-a-home/c-page)

(hks/reg-hook! hks/c-container :admin-overlay c-admin-overlay-container)

(hks/reg-hook! hks/c-container :a/home c-admin-container)

(println "END ADMIN")
