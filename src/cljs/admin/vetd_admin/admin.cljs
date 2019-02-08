(ns vetd-admin.admin
  (:require [vetd-app.util :as ut]   
            [secretary.core :as sec]))

(println "BEGIN ADMIN")

(sec/defroute admin-path "/a" []
    (.log js/console "nav admin"))

(defn init! []
  (println "INIT ADMIN"))

(println "END ADMIN")
