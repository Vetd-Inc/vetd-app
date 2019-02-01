(ns com.vetd.app.core
  (:require[com.vetd.app.server :as svr]
           [com.vetd.app.env :as env]
           [cheshire.core :as json]
           [clj-http.client :as http]
           [taoensso.timbre :as log]            
           com.vetd.app.db)
  (:gen-class))


(log/set-level! :info)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (log/set-level! :info)
  (svr/start-server))
