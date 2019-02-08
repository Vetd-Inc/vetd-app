(ns vetd-app.admin-loader
    (:require [vetd-app.util :as ut]   
              [secretary.core :as sec]))

(println "BEGIN admin-loader")

(try
  (require '[vetd-admin.admin]
           :reload-all)
  (catch js/Error e))

(defn try-init-admin! []
  (when-let [f (resolve 'vetd-admin.admin/init!)]
    (f)))

(println "END admin-loader")
