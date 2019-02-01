(ns com.vetd.app.hasura
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.env :as env]
            [com.vetd.app.db-schema :as sch]
            [aleph.http :as ah]
            [manifold.stream :as ms]
            [manifold.deferred :as md]            
            [clojure.java.jdbc :as j]
            [clojure.core.async :as a]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]
            [honeysql.format :as hsfmt]
            [clojure.walk :as w]
            [cheshire.core :as json]
            [graphql-query.core :as dgql]
            clojure.edn))

;; :pre-init :init-sent :ackd :closed

(defonce cn& (atom {:ws nil
                :state :closed}))

;; TODO switch to channel??
(defonce queue& (atom []))

#_ (def sub-id->resp-fn& (atom {}))
(defonce sub-id->resp-fn& (atom {}))

;; https://github.com/apollographql/subscriptions-transport-ws/blob/faa219cff7b6f9873cae59b490da46684d7bea19/src/message-types.ts

(def gql-msg-types-kw->str
  {:connection-init "connection_init"
   :connection-ack "connection_ack"
   :connection-error "connection_error"
   :connection-keep-alive "ka"
   :connection-terminate "connection_terminate"
   :start "start"
   :data "data"
   :error "error"
   :complete "complete"
   :stop "stop"})

(def gql-msg-types-str->kw (clojure.set/map-invert gql-msg-types-kw->str))


(defn smoosh [a b]
  (if (every? map? [a b])
    (merge a b)
    b))

(defn process-sub-gql-map* [agg [path v]]
  (update-in agg path smoosh v))

(defn drop-kw-underscore
  [kw]
  (let [s (name kw)]
    (when (-> s first (= \_))
      (-> s rest clojure.string/join keyword))))

(defn process-sub-gql-map [m]
  (->> (for [[k v] m]
            (if-let [op (drop-kw-underscore k)]
              [[op] v]
              [[:where k] (cond (map? v) v
                                (coll? v) {:_in v}
                                :else {:_eq v})]))
       (reduce process-sub-gql-map*
               {})))

#_(process-sub-gql-map {:id 4
                      :xx {:_eq "hi"}
                      :_where {:yy {:> 555}}
                      :_order :hihih})

(defn process-sub-gql
  [v]
  (cond
    (instance? clojure.lang.MapEntry v) v
    
    (and (or (vector? v) (list? v))
         (some-> v second map?))
    (let [[_ m] v]
      (assoc v 1 (process-sub-gql-map m)))

    (instance? java.lang.Long v) (str v)
    
    :else v))

(defn ->gql-str
  [v]
  (->> v
       (w/postwalk process-sub-gql)
       sch/walk-clj-kw->sql-field  ;; will I regret this??????
       dgql/graphql-query))

#_(->gql-str {:queries [[:sessions
                         {:id 5
                          :_where {:code {:_eq "session-code"}
                                   :deleted {:_is_null false}}}
                         [:user_id]]]})

(defn walk-ids->long*
  [m]
  (if (map? m)
    (into {}
          (for [[k v] m]
            (if (->> k name (take-last 2) (= [\i \d]))
              [k (ut/->long v)]
              [k v])))
    m))

;; HACK -- Hasura returns bigints as string
(defn walk-ids->long [v]
  (w/prewalk walk-ids->long* v))


(defn process-result
  [r]
  (-> r
      sch/walk-sql-field->clj-kw
      walk-ids->long))

(defmulti handle-from-graphql (fn [{gtype :type}] (gql-msg-types-str->kw gtype)))

(defn ws-send [ws msg]
#_  (clojure.pprint/pprint msg)
  (try
    (ut/$- -> msg
           json/generate-string
           (ms/try-put! ws
                        $
                        1000))
    true
    (catch Exception e
      (log/error e)
      false)))

(defn send-terminate []
  (let [{:keys [ws]} @cn&]
    (reset! cn& {:ws nil
                 :state :closed})
    (ws-send ws {:type (gql-msg-types-kw->str :connection-terminate)})
    (ms/close! ws)))

#_(send-terminate)

(defn send-init
  [ws]
  (ws-send ws {:type (gql-msg-types-kw->str :connection-init)}))

(defn ws-on-closed []
  (println "CLOSED"))

