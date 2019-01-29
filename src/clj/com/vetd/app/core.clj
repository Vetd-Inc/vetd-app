(ns com.vetd.app.core
  (:require[com.vetd.app.server :as svr]
           [com.vetd.app.env :as env]
           [cheshire.core :as json]
           [clj-http.client :as http]
           [taoensso.timbre :as log]            
           com.vetd.app.db)
  (:gen-class))


(log/info "VETD_ENV is:")
(log/info (env/get-vetd-env))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (when-not env/building?
    (svr/start-server)))
