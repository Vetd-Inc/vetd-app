(ns vetd-app.admin
  (:require [vetd-app.common :as com]   
            [vetd-app.util :as ut]   
            [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as sec]))


(sec/defroute admin-path "/a" []
  (do (.log js/console "nav admin")
      (rf/dispatch [:admin/route-home])))

(defmethod com/fn-plugin-hook :admin/init [& _]
  (println "INIT ADMIN"))
