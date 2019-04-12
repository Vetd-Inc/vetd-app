(ns com.vetd.app.core
  (:require [com.vetd.app.server :as svr]
            [com.vetd.app.env :as env]
            [com.vetd.app.common :as com]
            [clojure.core.async :as a]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [taoensso.timbre :as log]
            [migratus.core :as mig]
            com.vetd.app.migrations
            com.vetd.app.db)
  (:gen-class))

(def nrepl-server& (atom nil))

(log/set-level! :info)

(defn resolve-start-server-fn []
  (try
    (load "/nrepl/server")
    (ns-resolve 'nrepl.server 'start-server)
    (catch Throwable t
      nil)))

(defn resolve-stop-server-fn []
  (try
    (load "/nrepl/server")
    (ns-resolve 'nrepl.server 'stop-server)
    (catch Throwable t
      nil)))

(defn try-start-nrepl-server []
  (try
    (if-not @nrepl-server&
      (if-let [start-fn (resolve-start-server-fn)]
        (do (log/info "starting nrepl server...")
            (reset! nrepl-server& (start-fn :bind "0.0.0.0" :port 4001)) ; TODO add cider middleware
            (log/info "started nrepl server on port 4001")
            @nrepl-server&)
        (log/info "Could not resolve `nrepl.server/start-server`"))
      (log/info "nrepl server already running"))
    (catch Throwable t
      (log/error t "EXCEPTION while trying to start nrepl server"))))

(defn try-stop-nrepl-server []
  (try
    (if-let [nrepl-server @nrepl-server&]
      (if-let [stop-fn (resolve-stop-server-fn)]
        (do (log/info "stoping nrepl server...")
            (stop-fn nrepl-server)
            (reset! nrepl-server& nil)
            (log/info "stopped nrepl server")
            true)
        (log/info "Could not resolve `nrepl.server/stop-server`"))
      (log/info "nrepl server is not running"))
    (catch Throwable t
      (log/error t "EXCEPTION while trying to stop nrepl server"))))

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
    (a/close! (com/shutdown-ch))
    (when-let [svr (svr/stop-server)]
      (wait-to-exit svr)
      (log/info "Completed `wait-to-exit`"))
    (try-stop-nrepl-server)
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
      (log/error t)))
  (try
    (try-start-nrepl-server)
    (catch Throwable t
      (log/error t))))

#_ (-main)
