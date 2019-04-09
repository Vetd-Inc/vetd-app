(ns com.vetd.app.docs
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]
            clojure.data))

(defn get-latest-form-doc-by-ftype&from-org
  [ftype doc-from-org-id]
  (-> [[:form-docs {:ftype ftype
                    :doc-from-org-id doc-from-org-id
                    :_order_by {:created :desc}
                    :_limit 1}
        [:id :title :doc-id :doc-title :doc-from-org-id
         :ftype :fsubtype
         [:from-org [:id :oname]]
         [:from-user [:id :uname]]
         [:to-org [:id :oname]]
         [:to-user [:id :uname]]
         [:prompts {:deleted nil
                    :_order_by {:sort :asc}}
          [:id :idstr :prompt :descr
           [:fields
            [:id :idstr :fname :ftype
             :fsubtype :list?]]]]
         [:responses
          [:id :prompt-id :notes
           [:fields [:id :pf-id :idx :sval :nval :dval]]]]]]]
      ha/sync-query
      :form-docs
      first))

(defn get-latest-form-doc-by-ftype&subject
  [ftype subject]
  (-> [[:form-docs {:ftype ftype
                    :doc-subject subject
                    :_order_by {:created :desc}
                    :_limit 1}
        [:id :title :doc-id :doc-title :doc-from-org-id
         :ftype :fsubtype
         [:from-org [:id :oname]]
         [:from-user [:id :uname]]
         [:to-org [:id :oname]]
         [:to-user [:id :uname]]
         [:prompts {:deleted nil
                    :_order_by {:sort :asc}}
          [:id :idstr :prompt :descr
           [:fields
            [:id :idstr :fname :ftype
             :fsubtype :list?]]]]
         [:responses
          [:id :prompt-id :notes
           [:fields [:id :pf-id :idx :sval :nval :dval]]]]]]]
      ha/sync-query
      :form-docs
      first))

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
           ftype fsubtype subject
           id idstr]}
   & [use-id?]]
  (let [[id idstr] (if use-id?
                     [id idstr]
                     (ut/mk-id&str))]
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

(defn insert-form-template
  [{:keys [title descr notes
           ftype fsubtype subject
           id idstr]}
   & [use-id?]]
  (let [[id idstr] (if use-id?
                     [id idstr]
                     (ut/mk-id&str))]
    (-> (db/insert! :form_templates
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :ftype ftype
                     :fsubtype fsubtype
                     :title title
                     :descr descr})
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

(defn insert-prompt
  [{:keys [prompt descr id idstr]} & [use-id?]]
  (let [[id idstr] (if use-id?
                     [id idstr]
                     (ut/mk-id&str))]
    (-> (db/insert! :prompts
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :prompt prompt
                     :descr descr})
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

(defn insert-form-template-prompt
  [form-template-id prompt-id sort']
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :form_template_prompt
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :form_template_id form-template-id
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

(defn insert-default-prompt-field
  [prompt-id {sort' :sort}]
  (let [[id idstr] (ut/mk-id&str)]
    (->> (db/insert! :prompt_fields
                     {:id id
                      :idstr idstr
                      :created (ut/now-ts)
                      :updated (ut/now-ts)
                      :deleted nil
                      :prompt_id prompt-id
                      :fname "New Field"
                      :list_qm false
                      :descr ""
                      :ftype "s"
                      :fsubtype "single"})
         first)))

(defn insert-prompt-field
  [{:keys [prompt-id fname list? descr ftype fsubtype id idstr]} & [use-id?]]
  (let [[id idstr] (if use-id?
                     [id idstr]
                     (ut/mk-id&str))]
    (->> (db/insert! :prompt_fields
                     {:id id
                      :idstr idstr
                      :created (ut/now-ts)
                      :updated (ut/now-ts)
                      :deleted nil
                      :prompt_id prompt-id
                      :fname fname
                      :list_qm list?
                      :descr descr
                      :ftype ftype
                      :fsubtype fsubtype})
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
                         :where [:and
                                 [:= :form_template_id form-temp-id]
                                 [:= :deleted nil]]})]
    (doseq [{prompt-id :prompt_id sort' :sort} ps]
      (insert-form-prompt form-id prompt-id sort'))
    form))

(defn- update-deleted [tbl-kw id]
  (db/hs-exe! {:update tbl-kw
               :set {:deleted (ut/now-ts)}
               :where [:= :id id]}))

(def delete-template-prompt (partial update-deleted :form_template_prompt))
(def delete-form-prompt-field (partial update-deleted :prompt_fields))
(def delete-form-prompt (partial update-deleted :form_prompt))
(def delete-form-template-prompt (partial update-deleted :form_template_prompt))

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

(defn- get-child-responses
  [doc-id]
  (some->> [[:docs {:id doc-id}
             [:id
              [:responses
               {:ref-deleted nil}
               [:id :ref-id 
                [:fields {:deleted nil}
                 [:sval :nval :dval
                  [:prompt-field [:fname]]]]]]]]]
           ha/sync-query
           :docs
           first
           :responses))

(defn response-fields-eq?
  [old-field {:keys [response ftype]}]
  (let [{:keys [state]} (first response)]
    (case ftype
      "s" (= (:sval old-field)
             state)
      "n" (= (:nval old-field)
             (ut/->long state))
      "d" (= (str (:dval old-field)) ;; HACK
             state)
      "e" (= (:sval old-field)
             state))))

(defn update-response-from-form-doc
  [doc-id {old-fields :fields :keys [ref-id]} {new-fields :fields :keys [response]}]
  ;; TODO handle if old or new fields are missing
  (cond (and (not-empty old-fields) (not-empty new-fields))
        (let [old-fields' (group-by (comp :fname :prompt-field) old-fields)
              new-fields' (group-by :fname new-fields)
              resp-ids (keys new-fields')]
          (when (->> resp-ids
                     (map #(response-fields-eq? (-> % old-fields' first)
                                                (-> % new-fields' first)))
                     (some false?))
            (update-deleted :doc_resp ref-id)
            (create-attached-doc-response doc-id
                                          {:prompt-id (:prompt-id response)
                                           :fields new-fields})))

        (not-empty new-fields)
        (create-attached-doc-response doc-id
                                      {:prompt-id (:prompt-id response)
                                       :fields new-fields})))



(defn update-responses-from-form-doc
  [{:keys [doc-id prompts]}]
  (let [old-responses (->> doc-id
                           get-child-responses
                           (group-by :id))
        new-responses (->> prompts
                           (group-by #(-> % :response :id)))]
    (doseq [k (keys new-responses)]
      (update-response-from-form-doc doc-id
                                     (-> k old-responses first)
                                     (-> k new-responses first)))))

#_(clojure.pprint/pprint or1)

#_(def fd1 $s/*)

#_(clojure.pprint/pprint  fd1)

#_(update-doc-from-form-doc fd1)

(defn update-doc-from-form-doc
  [{:keys [id doc-id doc-title responses from-org from-user to-org to-user
           doc-descr doc-notes doc-dtype doc-dsubtype product]
    :as form-doc}]
  (db/update-any!
   {:id doc-id
    :title doc-title
    :dtype doc-dtype
    :dsubtype doc-dsubtype
    :subject (:id product)
    :descr doc-descr
    :notes doc-notes
    :form_id id
    ;; fields below are reveresed intentionally
    :from_org_id (:id to-org)
    :from_user_id (:id to-user)
    :to_org_id (:id from-org)
    :to_user_id (:id from-user)}
   :docs)
  (update-responses-from-form-doc form-doc))

(defn create-blank-form-template-prompt
  [form-template-id]
  (let [{:keys [id]} (insert-prompt {:prompt "Empty Prompt"
                                     :descr "Empty Description"})]
    (insert-form-template-prompt form-template-id
                                 id
                                 100)))

(defn- upsert*
  [exists?-fn ins-fn dissoc-fields entity & [use-id?]]
  (if (exists?-fn entity)
    ;; TODO Don't use db/update-any! -- not efficient
    (-> (apply dissoc entity dissoc-fields)
        ha/walk-clj-kw->sql-field
        db/update-any!)
    (ins-fn entity use-id?)))

(defn form-exists? [{:keys [id] :as form}]
  (-> [[:forms {:id id} [:id]]]
      ha/sync-query
      :forms
      empty?
      not))

(defn upsert-form [{:keys [id] :as form} & [use-id?]]
  (upsert* form-exists?
           insert-form
           [:prompts]
           form
           use-id?))

(defn prompt-exists? [{:keys [id] :as prompt} & [use-id?]]
  (-> [[:prompts {:id id} [:id]]]
      ha/sync-query
      :prompts
      empty?
      not))

(defn upsert-prompt [{:keys [id] :as prompt} & [use-id?]]
  (upsert* prompt-exists?
           insert-prompt
           [:fields :form-template-id :sort :form-id]
           prompt
           use-id?))

(defn form-template-exists? [{:keys [id] :as form-template}]
  (-> [[:form-templates {:id id} [:id]]]
      ha/sync-query
      :form-templates
      empty?
      not))

(defn upsert-form-template [{:keys [id] :as form-template} & [use-id?]]
  (upsert* form-template-exists?
           insert-form-template
           [:prompts]
           form-template
           use-id?))

(defn prompt-field-exists? [{:keys [id prompt-id] :as field}]
  (-> [[:prompts {:id prompt-id}
        [:id
         [:fields {:id id} [:id]]]]]
      ha/sync-query
      :prompts
      first
      :fields
      empty?
      not))

(defn upsert-prompt-field [{:keys [id prompt-id] :as field} & [use-id?]]
  (upsert* prompt-field-exists?
           insert-prompt-field
           []
           field
           use-id?))

(defn- get-child-prompts
  [field id]
  (some->> [[field {:id id}
             [:id
              [:prompts [:id :rpid :sort]]]]]
           ha/sync-query
           field
           first
           :prompts))

(defn upsert-form-prompts* [old-prompts new-prompts]
  (let [[a b] (clojure.data/diff
               (group-by :id old-prompts)
               (group-by :id new-prompts))]
    {:kill-rpids (->> a
                      vals
                      flatten
                      (filter :id)
                      (map :rpid))
     :new-form-prompts (->> b
                            vals
                            flatten
                            (filter :id))}))

(defn upsert-prompt-parent&children*
  [id etype prompt-ins-fn new-prompts & [use-id?]]
  (let [old-prompts (get-child-prompts etype id)
        {:keys [kill-rpids new-form-prompts]}
        (upsert-form-prompts* old-prompts new-prompts)]
    (doseq [form-prompt-id kill-rpids]
      (delete-form-prompt form-prompt-id))
    (doseq [{:keys [fields] prompt-id :id sort' :sort :as prompt} new-form-prompts]
      (mapv #(upsert-prompt-field % true) fields)
      (upsert-prompt prompt use-id?)
      (prompt-ins-fn id prompt-id sort'))))

(defn upsert-form-prompts
  [form-id new-prompts & [use-id?]]
  (upsert-prompt-parent&children* form-id
                                  :forms
                                  insert-form-prompt
                                  new-prompts
                                  use-id?))

(defn upsert-form-template-prompts
  [form-template-id new-prompts & [use-id?]]
  (upsert-prompt-parent&children* form-template-id
                                  :form-templates
                                  insert-form-template-prompt
                                  new-prompts
                                  use-id?))
