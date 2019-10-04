(ns vetd-app.websockets
  (:require [vetd-app.util :as util]
            [vetd-app.local-store :as local-store]
            [re-frame.core :as rf]
            [cognitect.transit :as t]))

(def magic-number 100000)

(def msg-id-base (mod (util/now) (* 1000 60 60 24)))

(def last-msg-id& (atom (rand-int magic-number)))

(defn get-next-msg-id []
  (str msg-id-base
       "-"
       (swap! last-msg-id& #(-> % inc (mod magic-number)))))

(def json-writer (t/writer :json))
(def json-reader (t/reader :json))

(defonce ws& (atom nil))

(def ws-queue (atom []))

(def ws-buffer& (atom {}))
(def ws-subsciption-buffer& (atom {}))

(def last-send-ts& (atom 0))
(def last-ack-ts& (atom 0))

(defn log-ws? []
  (and (exists? js/log_ws)
       (true? js/log_ws)))

(declare ws-reconnect)
(declare ws-send-buffer)

(defn watch-send-ack-ts []
  (when (> (- @last-send-ts& @last-ack-ts&) 4000)
    (.log js/console "reconnect")
    (try
      (.close @ws&)
      (catch js/Error e))
    (ws-reconnect)))

(defonce watch-send-ack-ts-id
  (js/setInterval watch-send-ack-ts 4000))

(defn get-buffer-by-msg-id [ws-subsciption-buffer]
  (->> ws-subsciption-buffer
       vals
       (group-by :msg-id)
       (util/fmap first)))

(rf/reg-event-db
 :ws-connected
 (fn [db [_ url]]
   (println "Websocket connected to: " url)
   (let [ws-buffer @ws-buffer&
         ws-subsciption-buffer @ws-subsciption-buffer&
         buffer (merge ws-buffer
                       (get-buffer-by-msg-id ws-subsciption-buffer))]
     (when-not (empty? buffer)
       (ws-send-buffer buffer)))
   db))

(rf/reg-event-fx
 :ws-inbound
 (fn [cofx [_ data]]
   (when (log-ws?)
     (.log js/console "ws-inbound")
     (.log js/console (str data)))
   (or (when (map? data)
         (let [{:keys [return response]} data
               handler (or (and (keyword? return) return)
                           (:handler return))]
           (when handler
             {:dispatch [handler response data]})))
       {})))

(rf/reg-event-fx
 :ws/ack
 (fn [_ [_ {:keys [msg-ids]} data]]
   (reset! last-ack-ts& (util/now))
   (swap! ws-buffer&
          (partial reduce dissoc)
          msg-ids)
   {}))

(defn ws-onmessage
  [data]
  (let [d (t/read json-reader (.-data data))]
    (rf/dispatch [:ws-inbound d])))

(defn mk-ws-conn [url]
  (println "attempting to connect websocket")
  (reset! last-send-ts& 0)
  (reset! last-ack-ts& 0)
  (if-let [ws (js/WebSocket. url)]
    (do
      (set! (.-onmessage ws) ws-onmessage)
      (set! (.-onclose ws)
            (fn []
              (.log js/console "Websocket closed. Reconnecting in 1 sec...")
              (js/setTimeout #(mk-ws-conn url) 1000)))
      (set! (.-onopen ws)
            #(rf/dispatch [:ws-connected url]))
      (println "Websocket connection initiated with: " url)
      (reset! ws& ws)
      ws)
    (throw (js/Error. "Websocket connection failed!"))))

(defn ws-reconnect []
  (when-let [ws @ws&]
    (mk-ws-conn (.-url ws))))

(rf/reg-cofx
 :ws-conn
 (fn [cofx url]
   (let [ws (mk-ws-conn url)]
     (assoc cofx
            :ws-conn
            ws))))

(defn mk-ws-url []
  (str (if (-> js/window .-location .-protocol (= "https:"))
         "wss://"
         "ws://")
       (.-host js/location) "/ws"))

(rf/reg-event-fx
 :ws-init
 [(rf/inject-cofx :ws-conn (mk-ws-url))]
 (fn [{:keys [db ws-conn]} _]
   {}))

(defn ws-send-buffer [buffer-map]
  (when (log-ws?)
    (.log js/console (str buffer-map)))
  (let [ws @ws&]   
    (when (and ws
               (= 1 (.-readyState ws)))
      (do (reset! last-send-ts& (util/now))
          (.send ws (t/write json-writer
                             {:payloads buffer-map}))))))

(rf/reg-fx
 :ws-send
 (fn [rs]
   (doseq [r (util/->vec rs)]
     (let [msg-id (get-next-msg-id)
           p (assoc (:payload r)
                    :session-token (local-store/get-item :session-token)
                    :ws/msg-id msg-id
                    :ws/ts (util/now))]
       (swap! ws-buffer& assoc msg-id p)
       (case (:subscription r)
         :start (swap! ws-subsciption-buffer& assoc (:sub-id p) p)
         :stop (swap! ws-subsciption-buffer& dissoc (:sub-id p))
         nil)))
   (ws-send-buffer @ws-buffer&)))
