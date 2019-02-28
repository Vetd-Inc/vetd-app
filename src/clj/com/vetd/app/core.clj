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

(defn wait-to-exit [s]
  [s]
  (log/info "Waiting for server to close...")
  (try
    (.wait_for_close s)
    (log/info "Server closed.")
    (catch Throwable t
      (log/error "Exception waiting for server to close." t))))

(defn shutdown []
  (try
    (log/info "Starting shutdown hook")
    (when-let [svr (svr/stop-server)]
      (wait-to-exit svr)
      (log/info "Completed `wait-to-exit`"))
    (catch Throwable t
      (log/error t))
    (finally
      (log/info "Done shutdown hook"))))

#_ (shutdown)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. shutdown))
  (log/set-level! :info)
  (try
    (mig/migrate {:store :database
                  :db env/pg-db})
    (catch Throwable t
      (log/error t)))
  (log/set-level! :info)
  (try
    (svr/start-server)
    (catch Throwable t
      (log/error t))))


#_ (-main)
