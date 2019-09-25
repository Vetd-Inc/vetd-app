(ns vetd-admin.full
  (:require [vetd-admin.admin :as admin]
            [vetd-app.app :as app]))

(aset js/window "suppressAnalytics" true)
(println "analytics are suppressed for admins")

(println "LOADED FULL")
