(ns com.vetd.app.hasura
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [com.vetd.app.hasura-meta :as hm]
            [clojure.core.async :as a]
            [aleph.http :as ah]
            [manifold.stream :as ms]
            [manifold.deferred :as md]            
            [clojure.string :as st]            
            [taoensso.timbre :as log]
            [clojure.walk :as w]
            [cheshire.core :as json]
            [graphql-query.core :as dgql]
            [clj-time.coerce :as tc]
            clojure.edn))

;; :pre-init :init-sent :ackd :closed

(defonce cn& (atom {:ws nil
                    :state :closed}))

(def last-ka& (atom nil))

;; TODO switch to channel??
(defonce queue& (atom []))

#_ (def sub-id->resp-fn& (atom {}))
(defonce sub-id->resp-fn& (atom {}))

(defonce sub-id->created-ts& (atom {}))

(defonce sub-id->info& (atom {}))

(defonce sub-count-monitor (atom nil))

(defn get-oldest-sub-age []
  (or (some->> @sub-id->created-ts&
               vals
               not-empty
               (apply min)
               (- (ut/now)))
      0))

(defn get-sub-distinct-session-count []
  (or (some->> @sub-id->info&
               vals
               not-empty
               (map :session-token)
               distinct
               count)
      0))

#_
(get-sub-distinct-session-count)

#_
(count @sub-id->info&)

#_
(clojure.pprint/pprint @sub-id->info&)

#_
(get-oldest-sub-age)

(defn start-sub-count-monitor-thread []
  (when (and env/prod?
             (nil? @sub-count-monitor))
    (reset! sub-count-monitor
            (future
              (log/info "Starting sub-count-monitor")
              (while (not @com/shutdown-signal)
                (com/hc-send {:type :stats
                              :hasura-sub-count (count @sub-id->resp-fn&)})
                (com/hc-send {:type :stats
                              :hasura-sub-max-age (get-oldest-sub-age)})
                (com/hc-send {:type :stats
                              :hasura-sub-distinct-session-count (get-sub-distinct-session-count)})
                (Thread/sleep 5000))
              (log/info "Stopped sub-count-monitor")))))

(start-sub-count-monitor-thread) ;; TODO calling this here is gross -- Bill

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


(defn convert-kw
  [kw]
  (let [n (name kw)]
    (-> n
        (clojure.string/replace #"_qm$" "?")
        (clojure.string/replace #"_" "-")
        keyword)))

(defn reverse-convert-kw
  [kw]
  (let [n (name kw)]
    (-> n
        (clojure.string/replace #"\?$" "_qm")
        (clojure.string/replace #"-" "_")
        keyword)))

(defn mk-sql-field->clj-kw [fields]
  (into {}
        (for [f fields]
          [f (convert-kw f)])))

(defn get-all-field-names []
  (-> (concat (->> (db/select-distinct-col-names)
                   (map keyword))
              (->> hm/hasura-meta-cfg
                   :rels
                   (map :tables)
                   flatten
                   distinct)
              (->> hm/hasura-meta-cfg
                   :rels
                   (map :fields)
                   flatten
                   distinct
                   (map reverse-convert-kw)))
      distinct))

(def sql-field->clj-kw
  (try (mk-sql-field->clj-kw (get-all-field-names))
       (catch Throwable t
         (com/log-error t)
         {})))

(def clj-kw->sql-field (clojure.set/map-invert sql-field->clj-kw))

(defn walk-sql-field->clj-kw [v]
  (w/prewalk-replace sql-field->clj-kw v))

(defn walk-clj-kw->sql-field [v]
  (w/prewalk-replace clj-kw->sql-field v))


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
                             (nil? v) {:_is_null true}
                             :else {:_eq v})]))
       (reduce process-sub-gql-map*
               {})))

#_(process-sub-gql-map {:id 4
                        :xx {:_eq "hi"}
                        :_where {:yy {:> 555}}
                        :_order :hihih})

;; TODO _ => - in table names????????????

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



(declare walk-gql-query)

(defn walk-gql-query->sub-kw
  [field args sub]
  (walk-clj-kw->sql-field sub))

(defn walk-gql-query->sub-coll
  [field args sub]
  (walk-gql-query false sub))

(defn walk-gql-query->sub*
  [field args sub]
  (if (keyword? sub)
    (walk-gql-query->sub-kw field args sub)
    (walk-gql-query->sub-coll field args sub)))