(defn ws-ib-handler
  [id data]
#_  (println id)
#_  (clojure.pprint/pprint data)
  (let [data' (json/parse-string data keyword)]
    (when-let [errors (-> data' :payload :errors)]
      (log/error errors))
    (-> data'        
        (update :payload :data)
        (update :payload process-result)
        handle-from-graphql)))

(declare ensure-ws-setup)

(defn mk-ws []
  (println "MK-WS")
  (let [ws @(ah/websocket-client env/hasura-ws-url
                                 {:sub-protocols "graphql-ws"})]
    (ms/on-closed ws ws-on-closed)
    (ms/consume (partial ws-ib-handler (gensym "gql"))
                ws)
    (reset! cn& {:ws ws
                 :state :pre-init})
    (ensure-ws-setup @cn&)
    ws))

#_ (mk-ws)

(defn ensure-ws-setup
  [{:keys [ws state]}]
  (true?
   (if (or (nil? ws) (.isClosed ws))
     (mk-ws)
     (case state
       :pre-init (do (when
                         (send-init ws)
                         (swap! cn& assoc :state :init-sent))
                     false)
       :init-sent false
       :ackd true
       :closed false))))

(defn send-queue
  [ws]
  (try
    (when-let [msgs (-> queue&
                         (swap-vals! (constantly []))
                         first
                         not-empty)]
      (doseq [m msgs]
        (ws-send ws m))
      (count msgs))
    (catch Exception e
      (log/error e)
      false)))

(defn add-to-queue [msg]
  (swap! queue& conj msg))

(defn try-send [{:keys [ws] :as cn} msg]
  (if (ensure-ws-setup cn)
    (ws-send ws msg)
    (add-to-queue msg)))

(defn register-sub-id
  [sub-id resp-fn]
  (swap! sub-id->resp-fn&
         assoc sub-id resp-fn))

(defn unregister-sub-id
  [sub-id]
  (swap! sub-id->resp-fn&
         dissoc sub-id))

(defn respond-to-client
  [id msg]
  ((@sub-id->resp-fn& (keyword id))
   msg))

(defmethod handle-from-graphql :connection-keep-alive [_]

  #_(println "KA"))

(defmethod handle-from-graphql :connection-ack [_]
  (swap! cn& assoc :state :ackd)
  (-> @cn&
      :ws
      send-queue))

(defmethod handle-from-graphql :connection-error [{:keys [payload]}]
  (log/error payload))

(defmethod handle-from-graphql :data [{:keys [id payload]}]
  
  (respond-to-client id {:mtype :data
                         :payload payload}))

(defmethod handle-from-graphql :error [{:keys [id payload]}]
  (log/error payload)
  (respond-to-client id {:mtype :error
                         :payload payload}))

(defmethod handle-from-graphql :complete [{:keys [id payload]}]
  (unregister-sub-id id)
  (respond-to-client id {:mtype :complete
                         :payload payload}))

;; ws from client
(defmethod com/handle-ws-inbound :graphql
  [{:keys [sub-id query subscription? stop] :as msg} ws-id resp-fn]
#_  (clojure.pprint/pprint query)
  (let [qual-sub-id (keyword (name ws-id)
                             (str sub-id))]
    (if-not stop
      (do
        (register-sub-id qual-sub-id resp-fn)
        (try-send @cn&
                  {:type :start
                   :id qual-sub-id
                   :payload {:query (format "%s %s"
                                            (if subscription?
                                              "subscription"
                                              "query")
                                            (->gql-str query))}}))
      (do
        (unregister-sub-id qual-sub-id)
        (try-send @cn&
                  {:type (gql-msg-types-kw->str :stop)
                   :id qual-sub-id})))
    nil))

(defn sync-query
  [queries]
  (try
    (let [r (-> @(ah/post env/hasura-http-url
                          {:body (json/generate-string
                                  {:query (->gql-str {:queries queries})})})
                :body
                slurp)]
      (def r1 r)
      (-> r
          (json/parse-string keyword)
          :data
          process-result))
    (catch Exception e
      (def e1 e)
      (log/error e (try (some-> e .getData :body slurp)
                        (catch Exception e2 "")))
      (throw e))))

