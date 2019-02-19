(ns com.vetd.app.docs
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]))

;; TODO use prompt-field data type
(defn convert-field-val
  [v ftype fsubtype]
  (case ftype
    "n" {:sval (str v)
         :nval (ut/->long v)
         :dval nil}
    "s" {:sval v
         :nval nil
         :dval nil}
    "e" {:sval v
         :nval nil
         :dval nil}
    "d" (throw (Exception. "TODO convert-field-val does not support dates"))))

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
           from-user-id to-org-id to-user-id
           dtype dsubtype form-id subject]}]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :docs
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :title title
                     :dtype dtype
                     :dsubtype dsubtype
                     :descr descr
                     :notes notes
                     :subject subject
                     :form_id form-id
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

(defn insert-response
  [{:keys [org-id prompt-id notes user-id]}]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :responses
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :prompt_id prompt-id
                     :notes notes
                     :user_id user-id})
        first)))

(defn insert-doc-response
  [doc-id resp-id]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :doc_resp
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :doc_id doc-id
                     :resp_id resp-id})
        first)))

(defn insert-response-field
  [resp-id {:keys [prompt-field-id idx sval nval dval response ftype fsubtype]}]
  (let [[id idstr] (ut/mk-id&str)]
    (->> (merge {:id id
                 :idstr idstr
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :pf_id prompt-field-id
                 :idx idx
                 :sval sval
                 :nval nval
                 :dval dval
                 :resp_id resp-id}
                  ;; TODO support multiple response fields (for where list? = true)
                (when-let [v (-> response first :state)]
                  (convert-field-val v ftype fsubtype)))
         (db/insert! :resp_fields)
         first)))

(defn create-attached-doc-response
  [doc-id {:keys [org-id prompt-id notes user-id fields] :as resp}]
  (let [{resp-id :id} (insert-response resp)]
    (insert-doc-response doc-id resp-id)
    (doseq [{:keys [id] :as f} fields]
      (insert-response-field resp-id
                             (assoc f
                                    :prompt-field-id
                                    id)))))

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

;; necessary? not used - Bill
(defn create-form&doc
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

(defn create-doc-from-form-doc
  [{:keys [id doc-title prompts from-org from-user to-org to-user
           doc-descr doc-notes doc-dtype doc-dsubtype product]}]
  (let [{doc-id :id} (insert-doc {:title doc-title
                                  :dtype doc-dtype
                                  :dsubtype doc-dsubtype
                                  :subject (:id product)
                                  :descr doc-descr
                                  :notes doc-notes
                                  :form-id id
                                  ;; fields below are reveresed intentionally
                                  :from-org-id (:id to-org)
                                  :from-user-id (:id to-user)
                                  :to-org-id (:id from-org)
                                  :to-user-id (:id from-user)})]
    (doseq [{prompt-id :id :keys [response fields]} prompts]
      (create-attached-doc-response doc-id
                                    {:org-id (:id to-org)
                                     :user-id (:id to-user)
                                     :prompt-id prompt-id
                                     :fields fields}))))

(defn update-doc-from-form-doc [form-doc])

#_(clojure.pprint/pprint 
 (ha/sync-query [[:form-templates {:ftype "preposal"}
                  [:id :idstr :ftype :fsubtype
                   [:prompts
                    [:id :idstr :prompt :descr
                     [:fields
                      [:id :idstr :fname :descr :ftype :list? :sort]]]]]]]))