(defn walk-gql-query->sub
  [field args sub]
  (mapv (partial walk-gql-query->sub* field args)
        sub))

(defn long->str [l]
  (when l (str l)))

(defn coerce-to-long?
  [kw]
  (or (.endsWith (name kw) "id")
      ;; HACK
      (#{:subject :sort :idx :result :count_stack_items} kw)))

(defn walk-gql-query->args
  [field args sub]
  (-> (for [[k v] args]
        [k (if (coerce-to-long? k)
             (if (coll? v)
               (mapv long->str v)
               (long->str v))
             v)])
      (into {})
      process-sub-gql-map
      walk-clj-kw->sql-field))

(defn walk-gql-query->entity
  [field args sub]
  (->> field
       clj-kw->sql-field
       name
       ;; HACK enforce default schema
       (str "vetd_")
       keyword))

(defn walk-gql-query->field
  [field args sub]
  (clj-kw->sql-field field))

(defn walk-gql-query
  [root? [field a b]]
  (let [[args sub] (if (map? a)
                     [a b] [nil a])
        field' (if root?
                 (walk-gql-query->entity field args sub)
                 (walk-gql-query->field field args sub))
        subs'  (walk-gql-query->sub field args sub)]
    (if args
      [field'
       (walk-gql-query->args field args subs)
       subs']
      [field' subs'])))

(defn walk-gql
  [{:keys [queries]}]
  {:queries (mapv (partial walk-gql-query true) queries)})

(defn ->gql-str
  [v]
  (->> v
       walk-gql
       dgql/graphql-query))

(defn walk-result-sub-kw [field sub v]
  (or (sql-field->clj-kw sub)
      sub))

(declare walk-result-sub-val-pair)

(defn walk-result-sub-map [field sub coll]
  (->> (for [[k v] coll]
         (walk-result-sub-val-pair sub k v))
       (into {})))

(defn walk-result-sub-vec [field sub coll]
  (mapv (partial walk-result-sub-map field sub)
        coll))

(defn timestamp? [kw]
  ;; HACK -- this would ideally be based on schema, not column names
  (#{:created :deleted :updated :expires_action :expires_read} kw))

(defn walk-result-sub-val [field sub v]
  (cond
    (coerce-to-long? sub) (ut/->long v) ; HACK -- Hasura returns bigints as string
    (timestamp? sub) (tc/to-sql-time v)
    :else v))

(defn walk-result-sub-val-pair
  [field sub v]
  [(walk-result-sub-kw field sub v)
   (cond (map? v) (walk-result-sub-map field sub v)
         (sequential? v) (walk-result-sub-vec field sub v)
         :else (walk-result-sub-val field sub v))])

(defn walk-result-recs
  [field rec]
  (->> (for [[k v] rec]
         (walk-result-sub-val-pair field k v))
       (into {})))

(defn walk-result->entity-kw
  [field recs]
  (-> field
      name
      (st/replace #"^vetd_" "")
      keyword
      sql-field->clj-kw))

(defn walk-result->field
  [field recs]
  field)

(defn walk-result-field-recs-pair
  [root? field recs]
  [(if root?
     (walk-result->entity-kw field recs)
     (walk-result->field field recs))
   (mapv (partial walk-result-recs field)
         recs)])

(defn walk-result
  [r]
  (->> (for [[k v] r]
         (walk-result-field-recs-pair true k v))
       (into {})))

(defmulti handle-from-graphql (fn [{gtype :type}] (gql-msg-types-str->kw gtype)))

(defn ws-send [ws msg]
  (try
    (ut/$- -> msg
           json/generate-string
           (ms/try-put! ws
                        $
                        1000))
    true
    (catch Exception e
      (com/log-error e)
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
  (log/info "hasura ws CLOSED"))

(defn ws-ib-handler
  [id data]
  (let [data' (json/parse-string data keyword)]
    (when-let [errors (-> data' :payload :errors)]
      (com/log-error errors))
    (-> data'
        (assoc :pdata (-> data' :payload :data walk-result))
        handle-from-graphql)))

(declare ensure-ws-setup)

(defn mk-ws []
  (log/info "hasura mk-ws")  
  (let [ws @(ah/websocket-client env/hasura-ws-url
                                 {:sub-protocols "graphql-ws"
                                  :max-frame-payload (* 8 65536)
                                  :max-frame-size (* 8 65536)})]
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
      (com/log-error e)
      false)))

(defn add-to-queue [msg]
  (swap! queue& conj msg))

(defn try-send [{:keys [ws] :as cn} msg]
  (if (ensure-ws-setup cn)
    (ws-send ws msg)
    (add-to-queue msg)))

(defn register-sub-id
  [sub-id resp-fn info]
  (swap! sub-id->created-ts&
         assoc sub-id (ut/now))
  (swap! sub-id->info&
         assoc sub-id info)
  (swap! sub-id->resp-fn&
         assoc sub-id resp-fn))

(defn unregister-sub-id
  [sub-id]
  (swap! sub-id->created-ts&
         dissoc sub-id)
  (swap! sub-id->info&
         dissoc sub-id)  
  (swap! sub-id->resp-fn&
         dissoc sub-id))

(defn respond-to-client
  [id msg]
  (try
    (when-let [f (@sub-id->resp-fn& (keyword id))]
      (f msg))
    (catch Throwable e
      (com/log-error e))))

(defmethod handle-from-graphql :connection-keep-alive [_]
  (reset! last-ka& (java.util.Date.))
  #_(println "KA"))

(defmethod handle-from-graphql :connection-ack [_]
  (swap! cn& assoc :state :ackd)
  (-> @cn&
      :ws
      send-queue))

(defmethod handle-from-graphql :connection-error [data]
  (com/log-error data))

(defmethod handle-from-graphql :data [{:keys [id pdata]}]
  (respond-to-client id {:mtype :data
                         :payload pdata}))

(defmethod handle-from-graphql :error [{:keys [id pdata] :as resp}]
  (com/log-error resp)
  (respond-to-client id {:mtype :error
                         :payload pdata}))

(defmethod handle-from-graphql :complete [{:keys [id pdata]}]
  (unregister-sub-id id)
  (respond-to-client id {:mtype :complete
                         :payload pdata}))

(defn unsub [qual-sub-id]
  (when (@sub-id->resp-fn& qual-sub-id)
    (try-send @cn&
              {:type (gql-msg-types-kw->str :stop)
               :id qual-sub-id})
    (unregister-sub-id qual-sub-id)))

(defn sync-query
  [queries]
  (try
    (let [r (-> @(ah/post env/hasura-http-url
                          {:body (json/generate-string
                                  {:query (->gql-str {:queries queries})})})
                :body
                slurp)]
      (-> r
          (json/parse-string keyword)
          :data
          walk-result #_process-result))
    (catch Exception e
      (com/log-error e (try (some-> e .getData :body slurp)
                            (catch Exception e2 "")))
      (throw e))))

(defn ez-sync-query [query]
  (-> [query]
      sync-query
      vals
      ffirst))

(defn select-orgs-by-session-id [token]
  (->> (db/hs-query {:select [:o.id]
                     :from [[:sessions :s]]
                     :join [[:users :u] [:and
                                         [:= :u.deleted nil]
                                         [:= :u.id :s.user_id ]]
                            [:memberships :m] [:and
                                               [:= :m.deleted nil]
                                               [:= :m.user_id :u.id]]
                            [:orgs :o] [:and
                                        [:= :o.deleted nil]
                                        [:= :o.id :m.org_id]]]
                     :where [:= :s.token token]})
       (mapv :id)))

(defn admin-session? [session-token]
  (-> (db/hs-query {:select [:a.id]
                    :from [[:sessions :s]]
                    :join [[:admins :a] [:and
                                         [:= :a.user_id :s.user_id]
                                         [:= :a.deleted nil]]]
                    :where [:and
                            [:= :s.token session-token]
                            [:= :s.deleted nil]]})
      empty?
      not))

(defmulti secure-gql? (fn [[field maybe-map] session-token]
                       field))

(defmethod secure-gql? :default [_ _]  true)

(defmethod secure-gql? :orgs
  [[_ maybe-map :as q] session-token]
  (if (and (some #{:stack-items} 
                 (tree-seq vector? seq q))
           (map? maybe-map))
    (let [{:keys [id idstr]} maybe-map
          restrict-org (cond id [:= :o.id id]
                             idstr [:= :o.idstr idstr])]
      (when restrict-org
        (->> (db/hs-query {:select [:%count.s.id]
                           :from [[:sessions :s]]
                           :join [[:users :u] [:and
                                               [:= :u.deleted nil]
                                               [:= :u.id :s.user_id ]]

                                  [:memberships :m] [:and
                                                     [:= :m.deleted nil]
                                                     [:= :m.user_id :u.id]]

                                  [:group_org_memberships :gom]
                                  [:and
                                   [:= :gom.deleted nil]
                                   [:= :gom.org_id :m.org_id]]
                                  
                                  [:group_org_memberships :gom2]
                                  [:and
                                   [:= :gom2.deleted nil]
                                   [:= :gom2.group_id :gom.group_id]]

                                  [:orgs :o]
                                  [:and
                                   [:= :o.deleted nil]
                                   [:= :gom2.org_id :o.id]
                                   restrict-org]]
                           :where [:= :s.token session-token]})
             first
             :count
             (= 1))))
    true))

(defmethod secure-gql? :rounds
  [[_ maybe-map] session-token]
  (if (map? maybe-map)
    (let [{:keys [id idstr]} maybe-map
          restrict (cond id [:= :r.id id]
                         idstr [:= :r.idstr idstr])]
      (if restrict
        (or (->> (db/hs-query {:select [:%count.s.id]
                               :from [[:sessions :s]]
                               :join [[:users :u] [:and
                                                   [:= :u.deleted nil]
                                                   [:= :u.id :s.user_id ]]
                                      [:memberships :m] [:and
                                                         [:= :m.deleted nil]
                                                         [:= :m.user_id :u.id]]
                                      [:orgs :o] [:and
                                                  [:= :o.deleted nil]
                                                  [:= :o.id :m.org_id]]
                                      [:rounds :r] [:and
                                                    [:= :r.deleted nil]
                                                    [:= :o.id :r.buyer_id]
                                                    restrict]]
                               :where   [:= :s.token session-token]})
                 first
                 :count
                 (= 1))
            (->> (db/hs-query {:select [:%count.s.id]
                               :from [[:sessions :s]]
                               :join [[:users :u] [:and
                                                   [:= :u.deleted nil]
                                                   [:= :u.id :s.user_id ]]

                                      [:memberships :m] [:and
                                                         [:= :m.deleted nil]
                                                         [:= :m.user_id :u.id]]

                                      [:group_org_memberships :gom]
                                      [:and
                                       [:= :gom.deleted nil]
                                       [:= :gom.org_id :m.org_id]]
                                  
                                      [:group_org_memberships :gom2]
                                      [:and
                                       [:= :gom2.deleted nil]
                                       [:= :gom2.group_id :gom.group_id]]

                                      [:rounds :r] [:and
                                                    [:= :r.deleted nil]
                                                    [:= :gom2.org_id :r.buyer_id]
                                                    restrict]]
                               :where   [:= :s.token session-token]})
                 first
                 :count
                 (= 1)))
        true))
    true))

;; ws from client
(defmethod com/handle-ws-inbound :graphql
  [{:keys [sub-id query admin? subscription? stop session-token] :as msg} ws-id resp-fn]
  (def q1 query)
  #_ (clojure.pprint/pprint q1)
  #_(clojure.pprint/pprint msg)
  (if (or stop
          (and admin? (admin-session? session-token))
          (some-> query :queries first (secure-gql? session-token)))
    (let [qual-sub-id (keyword (name ws-id)
                               (str sub-id))]
      (if-not stop
        (do
          (let [f (if subscription?
                    resp-fn
                    #(do (unsub qual-sub-id)
                         (resp-fn %)))]
            (register-sub-id qual-sub-id f
                             {:created (ut/now)
                              :query query
                              :admin admin?
                              :sub-id sub-id
                              :qual-sub-id qual-sub-id
                              :session-token session-token}))
          (com/reg-ws-on-close-fn ws-id
                                  qual-sub-id
                                  (partial unsub qual-sub-id))
          (try-send @cn&
                    {:type :start
                     :id qual-sub-id
                     :payload {:query (format "%s %s"
                                              (if subscription?
                                                "subscription"
                                                "query")
                                              (->gql-str query))}}))
        (do
          (com/unreg-ws-on-close-fn ws-id qual-sub-id)
          (unregister-sub-id qual-sub-id)
          (try-send @cn&
                    {:type (gql-msg-types-kw->str :stop)
                     :id qual-sub-id})))
      nil)
    {:authorization-failed? true}))


#_(send-terminate)

#_
(clojure.pprint/pprint 
 (json/parse-string
  (slurp (clojure.java.io/resource "hasura-metadata.json"))
  keyword))

