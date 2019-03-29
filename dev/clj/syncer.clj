(ns syncer
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as hs]
            [com.vetd.app.server :as svr]
            [clojure.core.async :as a]
            [aleph.http :as ah]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [cheshire.core :as json]))

#_(defonce ws& (atom nil))

#_(defn req-profile-forms&dependents [ws]
  (svr/respond-transit {:cmd :graphql
                        :sub-id (keyword (gensym))
                        :return :yes
                        :subscription? false
                        :query {:queries
                                [[:forms
                                  {:ftype ["vendor-profile" "product-profile"]
                                   :deleted nil}
                                  [:id
                                   :idstr
                                   :created
                                   :updated
                                   :deleted
                                   :form-template-id
                                   :title
                                   :subject
                                   :descr
                                   :notes
                                   :ftype
                                   :fsubtype
                                   :from-org-id
                                   :from-user-id
                                   :to-org-id
                                   :to-user-id
                                   :status
                                   #_[:prompts {:deleted nil}
                                    [:id 
                                     :idstr
                                     :created
                                     :updated
                                     :deleted
                                     :prompt
                                     :descr
                                     #_[:fields {:deleted nil}
                                      [:id 
                                       :idstr
                                       :created
                                       :updated
                                       :deleted
                                       :prompt_id
                                       :fname
                                       :descr
                                       :ftype
                                       :fsubtype
                                       :list_qm
                                       :sort]]]]]]]}}
                       ws))

(defn ws-on-closed []
  (println "ws-on-closed"))

(defn ws-ib-handler
  [ch data]
  (try
    (println "ws-ib-handler")
    (clojure.pprint/pprint data)
    (let [d (svr/read-transit-string data)
          mtype (some-> d :response :mtype)
          payload (-> d :response :payload)]
      #_(clojure.pprint/pprint d)
      #_      (println mtype)
      #_      (println (= mtype :data))
      (when (= mtype :data)
          (a/>!! ch payload)
          (a/close! ch))
      #_(when (= mtype :data)
        (reset! ch payload)))
    (catch Throwable t
      (clojure.pprint/pprint t)
      nil)))

(defn mk-ws [ch]
  (let [ws @(ah/websocket-client #_"ws://localhost:5080/ws"
                                 #_"wss://app.vetd.com/ws"
                                 "ws://18.204.1.191:5080/ws"
                                 )]
    (ms/on-closed ws ws-on-closed)
    (ms/consume (partial ws-ib-handler ch) ws)
    (Thread/sleep 1000)
    ws))

#_(mk-ws)

#_(svr/respond-transit {:cmd :graphql
                        :sub-id (keyword (gensym))
                      :return :yes
                      :subscription? false
                      :query {:queries [[:forms {:_limit 1} [:id :__typename]]]}}
                     @ws&)

(defn req-profile-forms&dependents [ws]
  (svr/respond-transit {:cmd :graphql
                        :sub-id (keyword (gensym))
                        :return :yes
                        :subscription? false
                        :query {:queries
                                [[:forms
                                  {:ftype ["vendor-profile" "product-profile"]
                                   :deleted nil}
                                  [:id
                                   :idstr
                                   :created
                                   :updated
                                   :deleted
                                   :form-template-id
                                   :title
                                   :subject
                                   :descr
                                   :notes
                                   :ftype
                                   :fsubtype
                                   :from-org-id
                                   :from-user-id
                                   :to-org-id
                                   :to-user-id
                                   :status
                                   [:prompts {:deleted nil}
                                    [:id 
                                     :idstr
                                     :created
                                     :updated
                                     :deleted
                                     :prompt
                                     :descr
                                     [:fields {:deleted nil}
                                      [:id 
                                       :idstr
                                       :created
                                       :updated
                                       :deleted
                                       :prompt_id
                                       :fname
                                       :descr
                                       :ftype
                                       :fsubtype
                                       :list_qm
                                       :sort]]]]]]]}}
                       ws))


(defn sync-profile-forms-from-prod []
  (let [ch (a/chan 1)
        ;res (atom nil)
        ws (mk-ws ch #_ res)]
    (req-profile-forms&dependents ws)
    #_(Thread/sleep 5000)
    (let [forms #_@res (a/alt!!
                  (a/timeout 5000) :timeout
                  ch ([v] v))]
      (println "forms:")
      (clojure.pprint/pprint forms))
    (.close ws)
    true))

#_ (sync-profile-forms-from-prod)

#_(.close @ws&)

