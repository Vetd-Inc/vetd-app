(ns vetd-admin.admin
  (:require [vetd-app.hooks :as hooks]
            [vetd-admin.pages.a-search :as p-a-search]
            [vetd-admin.admin-fixtures :as a-fix]
            [vetd-admin.overlays.admin-v-home :as ovr-v-home]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [secretary.core :as sec]))

(println "START ADMIN")

(def show-admin?& (r/atom true #_false))

(sec/defroute admin-search-path "/a/search/" []
  (rf/dispatch [:a/route-search]))

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

(hooks/reg-hook! hooks/init! :admin init!)

(hooks/reg-hook! hooks/c-page :a/search p-a-search/c-page)

(hooks/reg-hook! hooks/c-container :admin-overlay c-admin-overlay-container)

(hooks/reg-hook! hooks/c-container :a/search a-fix/container)

(hooks/reg-hook! hooks/c-admin :v/home ovr-v-home/c-overlay)


(println "END ADMIN")
