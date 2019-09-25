(ns vetd-admin.full
  (:require [vetd-admin.admin :as admin]
            [vetd-app.app :as app]))

(aset js/window "suppressAnalytics" true)

(println "LOADED FULL")
