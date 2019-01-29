(ns com.vetd.app.core
  (:require[com.vetd.app.server :as svr]
           [com.vetd.app.env :as env]
           [cheshire.core :as json]
           [clj-http.client :as http]
           [taoensso.timbre :as log]            
           com.vetd.app.db)
  (:gen-class))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (svr/start-server))
