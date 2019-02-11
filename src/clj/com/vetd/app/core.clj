(ns com.vetd.app.core
  (:require[com.vetd.app.server :as svr]
           [com.vetd.app.env :as env]
           [cheshire.core :as json]
           [clj-http.client :as http]
           [taoensso.timbre :as log]
           [migratus.core :as mig]
           com.vetd.app.migrations
           com.vetd.app.db)
  (:gen-class))


(log/set-level! :info)

(defn shutdown []
  (try
    (log/info "Starting shutdown hook")
    (svr/stop-server
     (fn [s]
       (.wait_for_close s)
       (log/info "Exiting")        
       (.exit (Runtime/getRuntime) 0)
       (log/info "Done shutdown hook")))
    (catch Throwable t)))

#_ (shutdown)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. shutdown))
  (log/set-level! :info)
  (mig/migrate {:store :database
                :db env/pg-db})
  (log/set-level! :info)  
  (svr/start-server))


#_ (-main)
