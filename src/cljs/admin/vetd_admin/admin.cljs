(ns vetd-admin.admin
  (:require [vetd-app.hooks :as hooks]
            [vetd-admin.pages.a-search :as p-a-search]
            [vetd-admin.pages.form-templates :as p-aform-templates]
            [vetd-admin.admin-fixtures :as a-fix]
            [vetd-admin.overlays.admin-v-preposals :as ovr-v-preposals]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [secretary.core :as sec]))

(println "START ADMIN")

(def show-admin?& (r/atom true #_false))

(sec/defroute admin-search-path "/a/search" []
  (rf/dispatch [:a/route-search]))

(sec/defroute admin-form-templates-path "/a/form-templates" []
  (rf/dispatch [:a/route-form-templates nil]))

(sec/defroute admin-form-templates-id-path "/a/form-templates/:idstr" [idstr]
  (rf/dispatch [:a/route-form-templates idstr]))

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

(hooks/reg-hooks! hooks/c-page
                  {:a/search #'p-a-search/c-page
                   :a/form-templates #'p-aform-templates/c-page})

(hooks/reg-hooks! hooks/c-container
                  {:admin-overlay #'c-admin-overlay-container
                   :a/search #'a-fix/container
                   :a/form-templates #'a-fix/container})

(hooks/reg-hook! hooks/init! :admin init!)

(hooks/reg-hook! hooks/c-admin :v/preposals ovr-v-preposals/c-overlay)


(println "END ADMIN")
