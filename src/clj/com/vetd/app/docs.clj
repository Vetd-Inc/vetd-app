(ns com.vetd.app.docs
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]))

(defn create-doc [])

(defn create-req-template [])

(defn insert-req
  [{:keys [req-temp-id title descr notes from-org-id
           from-user-id to-org-id to-user-id status]}]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :reqs
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :req_template_id req-temp-id
                     :title title
                     :descr descr
                     :notes notes
                     :from_org_id from-org-id
                     :from_user_id from-user-id
                     :to_org_id to-org-id
                     :to_user_id to-user-id
                     :status status})
        first)))

(defn insert-doc
  [{:keys [title dtype descr notes from-org-id
           from-user-id to-org-id to-user-id]}]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :docs
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :title title
                     :descr descr
                     :notes notes
                     :from_org_id from-org-id
                     :from_user_id from-user-id
                     :to_org_id to-org-id
                     :to_user_id to-user-id})
        first)))

(defn insert-req-prompt
  [req-id prompt-id sort']
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :req_prompt
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :req_id req-id
                     :prompt_id prompt-id
                     :sort sort'})
        first)))

(defn create-req-from-template
  [{:keys [req-temp-id from-org-id from-user-id
           title descr status notes to-org-id
           to-user-id] :as m}]
  (let [rtype (-> (db/hs-query {:select [:rtype]
                                :from [:req_template]
                                :where [:= :id req-temp-id]})
                  first
                  :rtype)
        {req-id :id :as req} (-> m
                                 (assoc :rtype rtype)
                                 insert-req)
        ps (db/hs-query {:select [:prompt_id :sort]
                         :from [:req_template_prompt]
                         :where [:= :req_template_id req-temp-id]})]
    (doseq [{prompt-id :prompt_id sort' :sort} ps]
      (insert-req-prompt req-id prompt-id sort'))
    req))

(defn create-req&preposal
  [{:keys [req-temp-id buyer-org-id buyer-user-id
           pitch price-val price-unit req-title req-descr
           req-notes vendor-org-id vendor-user-id
           doc-title doc-type doc-descr doc-notes]}]
  (let [req (create-req-from-template {:req-temp-id req-temp-id
                                       :title req-title
                                       :descr req-descr
                                       :notes req-notes
                                       :from-org-id buyer-org-id
                                       :from-user-id buyer-user-id
                                       :to-org-id vendor-org-id
                                       :to-user-id vendor-user-id
                                       :status nil})
        [id idstr] (ut/mk-id&str)]
    (insert-doc {:title doc-title
                 :dtype doc-type
                 :descr doc-descr
                 :notes doc-notes
                 :from-org-id vendor-org-id
                 :from-user-id vendor-user-id
                 :to-org-id buyer-org-id
                 :to-user-id buyer-user-id})))
