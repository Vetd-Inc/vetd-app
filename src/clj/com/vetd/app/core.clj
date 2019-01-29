(ns com.vetd.app.core
  (:require[com.vetd.app.server :as svr]
           [com.vetd.app.env :as env]
            
            [cheshire.core :as json]
            [clj-http.client :as http]
            com.vetd.app.db)
  (:gen-class))

(println "ENV: ")
(env/print-vetd-env)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (when-not env/building?
    (svr/start-server)))
