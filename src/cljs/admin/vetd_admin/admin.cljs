(ns vetd-admin.admin
  (:require [vetd-app.hooks :as hks]   
            [vetd-app.util :as ut]
            [vetd-admin.pages.a-home :as p-a-home]
            [vetd-admin.pages.a-search :as p-a-search]
            [vetd-admin.admin-fixtures :as a-fix]
            [vetd-admin.overlays.admin-v-home :as ovr-v-home]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [secretary.core :as sec]))

(println "START ADMIN")

(def show-admin?& (r/atom true #_false))

(sec/defroute admin-home-path "/a/home/" []
  (do (.log js/console "nav admin")
      (rf/dispatch [:a/route-home])))

(sec/defroute admin-search-path "/a/search/" []
  (rf/dispatch [:a/route-search]))

(rf/reg-event-db
 :a/route-home
 (fn [db [_ query-params]]
   (assoc db
          :page :a/home)))

(defn c-admin-buyer []
  [:div "ADMIN BUYER"])

(defn c-admin-overlay-container [p]
  (fn [p]
    (if-not @show-admin?&
      [:div#admin-over-cont.closed
       [:div#admin-symbol
        {:on-click #(reset! show-admin?& true)}]]
      [:div#admin-over-cont.open
       p
       [:div#admin-symbol
        {:on-click #(reset! show-admin?& false)}]])))

(defn c-admin-container [p]
  [:div "ADMIN CONTAINER " p])

(defn init! []
  (println "init! ADMIN"))

(hks/reg-hook! hks/init! :admin init!)

(hks/reg-hook! hks/c-page :a/home p-a-home/c-page)
(hks/reg-hook! hks/c-page :a/search p-a-search/c-page)

(hks/reg-hook! hks/c-container :admin-overlay c-admin-overlay-container)

(hks/reg-hook! hks/c-container :a/home a-fix/container)
(hks/reg-hook! hks/c-container :a/search a-fix/container)

(hks/reg-hook! hks/c-admin :v/home ovr-v-home/c-overlay)


(println "END ADMIN")
