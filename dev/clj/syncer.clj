(ns syncer
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as hs]
            [com.vetd.app.server :as svr]
            [com.vetd.app.docs :as docs]            
            [clojure.core.async :as a]
            [aleph.http :as ah]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [cheshire.core :as json]))

(defn ws-on-closed []
  (println "ws-on-closed"))

(defn ws-ib-handler
  [ch data]
  (try
    (let [d (svr/read-transit-string data)
          mtype (some-> d :response :mtype)
          payload (-> d :response :payload)]
      (when (= mtype :data)
          (a/>!! ch payload)
          (a/close! ch)))
    (catch Throwable t
      (clojure.pprint/pprint t)
      nil)))

;; this sucks. ws and alb not friends because no cookies -- Bill
(def prod-ip "52.91.178.190")

(defn mk-ws [ch]
  (let [ws @(ah/websocket-client #_"ws://localhost:5080/ws"
                                 (format "ws://%s:5080/ws"
                                         prod-ip))]
    (ms/on-closed ws ws-on-closed)
    (ms/consume (partial ws-ib-handler ch) ws)
    (Thread/sleep 1000)
    ws))

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
                                   [:prompts {:deleted nil
                                              :ref-deleted nil}
                                    [:id 
                                     :idstr
                                     :created
                                     :updated
                                     :deleted
                                     :prompt
                                     :descr
                                     :form-id
                                     :sort
                                     [:fields {:deleted nil}
                                      [:id 
                                       :idstr
                                       :created
                                       :updated
                                       :deleted
                                       :prompt-id
                                       :fname
                                       :descr
                                       :ftype
                                       :fsubtype
                                       :list?
                                       :sort]]]]]]]}}
                       ws))


(defn req-some-form-templates&dependents [ws]
  (svr/respond-transit {:cmd :graphql
                        :sub-id (keyword (gensym))
                        :return :yes
                        :subscription? false
                        :query {:queries
                                [[:form-templates
                                  {:ftype ["vendor-profile" "product-profile"
                                           "round-initiation"]
                                   :deleted nil}
                                  [:id
                                   :idstr
                                   :created
                                   :updated
                                   :deleted
                                   :title
                                   :descr
                                   :ftype
                                   :fsubtype
                                   [:prompts {:deleted nil
                                              :rp-deleted nil}
                                    [:id 
                                     :idstr
                                     :created
                                     :updated
                                     :deleted
                                     :prompt
                                     :term
                                     :descr
                                     :form-template-id
                                     :sort
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
                                       :list?
                                       :sort]]]]]]]}}
                       ws))

(defn upsert-form&dependents
  [{:keys [id prompts] :as form}]
  (docs/upsert-form (dissoc form :prompts) true)
  (docs/upsert-form-prompts id prompts true))

(defn upsert-form-template&dependents
  [{:keys [id prompts] :as form-template}]
  (docs/upsert-form-template (dissoc form-template :prompts) true)
  (docs/upsert-form-template-prompts id prompts true))

(defn sync-profile-forms-from-prod []
  (let [ch (a/chan 1)
        ws (mk-ws ch)]
    (req-profile-forms&dependents ws)
    (let [forms (a/alt!!
                  (a/timeout 5000) :timeout
                  ch ([v] v))]
      (->> forms :forms (mapv upsert-form&dependents)))
    (.close ws)
    true))

(defn sync-some-form-templates-from-prod []
  (let [ch (a/chan 1)
        ws (mk-ws ch)]
    (req-some-form-templates&dependents ws)
    (let [form-templates (a/alt!!
                           (a/timeout 5000) :timeout
                           ch ([v] v))]
      (->> form-templates :form-templates
           (mapv upsert-form-template&dependents)))
    (.close ws)
    true))

#_ (sync-profile-forms-from-prod)

#_ (sync-some-form-templates-from-prod)

