(ns com.vetd.app.server
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [migratus.core :as mig]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [clojure.core.async :as a]
            [compojure.core :as c]
            [compojure.handler :as ch]
            [compojure.route :as cr]
            [hiccup.page :as page]
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
;; cache key set on startup, unique every 5 minutes, appended to some resources
(defonce cache-key
  (let [now (ut/now)]
    (- now (mod now (* 1000 60 5)))))

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

(defn control-cache
  "Forces cache refresh per deploy"
  [uri]
  (str uri "?" cache-key))

(defn app-html
  [cookies]
  (page/html5
   [:head
    [:title (str "Vetd | "
                 (if (admin-session? cookies)
                   "ADMIN"
                   "B2B Software Buying Platform"))]
    [:meta {:http-equiv "Content-Type"
            :content "text/html; charset=UTF-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1"}]
    ;; favicon etc
    [:link {:rel "apple-touch-icon" :sizes "180x180"
            :href "/apple-touch-icon.png"}]
    [:link {:rel "icon" :type "image/png" :sizes "32x32"
            :href "/favicon-32x32.png"}]
    [:link {:rel "icon" :type "image/png" :sizes "16x16"
            :href "/favicon-16x16.png"}]
    [:link {:rel "manifest" :href "/site.webmanifest"}]
    [:link {:rel "mask-icon" :color "#4ec2c4"
            :href "/safari-pinned-tab.svg"}]
    [:meta {:name "apple-mobile-web-app-title" :content "Vetd"}]
    [:meta {:name "application-name" :content "Vetd"}]
    [:meta {:name "msapplication-TileColor" :content "#00aba9"}]
    [:meta {:name "theme-color" :content "#ffffff"}]
    (page/include-css (control-cache "/assets/css/semantic-ui.css"))
    (page/include-css "/assets/lib/toastr/toastr.min.css")
    (page/include-css (control-cache "/assets/css/app.css"))
    (when (admin-session? cookies)
      (page/include-css (control-cache "/assets/css/admin.css")))
    (when-not (admin-session? cookies)
      ;; load FullStory
      [:script {:type "text/javascript"}
       "window['_fs_debug'] = false;
window['_fs_host'] = 'fullstory.com';
window['_fs_org'] = 'KWYQ7';
window['_fs_namespace'] = 'FS';
(function(m,n,e,t,l,o,g,y){
    if (e in m) {if(m.console && m.console.log) { m.console.log('FullStory namespace conflict. Please set window[\"_fs_namespace\"].');} return;}
    g=m[e]=function(a,b,s){g.q?g.q.push([a,b,s]):g._api(a,b,s);};g.q=[];
    o=n.createElement(t);o.async=1;o.crossOrigin='anonymous';o.src='https://'+_fs_host+'/s/fs.js';
    y=n.getElementsByTagName(t)[0];y.parentNode.insertBefore(o,y);
    g.identify=function(i,v,s){g(l,{uid:i},s);if(v)g(l,v,s)};g.setUserVars=function(v,s){g(l,v,s)};g.event=function(i,v,s){g('event',{n:i,p:v},s)};
    g.shutdown=function(){g(\"rec\",!1)};g.restart=function(){g(\"rec\",!0)};
    g.consent=function(a){g(\"consent\",!arguments.length||a)};
    g.identifyAccount=function(i,v){o='account';v=v||{};v.acctId=i;g(o,v)};
    g.clearUserCookie=function(){};
})(window,document,window['_fs_namespace'],'script','user');"])]
   [:body
    [:div {:id "app"}
     ;; loading spinner
     [:div {:class "spinner"
            :style "margin: 175px auto;"}
      [:i] [:i] [:i]]]
    ;; load Segment's "Analytics.js"
    [:script {:type "text/javascript"}
     "!function(){var analytics=window.analytics=window.analytics||[];if(!analytics.initialize)if(analytics.invoked)window.console&&console.error&&console.error(\"Segment snippet included twice.\");else{analytics.invoked=!0;analytics.methods=[\"trackSubmit\",\"trackClick\",\"trackLink\",\"trackForm\",\"pageview\",\"identify\",\"reset\",\"group\",\"track\",\"ready\",\"alias\",\"debug\",\"page\",\"once\",\"off\",\"on\"];analytics.factory=function(t){return function(){var e=Array.prototype.slice.call(arguments);e.unshift(t);analytics.push(e);return analytics}};for(var t=0;t<analytics.methods.length;t++){var e=analytics.methods[t];analytics[e]=analytics.factory(e)}analytics.load=function(t,e){var n=document.createElement(\"script\");n.type=\"text/javascript\";n.async=!0;n.src=\"https://cdn.segment.com/analytics.js/v1/\"+t+\"/analytics.min.js\";var a=document.getElementsByTagName(\"script\")[0];a.parentNode.insertBefore(n,a);analytics._loadOptions=e};analytics.SNIPPET_VERSION=\"4.1.0\";}}();"]
    (page/include-js
     (control-cache
      (if (admin-session? cookies)
        "/js/full.js"
        "/js/app.js")))
    (page/include-js
     "/assets/js/confetti.js")]))

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
       (c/GET "*" [] (fn [{:keys [cookies]}]
                       (app-html cookies))))
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
