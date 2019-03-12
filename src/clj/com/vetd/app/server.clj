(ns com.vetd.app.server
  (:require [com.vetd.app.common :as com]
            [migratus.core :as mig]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [clojure.core.async :as a]
            [compojure.core :as c]
            [compojure.handler :as ch]
            [compojure.route :as cr]
            [aleph.http :as ah]
            [manifold.stream :as ms]
            [ring.middleware.cookies :as rm-cookies]
            [taoensso.timbre :as log]
            [cognitect.transit :as t]
            [clojure.java.io :as io]
            [com.vetd.app.auth :as auth]
            com.vetd.app.buyers
            com.vetd.app.vendors
            com.vetd.app.admin)
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:gen-class))

(defonce server (atom nil))
(defonce ws-conns (atom #{})) ;; TODO
(defonce kill-keep-alive-thread (atom false))
(defonce keep-alive-thread (atom nil))

(defn uri->content-type
  [uri]
  (or (-> uri
          (clojure.string/split #"\.")
          last
          ({"js" "application/javascript"
            "css" "text/css"
            "html" "text/html"
            "svg" "image/svg+xml"}))
      "text/plain"))

(defn public-resource-response
  [uri]
  (if-let [body (some-> (str "public" uri) io/resource io/input-stream)]
    {:status 200
     :headers {"Content-Type" (uri->content-type uri)}
     :body body}
    {:status 404
     :body "NOT FOUND"}))

(defmacro serve-public-resource
  [path]
  `(compojure.core/GET ~path [] (comp public-resource-response :uri)))

(defn ->transit
  [v]
  (def v1 v)
  (let [baos (ByteArrayOutputStream.)
        tw (t/writer baos :json
                     {:handlers {}})]
    (t/write tw v)
    (.toByteArray baos)))

(defn respond-transit
  [data ws]
  (ms/try-put! ws
               (String. (->transit data)) ;; TODO this can't be a byte array or something???
               200))

;; use muutanja??
(defn read-transit-string
  [s]
  (-> s
      .getBytes
      (ByteArrayInputStream.)
      (t/reader :json)
      t/read))

(defn start-keep-alive-thread []

  (reset! kill-keep-alive-thread false)
  (if-not @keep-alive-thread
    (do (log/info "Starting keep-alive thread...")  
        (reset! keep-alive-thread
                (future
                  (do (log/info "Started keep-alive thread.")  
                      (while (not @kill-keep-alive-thread)
                        (try
                          (Thread/sleep 30000)
                          (doseq [ws @ws-conns]
                            (do (def ws1 ws)
                                (respond-transit {:cmd :keep-alive
                                                  :return nil
                                                  :response nil}
                                                 ws)))
                          (catch Throwable t
                            (def ka-t t)
                            (log/error t))))
                      (log/info "Stopped keep-alive thread.")
                      :done))))
    (log/info "Keep-alive thread already running.")))

(defn stop-keep-alive-thread []
  (log/info "Stopping keep-alive thread...")
  (reset! kill-keep-alive-thread true)
  (reset! keep-alive-thread nil))

(defn ws-outbound-handler
  [ws {:keys [cmd return] :as req} data]
  (respond-transit {:cmd cmd
                    :return return
                    :response data}
                   ws))

(defn ws-inbound-handler
  [ws ws-id data]
  (try
    (let [{:keys [cmd return] :as data'} (read-transit-string data)
          resp-fn (partial #'ws-outbound-handler ws data')
          resp (com/handle-ws-inbound data' ws-id resp-fn)]
      (when (and return resp)
        (resp-fn resp)))
    (catch Exception e
      (log/error e))))

(defn ws-on-closed
  [ws ws-id & args]
  #_  (println "ws-on-closed")
  #_  (clojure.pprint/pprint args)
  (swap! ws-conns disj ws)
  #_  (db/unsubscribe-ws-conn ws-id)
  true)

(defn ws-handler
  [req]
  #_  (println "ws-handler:")
  #_  (clojure.pprint/pprint req)
  (let [ws @(ah/websocket-connection req)
        ws-id (str (gensym "ws"))]
    (swap! ws-conns conj ws)
    (ms/on-closed ws (partial ws-on-closed ws ws-id))
    (ms/consume (partial ws-inbound-handler
                         ws
                         ws-id)
                ws)
    "ws"))

(defn admin-session? [cookies]
  (-> "admin-token"
      cookies
      :value
      auth/admin-session?))

(def app
  (-> (c/routes
       (c/GET "/ws" [] #'ws-handler)
       (cr/resources "/assets" {:root "public/assets"})
       (c/GET "/assets*" [] cr/not-found)
       (serve-public-resource "/apple-touch-icon.png")
       (serve-public-resource "/favicon-32x32.png")
       (serve-public-resource "/favicon-16x16.png")
       (serve-public-resource "/site.webmanifest")
       (serve-public-resource "/safari-pinned-tab.svg")
       (c/GET "/js/app.js" [] (fn [{:keys [uri cookies]}]
                                (if env/prod?
                                  (public-resource-response uri)
                                  (public-resource-response "/js/full.js"))))
       (c/GET "/js/full.js" [] (fn [{:keys [uri cookies]}]
                                 (if (admin-session? cookies)
                                   (public-resource-response uri)
                                   "")))
       (serve-public-resource "/js*")
       (c/GET "/-reset-db-" [] (fn [{:keys [cookies]}]
                                 (do (future (mig/reset {:store :database
                                                         :db env/pg-db}))
                                     "DOING IT")))
       (c/GET "*" [] (fn [{:keys [cookies]}]
                       (-> (if (admin-session? cookies)
                             "public/admin.html"
                             "public/app.html")
                           io/resource
                           io/input-stream))))
      rm-cookies/wrap-cookies))

(defn start-server []
  (log/info "starting http server...")
  (try
    (if-not @server
      (do (start-keep-alive-thread)
          (reset! server
                  (ah/start-server #'app {:port 5080}))
          (log/info "started http server on port 5080"))
      (log/info "server already running"))
    (catch Exception e
      (log/error e "EXCEPTION while trying to start http server"))))

(defn stop-server []
  (log/info "stopping http server...")
  (try
    (stop-keep-alive-thread)
    (if-let [svr @server]
      (do
        (.close svr)
        (reset! server nil)
        (log/info "executed close for http server on port 5080")
        svr)
      (log/info "There was no server to stop."))
    (catch Exception e
      (log/error e "EXCEPTION while trying to stop http server"))))

#_ (stop-server)

#_(start-server)
