(ns com.vetd.app.server
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.db :as db]
            [clojure.core.async :as a]
            [compojure.core :as c]
            [compojure.handler :as ch]
            [compojure.route :as cr]
            [aleph.http :as ah]
            [manifold.stream :as ms]
            [taoensso.timbre :as log]
            [cognitect.transit :as t]
            [clojure.java.io :as io]
            com.vetd.app.auth
            com.vetd.app.buyers
            com.vetd.app.vendors)
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:gen-class))

(defonce server (atom nil))
(defonce ws-conns (atom #{})) ;; TODO



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

(defn get-public-resource
  [uri]
  (if-let [body (some-> (str "public" uri) io/resource io/input-stream)]
    {:status 200
     :headers {"Content-Type" (uri->content-type uri)}
     :body body}
    {:status 404
     :body "NOT FOUND"}))

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
                ws)))

(c/defroutes routes
  (c/GET "/ws" [] #'ws-handler)
  (c/GET "/assets*" [] (fn [{:keys [uri]}] (get-public-resource uri)))
  (c/GET "/js*" [] (fn [{:keys [uri]}] (get-public-resource uri)))
  (c/GET "/a" [] (fn [_] (-> "public/admin.html" io/resource io/input-stream)))  
  (c/GET "*" [] (fn [_] (-> "public/app.html" io/resource io/input-stream))))

(defn start-server []
  (log/info "starting http server...")
  (try
    (if-not @server
      (do (reset! server
                  (ah/start-server #'routes {:port 5080}))
          (log/info "started http server on port 5080"))
      (log/info "server already running"))
    (catch Exception e
      (log/error e "EXCEPTION while trying to start http server"))))

(defn stop-server []
  (log/info "stopping http server...")
  (try
    (.close @server)
    (reset! server nil)
    (log/info "stopped http server on port 5080")
    (catch Exception e
      (log/error e "EXCEPTION while trying to stop http server"))))

#_ (stop-server)

#_(start-server)

