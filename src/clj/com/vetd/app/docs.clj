(ns com.vetd.app.docs
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]))

(defn create-doc [])

(defn create-form-template [])

(defn insert-form
  [{:keys [form-temp-id title descr notes from-org-id
           from-user-id to-org-id to-user-id status
           ftype fsubtype subject]}]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :forms
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :form_template_id form-temp-id
                     :ftype ftype
                     :fsubtype fsubtype
                     :title title
                     :descr descr
                     :notes notes
                     :subject subject
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

(defn insert-form-prompt
  [form-id prompt-id sort']
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :form_prompt
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :form_id form-id
                     :prompt_id prompt-id
                     :sort sort'})
        first)))

(defn create-form-from-template
  [{:keys [form-temp-id from-org-id from-user-id
           title descr status notes to-org-id
           to-user-id] :as m}]
  (let [ftypes (-> {:select [:ftype :fsubtype]
                    :from [:form_templates]
                    :where [:= :id form-temp-id]}
                   db/hs-query
                   first)
        {form-id :id :as form} (-> m
                                   (merge ftypes)
                                   insert-form)
        ps (db/hs-query {:select [:prompt_id :sort]
                         :from [:form_template_prompt]
                         :where [:= :form_template_id form-temp-id]})]
    (doseq [{prompt-id :prompt_id sort' :sort} ps]
      (insert-form-prompt form-id prompt-id sort'))
    form))

(defn create-form&preposal
  [{:keys [form-temp-id buyer-org-id buyer-user-id
           pitch price-val price-unit form-title form-descr
           form-notes vendor-org-id vendor-user-id
           doc-title doc-type doc-descr doc-notes]}]
  (let [form (create-form-from-template {:form-temp-id form-temp-id
                                         :title form-title
                                         :descr form-descr
                                         :notes form-notes
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

(defn find-latest-form-template-id [where]
  (-> {:select [:id]
       :from [:form_templates]
       :where where
       :order-by [[:created :desc]]
       :limit 1}
      db/hs-query
      first
      :id))

(defn create-preposal-req-form
  [{:keys [to-org-id to-user-id from-org-id from-user-id prod-id] :as prep-req}]
  (create-form-from-template
   (merge prep-req
          {:form-temp-id
           (find-latest-form-template-id [:= :ftype "preposal"])
           :title (str "Preposal Request " (gensym ""))
           :status "init"
           :subject prod-id})))

#_(clojure.pprint/pprint 
 (ha/sync-query [[:form-templates {:ftype "preposal"}
                  [:id :idstr :ftype :fsubtype
                   [:prompts
                    [:id :idstr :prompt :descr
                     [:fields
                      [:id :idstr :fname :descr :dtype :list? :sort]]]]]]]))
