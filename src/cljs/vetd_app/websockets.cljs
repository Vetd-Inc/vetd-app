(ns vetd-app.websockets
  (:require [vetd-app.util :as ut]
            [re-frame.core :as rf]
            [cognitect.transit :as t]))

(def json-writer (t/writer :json))
(def json-reader (t/reader :json))

(def ws& (atom nil))

(def ws-queue (atom []))

(comment
    #_(def ws1 (mk-ws-conn "ws://localhost:8080/v1alpha1/graphql"))

    (def ws1  (js/WebSocket. "ws://localhost:8080/v1alpha1/graphql" "graphql-ws"))

    (.-onopen ws1 #(.log js/console %))

    (set! (.-onmessage ws1) #(.log js/console %))

    (.close ws1))


(rf/reg-event-db
 :ws-connected
 (fn [db [_ url]]
   (println "Websocket connected to: " url)
   (let [q @ws-queue]
     (reset! ws-queue [])
     (doseq [f q]
       (f)))
   db))

(rf/reg-event-fx
 :ws-inbound
 (fn [cofx [_ data]]
   (if (map? data)
     (let [{:keys [return response]} data]
       (let [handler (or (and (keyword? return) return)
                      (:handler return))]
         {:dispatch [handler response data]}))
     {})))

(defn ws-onmessage
  [data]
  (let [d (t/read json-reader (.-data data))]
#_    (println d)
    (rf/dispatch [:ws-inbound d])))

(defn mk-ws-conn [url]
 (println "attempting to connect websocket")
 (if-let [ws (js/WebSocket. url)]
   (do
     (set! (.-onmessage ws) ws-onmessage)
     (set! (.-onopen ws)
           #(rf/dispatch [:ws-connected url]))
     (println "Websocket connection initiated with: " url)
     ws)
   (throw (js/Error. "Websocket connection failed!"))))

(rf/reg-cofx
 :ws-conn
 (fn [cofx url]
   (let [ws (mk-ws-conn url)]
     (reset! ws& ws)
     (assoc cofx
            :ws-conn
            ws))))

(defn mk-ws-url []
  (str "ws://" #_(if (-> js/window .-location .-protocol (= "https:"))
         "wss://"
         "ws://")
       (.-host js/location) "/ws"))

;; TODO use wss:// in prod
(rf/reg-event-fx
 :ws-init
 [(rf/inject-cofx :ws-conn (mk-ws-url))]
 (fn [{:keys [db ws-conn]} _]
   {}))


(defn ws-send [{:keys [ws payload]}]
  (.log js/console "ws-send")
  (let [ws @ws&]   
    (if (and ws
             (= 1 (.-readyState ws)))
      (.send ws (t/write json-writer payload))
      (swap! ws-queue conj #(.send ws (t/write json-writer payload))))))

(rf/reg-fx
 :ws-send
 (fn [rs]
   (doseq [r (ut/->vec rs)]
     (ws-send r))))

