(ns com.vetd.app.docs
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]
            clojure.data))

(defmulti handle-doc-creation (fn [{:keys [dtype]}] (keyword dtype)))

(defmethod handle-doc-creation :default [_])

;; TODO use prompt-field data type
(defn convert-field-val
  [v ftype fsubtype]
  (case ftype
    "n" {:sval (str v)
         :nval (ut/->long v)
         :dval nil
         :jval nil}
    "s" {:sval v
         :nval nil
         :dval nil
         :jval nil}
    "e" {:sval v
         :nval nil
         :dval nil
         :jval nil}
    "j" {:sval nil
         :nval nil
         :dval nil
         :jval v}

    "d" (throw (Exception. "TODO convert-field-val does not support dates"))))

(defn insert-form
  [{:keys [form-template-id title descr notes from-org-id
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
                     :form_template_id form-template-id
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
  (let [[id idstr] (ut/mk-id&str)
        d (-> (db/insert! :docs
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
              first)]
    ;; TODO doing this here isn't great because responses likely won't be inserted yet
    (future (handle-doc-creation d))
    d))

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
  [resp-id {:keys [prompt-field-id idx sval nval dval jval response ftype fsubtype]}]
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
                 :jval jval                 
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

(defn select-form-template-prompts-by-parent-id
  [form-template-id]
  (->> (db/hs-query {:select [:prompt_id :sort]
                     :from [:form_template_prompt]
                     :where [:and
                             [:= :form_template_id form-template-id]
                             [:= :deleted nil]]})
       (map ha/walk-sql-field->clj-kw)))

(defn create-form-from-template
  [{:keys [form-template-id from-org-id from-user-id
           title descr status notes to-org-id
           to-user-id] :as m}]
  (let [ftypes (-> {:select [:ftype :fsubtype]
                    :from [:form_templates]
                    :where [:= :id form-template-id]}
                   db/hs-query
                   first)
        {form-id :id :as form} (-> m
                                   (merge ftypes)
                                   insert-form)
        ps (db/hs-query {:select [:prompt_id :sort]
                         :from [:form_template_prompt]
                         :where [:and
                                 [:= :form_template_id form-template-id]
                                 [:= :deleted nil]]})]
    (doseq [{prompt-id :prompt_id sort' :sort} ps]
      (insert-form-prompt form-id prompt-id sort'))
    form))

#_ {:form-template-id 732891754223}

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
          {:form-template-id
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
               [:id :ref-id :prompt-id
                [:fields {:deleted nil}
                 [:sval :nval :dval :jval
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
             state)
      "j" (= (:jval old-field)
             state))))

(defn update-response-from-form-doc
  [doc-id {old-fields :fields :keys [ref-id]} {prompt-id :id new-fields :fields :keys [response]}] 
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
                                          {:prompt-id (or (:prompt-id response)
                                                          prompt-id) ;; TODO I don't know -- Bill
                                           :fields new-fields})))

        (not-empty new-fields)
        (create-attached-doc-response doc-id
                                      {:prompt-id prompt-id
                                       :fields new-fields})))

(defn update-responses-from-form-doc
  [{:keys [doc-id prompts]}]
  (let [old-responses (->> doc-id
                           get-child-responses
                           (group-by :prompt-id))
        new-responses (->> prompts
                           (group-by :id))]
    (doseq [k (keys new-responses)]
      (update-response-from-form-doc doc-id
                                     (-> k old-responses first)
                                     (-> k new-responses first)))))

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

(defn prompt-exists? [{:keys [id] :as prompt}]
  (-> [[:prompts {:id id} [:id]]]
      ha/sync-query
      :prompts
      empty?
      not))

(defn form-prompt-exists? [{:keys [form-id prompt-id] :as form-prompt}]
  (-> {:select [[:%count.* :c]]
       :from [:form_prompt]
       :where [:and
               [:= :form_id form-id]
               [:= :prompt_id prompt-id]]}
      db/hs-query
      first
      :c
      zero?
      not))

(defn upsert-prompt [{:keys [id] :as prompt} & [use-id?]]
  (upsert* prompt-exists?
           insert-prompt
           [:fields :form-template-id :sort :form-id]
           prompt
           use-id?))

(defn select-form-templates [form-template-id & [fields]]
  (-> [[:form-templates {:id form-template-id}
        (or fields [:id])]]
      ha/sync-query
      :form-templates))

(defn form-template-exists? [{:keys [id] :as form-template}]
  (-> id
      select-form-templates
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

(defn determine-tree-existence
  [{:keys [exists?-fn select-fn use-id? ignore-id?] :as opts}
   {:keys [value existing exists? parent] :as tr}]
  (let [existing'  (or existing
                       (when (and (not ignore-id?)
                                  (-> exists? false? not)
                                  select-fn)
                         (select-fn value parent)))
        exists?' (cond (or existing' select-fn) (boolean existing')
                       (boolean? exists?) exists?
                       (and (not ignore-id?) exists?-fn) (exists?-fn value parent))]
    {:existing existing'
     :exists? exists?'}))

(defn apply-tree-top-value
  [{:keys [action insert-fn update-fn delete-fn use-id? ignore-id?] :as opts}
   {:keys [value children parent existing'] :as tr}]
  (when action
    (let [{existing' :existing exists?' :exists?} (determine-tree-existence opts tr)
          u-fn #(ha/walk-sql-field->clj-kw (update-fn value parent existing'))
          i-fn #(ha/walk-sql-field->clj-kw (insert-fn value parent existing'))
          d-fn #(delete-fn existing')]
      (case action
        :upsert (if exists?'
                  (or (u-fn) value)
                  (i-fn))
        :insert (when-not exists?'
                  (i-fn))
        :force-insert (i-fn)
        :update (or (u-fn) value)
        :replace (do (when existing'
                       (d-fn))
                     (i-fn))
        :delete (d-fn)))))

(declare apply-tree)

(defn apply-tree-children
  [cfg children parent-value parent-result]
  (mapv #(apply-tree cfg
                     (assoc %
                            :parent parent-result))
        children))

(defn diff-tree-children-with-existing
  [post-opts {:keys [children] parent-value :value} parent-result]
  (let [{:keys [get-existing-children-fn existing-children-group-fn given-children-group-fn]} post-opts
        existing-children (if get-existing-children-fn
                            (get-existing-children-fn parent-value parent-result)
                            [])
        existing-groups (->> existing-children
                             (group-by existing-children-group-fn)
                             (ut/fmap first))
        given-groups (->> children
                          (group-by (comp given-children-group-fn :value))
                          (ut/fmap first))
        all-keys (keys (merge existing-groups given-groups))]
    (reduce (fn [agg k]
              (let [existing (existing-groups k)
                    given (given-groups k)]
                (cond (nil? existing) (update agg :new conj (assoc given
                                                                   :exists? false))
                      (nil? given) (update agg :missing conj {:value existing
                                                              :mode :missing
                                                              :exists? true})
                      :else (update agg :common conj (assoc given
                                                            :existing existing
                                                            :exists? true)))))
            {:missing #{}
             :common #{}
             :new #{}}
            all-keys)))

(defn apply-tree
  [{:keys [pre] :as cfg} {:keys [value children mode] :as tr}]
  (let [{:keys [exists? existing] :as tr'} (merge tr
                                                 (determine-tree-existence pre tr))
        mode' (or mode
                  (if (or exists? existing)
                    :common
                    :new))
        {:keys [post] :as cfg'} (mode' cfg)
        result (apply-tree-top-value (merge pre post) tr)
        diff (diff-tree-children-with-existing post tr result)]
    ((post :finalize-fn identity)
     {:result result
      :missing (apply-tree-children cfg'
                                    (:missing diff)
                                    value
                                    result)
      :common (apply-tree-children cfg'
                                   (:common diff)
                                   value
                                   result)
      :new (apply-tree-children cfg'
                                (:new diff)
                                value
                                result)})))


(defn create-form-from-template
  [{:keys [form-template-id from-org-id from-user-id
           title descr status notes to-org-id
           to-user-id] :as m}]
  (->> {:value m}
       (apply-tree
        {:pre {:select-fn (fn [{:keys [form-template-id]} _]
                            (-> form-template-id
                                (select-form-templates [:id :ftype :fsubtype])
                                first))}
         :common {:post {:action :force-insert
                         :insert-fn (fn [value _ {:keys [ftype fsubtype]}]
                                      (-> value
                                          (assoc :ftype ftype
                                                 :fsubtype fsubtype)
                                          insert-form))
                         :get-existing-children-fn
                         (fn [{:keys [form-template-id]} _]
                           (select-form-template-prompts-by-parent-id form-template-id))
                         :existing-children-group-fn :prompt-id
                         :given-children-group-fn (constantly nil)}
                  :missing {:post {:action :force-insert
                                   :insert-fn (fn [{:keys [prompt-id] sort' :sort} {:keys [id]} _]
                                                (insert-form-prompt id prompt-id sort'))}}}})
       :result))


#_
(clojure.pprint/pprint 
 (apply-tree
  {:pre {:select-fn (fn [{:keys [form-template-id]} _]
                      (-> form-template-id
                          (select-form-templates [:id :ftype :fsubtype])
                          first))}
   :common {:post {:action :force-insert
                   :insert-fn (fn [value _ {:keys [ftype fsubtype]}]
                                (-> value
                                    (assoc :ftype ftype
                                           :fsubtype fsubtype)
                                    insert-form))
                   :get-existing-children-fn
                   (fn [{:keys [form-template-id]} _]
                     (select-form-template-prompts-by-parent-id form-template-id))
                   :existing-children-group-fn :prompt-id
                   :given-children-group-fn (constantly nil)}
            :missing {:post {:action :force-insert
                             :insert-fn (fn [{:keys [prompt-id] sort' :sort} {:keys [id]} _]
                                          (insert-form-prompt id prompt-id sort'))}}}} 
  {:value {:from-org-id 852106324668,
           :from-user-id 852106304667,
           :prod-id 272814695158,
           :form-template-id 370382503635,
           :title "Preposal Request 70722",
           :status "init",
           :subject 272814695158}}))

#_

(apply-4tree
 {:l0 {:get-children-fn (constantly (select-form-template-prompts-by-parent-id 370382503635))
       :insert-fn insert-form}
  :l1 {:existing-group-id-fn :prompt-id
       :new-group-fn (constantly {})
       :missing :create
       :insert-fn insert-form-prompt}
  :l2 {:ignore? true}
  :l3 {:ignore? true}} 
 {:value {:from-org-id 852106324668,
          :from-user-id 852106304667,
          :prod-id 272814695158,
          :form-template-id 370382503635,
          :title "Preposal Request 70722",
          :status "init",
          :subject 272814695158}})

{:value {:id 0}
 :children [{:value {:id 2}
             :children [{:value {:id 3}}]}]}


;; update-doc-from-form-doc

#_
{:doc-dtype "product-profile",
 :doc-title " doc",
 :doc-notes "",
 :doc-descr "",
 :prompts
 [{:id 740547877574,
   :idstr "jqhlddnc",
   :prompt "Competitive Differentiator",
   :descr
   "What makes this product different from its closest competitors",
   :sort 100,
   :fields
   [{:id 851290884048,
     :idstr "k5c2wcbm",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort nil,
     :response
     [{:id 879299906408,
       :pf-id 851290884048,
       :idx nil,
       :sval "anything??",
       :nval nil,
       :dval nil,
       :jval nil,
       :state "anything??"}]}],
   :response
   {:id 879299896406,
    :prompt-id 740547877574,
    :notes nil,
    :fields
    [{:id 879299906408,
      :pf-id 851290884048,
      :idx nil,
      :sval "anything??",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740458537534,
   :idstr "jqf36ifk",
   :prompt "Cancellation Process",
   :descr "How does cancelling work",
   :sort 100,
   :fields
   [{:id 851290884050,
     :idstr "k5c2wcbo",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort nil,
     :response
     [{:id 904513149677,
       :pf-id 851290884050,
       :idx nil,
       :sval "nopppeee  333 444",
       :nval nil,
       :dval nil,
       :jval nil,
       :state "nopppeee  555"}]}],
   :response
   {:id 904513149675,
    :prompt-id 740458537534,
    :notes nil,
    :fields
    [{:id 904513149677,
      :pf-id 851290884050,
      :idx nil,
      :sval "nopppeee  333 444",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 791676936590,
   :idstr "kdy6aj2o",
   :prompt "Product Website",
   :descr "Unique product website (URL)",
   :sort 100,
   :fields
   [{:id 851290884052,
     :idstr "k5c2wcbq",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :sort nil,
     :response
     [{:id 862227835259,
       :pf-id 851290884052,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227825257,
    :prompt-id 791676936590,
    :notes nil,
    :fields
    [{:id 862227835259,
      :pf-id 851290884052,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740517187557,
   :idstr "jqg23k2v",
   :prompt "Integrations",
   :descr "List of other products the vendor can integrate with",
   :sort 100,
   :fields
   [{:id 851290894054,
     :idstr "k5c2wj1k",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? nil,
     :sort nil,
     :response
     [{:id 879594746442,
       :pf-id 851290894054,
       :idx nil,
       :sval "dsfsdfsdfasdsdfsdfsdfsd11111111111111111",
       :nval nil,
       :dval nil,
       :jval nil,
       :state "dsfsdfsdfasdsdfsdfsdfsd11111111111111111"}]}],
   :response
   {:id 879594746440,
    :prompt-id 740517187557,
    :notes nil,
    :fields
    [{:id 879594746442,
      :pf-id 851290894054,
      :idx nil,
      :sval "dsfsdfsdfasdsdfsdfsdfsd11111111111111111",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740668010733,
   :idstr "jqjkv8zj",
   :prompt "Product Demo",
   :descr "Link to product demo",
   :sort 100,
   :fields
   [{:id 851290894056,
     :idstr "k5c2wj1m",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :sort nil,
     :response
     [{:id 862227835265,
       :pf-id 851290894056,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227835263,
    :prompt-id 740668010733,
    :notes nil,
    :fields
    [{:id 862227835265,
      :pf-id 851290894056,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740568977578,
   :idstr "jqhxxmik",
   :prompt "Tagline",
   :descr "Describe your product in fewer than 10 words",
   :sort 100,
   :fields
   [{:id 851290894058,
     :idstr "k5c2wj1o",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :sort nil,
     :response
     [{:id 862227835268,
       :pf-id 851290894058,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227835266,
    :prompt-id 740568977578,
    :notes nil,
    :fields
    [{:id 862227835268,
      :pf-id 851290894058,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740439647531,
   :idstr "jqfsxms9",
   :prompt "Minimum Contract Length",
   :descr "Is there a minimum contract length?",
   :sort 100,
   :fields
   [{:id 851290894060,
     :idstr "k5c2wj1q",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :sort nil,
     :response
     [{:id 862227835271,
       :pf-id 851290894060,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227835269,
    :prompt-id 740439647531,
    :notes nil,
    :fields
    [{:id 862227835271,
      :pf-id 851290894060,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740619217590,
   :idstr "jqirufyg",
   :prompt "Number of Current Clients",
   :descr "Number of current clients",
   :sort 100,
   :fields
   [{:id 851290894062,
     :idstr "k5c2wj1s",
     :fname "value",
     :ftype "n",
     :fsubtype "int",
     :list? false,
     :sort nil,
     :response
     [{:id 862227835274,
       :pf-id 851290894062,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227835272,
    :prompt-id 740619217590,
    :notes nil,
    :fields
    [{:id 862227835274,
      :pf-id 851290894062,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 791684236593,
   :idstr "kdzam0sj",
   :prompt "Product Logo",
   :descr "URL for the product logo",
   :sort 100,
   :fields
   [{:id 851290904064,
     :idstr "k5c2wrrm",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :sort nil,
     :response
     [{:id 862227845277,
       :pf-id 851290904064,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227835275,
    :prompt-id 791684236593,
    :notes nil,
    :fields
    [{:id 862227845277,
      :pf-id 851290904064,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740520667560,
   :idstr "jqg4559m",
   :prompt "Product Roadmap",
   :descr
   "Overview of the features on the vendor's roadmap for the next 6-12 months",
   :sort 100,
   :fields
   [{:id 851290904066,
     :idstr "k5c2wrro",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort nil,
     :response
     [{:id 862227845280,
       :pf-id 851290904066,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227845278,
    :prompt-id 740520667560,
    :notes nil,
    :fields
    [{:id 862227845280,
      :pf-id 851290904066,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740544647571,
   :idstr "jqhjf5c1",
   :prompt "Competitors",
   :descr "List of closest competitors",
   :sort 100,
   :fields
   [{:id 851290904068,
     :idstr "k5c2wrrq",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? nil,
     :sort nil,
     :response
     [{:id 862227845283,
       :pf-id 851290904068,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227845281,
    :prompt-id 740544647571,
    :notes nil,
    :fields
    [{:id 862227845283,
      :pf-id 851290904068,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740396517525,
   :idstr "jqe287hj",
   :prompt "Price Range",
   :descr "Descripe the different pricing tiers or options available",
   :sort 100,
   :fields
   [{:id 851290904070,
     :idstr "k5c2wrrs",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :sort nil,
     :response
     [{:id 862227855286,
       :pf-id 851290904070,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227845284,
    :prompt-id 740396517525,
    :notes nil,
    :fields
    [{:id 862227855286,
      :pf-id 851290904070,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740504037548,
   :idstr "jqgu9qgu",
   :prompt "Data Security",
   :descr "What the vendor does to ensure the buyer's data is secure",
   :sort 100,
   :fields
   [{:id 851290904072,
     :idstr "k5c2wrru",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort nil,
     :response
     [{:id 862227855289,
       :pf-id 851290904072,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227855287,
    :prompt-id 740504037548,
    :notes nil,
    :fields
    [{:id 862227855289,
      :pf-id 851290904072,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740681080738,
   :idstr "jqjsodu8",
   :prompt "Case Studies",
   :descr "Links to case studies",
   :sort 100,
   :fields
   [{:id 851290904074,
     :idstr "k5c2wrrw",
     :fname "Links to Case Studies",
     :ftype "s",
     :fsubtype "single",
     :list? nil,
     :sort nil,
     :response
     [{:id 862227855292,
       :pf-id 851290904074,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227855290,
    :prompt-id 740681080738,
    :notes nil,
    :fields
    [{:id 862227855292,
      :pf-id 851290904074,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740493817542,
   :idstr "jqgo6ons",
   :prompt "Reporting",
   :descr "What reporting is available to the client (if applicable)",
   :sort 100,
   :fields
   [{:id 851290914076,
     :idstr "k5c2wzhq",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort nil,
     :response
     [{:id 862227855295,
       :pf-id 851290914076,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227855293,
    :prompt-id 740493817542,
    :notes nil,
    :fields
    [{:id 862227855295,
      :pf-id 851290914076,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 558587751456,
   :idstr "hewa0drm",
   :prompt "Please describe the terms of your trial",
   :descr nil,
   :sort 100,
   :fields
   [{:id 558588011461,
     :idstr "hewa5ydz",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort 0,
     :response
     [{:id 862227855298,
       :pf-id 558588011461,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227855296,
    :prompt-id 558587751456,
    :notes nil,
    :fields
    [{:id 862227855298,
      :pf-id 558588011461,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740599617585,
   :idstr "jqif6chv",
   :prompt "Onboarding Process",
   :descr "Steps involved with onboarding or implementing product",
   :sort 100,
   :fields
   [{:id 851290924079,
     :idstr "k5c2w67l",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort nil,
     :response
     [{:id 862227855301,
       :pf-id 851290924079,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}
    {:id 851290924080,
     :idstr "k5c2w67m",
     :fname "Estimated Time To Onboard",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :sort nil,
     :response
     [{:id 862227855302,
       :pf-id 851290924080,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227855299,
    :prompt-id 740599617585,
    :notes nil,
    :fields
    [{:id 862227855301,
      :pf-id 851290924079,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}
     {:id 862227855302,
      :pf-id 851290924080,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740532527564,
   :idstr "jqhb8dh6",
   :prompt "Ideal Client Profile",
   :descr
   "Description of the ideal profile (# of employees, revenue, vertical, etc.) for this product",
   :sort 100,
   :fields
   [{:id 851290924082,
     :idstr "k5c2w67o",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort nil,
     :response
     [{:id 862227865305,
       :pf-id 851290924082,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227855303,
    :prompt-id 740532527564,
    :notes nil,
    :fields
    [{:id 862227865305,
      :pf-id 851290924082,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740599447583,
   :idstr "jqif2pbl",
   :prompt "Onboarding Team Involvement",
   :descr
   "What roles are involved in onboarding/implementation of this product, and what level of effort is needed from each of them?",
   :sort 100,
   :fields
   [{:id 851290924084,
     :idstr "k5c2w67q",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort nil,
     :response
     [{:id 862227865308,
       :pf-id 851290924084,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227865306,
    :prompt-id 740599447583,
    :notes nil,
    :fields
    [{:id 862227865308,
      :pf-id 851290924084,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740514557554,
   :idstr "jqg1i7q8",
   :prompt "Meeting Frequency",
   :descr "How often meetings will be held",
   :sort 100,
   :fields
   [{:id 851290934086,
     :idstr "k5c2xexk",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort nil,
     :response
     [{:id 862227865311,
       :pf-id 851290934086,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227865309,
    :prompt-id 740514557554,
    :notes nil,
    :fields
    [{:id 862227865311,
      :pf-id 851290934086,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 558587581453,
   :idstr "hewawqlb",
   :prompt "Describe your product or service",
   :descr nil,
   :sort 100,
   :fields
   [{:id 558587851458,
     :idstr "hewa2ixg",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort 0,
     :response
     [{:id 870328816349,
       :pf-id 558587851458,
       :idx nil,
       :sval "This is a description of the MailChimp promduct",
       :nval nil,
       :dval nil,
       :jval nil,
       :state "This is a description of the MailChimp promduct"}]}],
   :response
   {:id 870328816347,
    :prompt-id 558587581453,
    :notes nil,
    :fields
    [{:id 870328816349,
      :pf-id 558587851458,
      :idx nil,
      :sval "This is a description of the MailChimp promduct",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740499107545,
   :idstr "jqgsb2gb",
   :prompt "KPIs",
   :descr "KPIs that the vendor focuses on improving",
   :sort 100,
   :fields
   [{:id 851290944089,
     :idstr "k5c2xmnf",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? nil,
     :sort nil,
     :response
     [{:id 862227875317,
       :pf-id 851290944089,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227865315,
    :prompt-id 740499107545,
    :notes nil,
    :fields
    [{:id 862227875317,
      :pf-id 851290944089,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740404407528,
   :idstr "jqe7ybga",
   :prompt "Payment Options",
   :descr
   "What options does a user have to pay (Credit Card, Invoice, etc.)",
   :sort 100,
   :fields
   [{:id 851290944091,
     :idstr "k5c2xmnh",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? nil,
     :sort nil,
     :response
     [{:id 862227875320,
       :pf-id 851290944091,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227875318,
    :prompt-id 740404407528,
    :notes nil,
    :fields
    [{:id 862227875320,
      :pf-id 851290944091,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740621457593,
   :idstr "jqis6gcr",
   :prompt "Example Current Clients",
   :descr "Example current clients",
   :sort 100,
   :fields
   [{:id 851290944093,
     :idstr "k5c2xmnj",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? nil,
     :sort nil,
     :response
     [{:id 862227875323,
       :pf-id 851290944093,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227875321,
    :prompt-id 740621457593,
    :notes nil,
    :fields
    [{:id 862227875323,
      :pf-id 851290944093,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 740508717551,
   :idstr "jqgx11kx",
   :prompt "Point of Contact",
   :descr
   "Person the buyer will interact with the buyer most regularly",
   :sort 100,
   :fields
   [{:id 851290944095,
     :idstr "k5c2xmnl",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :sort nil,
     :response
     [{:id 862227875326,
       :pf-id 851290944095,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227875324,
    :prompt-id 740508717551,
    :notes nil,
    :fields
    [{:id 862227875326,
      :pf-id 851290944095,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 567926257448,
   :idstr "hi6qwy3u",
   :prompt "Categories",
   :descr "examples: zoo, forestry, cemetary",
   :sort 100,
   :fields
   [{:id 567926257451,
     :idstr "hi6qwy3x",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? nil,
     :sort 0,
     :response
     [{:id 862227875329,
       :pf-id 567926257451,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227875327,
    :prompt-id 567926257448,
    :notes nil,
    :fields
    [{:id 862227875329,
      :pf-id 567926257451,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 558587701455,
   :idstr "hewaza6p",
   :prompt "Do you offer a free trial?",
   :descr "Free as in beer",
   :sort 100,
   :fields
   [{:id 558587961460,
     :idstr "hewa4vs2",
     :fname "value",
     :ftype "e",
     :fsubtype "e-yes-no",
     :list? false,
     :sort 0,
     :response
     [{:id 862227885332,
       :pf-id 558587961460,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227875330,
    :prompt-id 558587701455,
    :notes nil,
    :fields
    [{:id 862227885332,
      :pf-id 558587961460,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}
  {:id 567926257446,
   :idstr "hi6qwy3s",
   :prompt "Pricing Model",
   :descr "Describe the method used to calculate price",
   :sort 100,
   :fields
   [{:id 567926257449,
     :idstr "hi6qwy3v",
     :fname "value",
     :ftype "s",
     :fsubtype "multi",
     :list? false,
     :sort 0,
     :response
     [{:id 862227885335,
       :pf-id 567926257449,
       :idx nil,
       :sval "",
       :nval nil,
       :dval nil,
       :jval nil,
       :state ""}]}],
   :response
   {:id 862227885333,
    :prompt-id 567926257446,
    :notes nil,
    :fields
    [{:id 862227885335,
      :pf-id 567926257449,
      :idx nil,
      :sval "",
      :nval nil,
      :dval nil,
      :jval nil}],
    :notes-state ""}}],
 :title nil,
 :product {:id 272814725271},
 :id 851292794100,
 :responses
 [{:id 862227825257,
   :prompt-id 791676936590,
   :notes nil,
   :fields
   [{:id 862227835259,
     :pf-id 851290884052,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227835263,
   :prompt-id 740668010733,
   :notes nil,
   :fields
   [{:id 862227835265,
     :pf-id 851290894056,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227835266,
   :prompt-id 740568977578,
   :notes nil,
   :fields
   [{:id 862227835268,
     :pf-id 851290894058,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227835269,
   :prompt-id 740439647531,
   :notes nil,
   :fields
   [{:id 862227835271,
     :pf-id 851290894060,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227835272,
   :prompt-id 740619217590,
   :notes nil,
   :fields
   [{:id 862227835274,
     :pf-id 851290894062,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227835275,
   :prompt-id 791684236593,
   :notes nil,
   :fields
   [{:id 862227845277,
     :pf-id 851290904064,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227845278,
   :prompt-id 740520667560,
   :notes nil,
   :fields
   [{:id 862227845280,
     :pf-id 851290904066,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227845281,
   :prompt-id 740544647571,
   :notes nil,
   :fields
   [{:id 862227845283,
     :pf-id 851290904068,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227845284,
   :prompt-id 740396517525,
   :notes nil,
   :fields
   [{:id 862227855286,
     :pf-id 851290904070,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227855287,
   :prompt-id 740504037548,
   :notes nil,
   :fields
   [{:id 862227855289,
     :pf-id 851290904072,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227855290,
   :prompt-id 740681080738,
   :notes nil,
   :fields
   [{:id 862227855292,
     :pf-id 851290904074,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227855293,
   :prompt-id 740493817542,
   :notes nil,
   :fields
   [{:id 862227855295,
     :pf-id 851290914076,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227855296,
   :prompt-id 558587751456,
   :notes nil,
   :fields
   [{:id 862227855298,
     :pf-id 558588011461,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227855299,
   :prompt-id 740599617585,
   :notes nil,
   :fields
   [{:id 862227855301,
     :pf-id 851290924079,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}
    {:id 862227855302,
     :pf-id 851290924080,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227855303,
   :prompt-id 740532527564,
   :notes nil,
   :fields
   [{:id 862227865305,
     :pf-id 851290924082,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227865306,
   :prompt-id 740599447583,
   :notes nil,
   :fields
   [{:id 862227865308,
     :pf-id 851290924084,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227865309,
   :prompt-id 740514557554,
   :notes nil,
   :fields
   [{:id 862227865311,
     :pf-id 851290934086,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227865315,
   :prompt-id 740499107545,
   :notes nil,
   :fields
   [{:id 862227875317,
     :pf-id 851290944089,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227875318,
   :prompt-id 740404407528,
   :notes nil,
   :fields
   [{:id 862227875320,
     :pf-id 851290944091,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227875321,
   :prompt-id 740621457593,
   :notes nil,
   :fields
   [{:id 862227875323,
     :pf-id 851290944093,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227875324,
   :prompt-id 740508717551,
   :notes nil,
   :fields
   [{:id 862227875326,
     :pf-id 851290944095,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227875327,
   :prompt-id 567926257448,
   :notes nil,
   :fields
   [{:id 862227875329,
     :pf-id 567926257451,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227875330,
   :prompt-id 558587701455,
   :notes nil,
   :fields
   [{:id 862227885332,
     :pf-id 558587961460,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 862227885333,
   :prompt-id 567926257446,
   :notes nil,
   :fields
   [{:id 862227885335,
     :pf-id 567926257449,
     :idx nil,
     :sval "",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 870328816347,
   :prompt-id 558587581453,
   :notes nil,
   :fields
   [{:id 870328816349,
     :pf-id 558587851458,
     :idx nil,
     :sval "This is a description of the MailChimp promduct",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 879299896406,
   :prompt-id 740547877574,
   :notes nil,
   :fields
   [{:id 879299906408,
     :pf-id 851290884048,
     :idx nil,
     :sval "anything??",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 879594746440,
   :prompt-id 740517187557,
   :notes nil,
   :fields
   [{:id 879594746442,
     :pf-id 851290894054,
     :idx nil,
     :sval "dsfsdfsdfasdsdfsdfsdfsd11111111111111111",
     :nval nil,
     :dval nil,
     :jval nil}]}
  {:id 904513149675,
   :prompt-id 740458537534,
   :notes nil,
   :fields
   [{:id 904513149677,
     :pf-id 851290884050,
     :idx nil,
     :sval "nopppeee  333 444",
     :nval nil,
     :dval nil,
     :jval nil}]}],
 :doc-id 862227825250,
 :ftype "product-profile",
 :doc-product {:id 272814725271},
 :doc-dsubtype "product-profile1",
 :fsubtype "product-profile1"}



;; create-doc-from-form-doc

#_
{:doc-dtype "vendor-profile",
 :doc-title " doc",
 :to-org {:id 920694689711},
 :doc-notes "",
 :doc-descr "",
 :prompts
 [{:id 558587521452,
   :idstr "hewavgam",
   :prompt "Employee Count",
   :descr "About how many people does your organzation employ?",
   :fields
   [{:id 558587801457,
     :idstr "hewa1gcj",
     :fname "value",
     :ftype "n",
     :fsubtype "int",
     :list? false,
     :response [{:state "2"}]}],
   :response {:notes-state ""}}
  {:id 558587631454,
   :idstr "hewaxs58",
   :prompt "Website",
   :descr
   "At which universal resource locator (URL) may your marketing website be found?",
   :fields
   [{:id 558587901459,
     :idstr "hewa3lid",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :response [{:state "v.com"}]}],
   :response {:notes-state ""}}
  {:id 567926257447,
   :idstr "hi6qwy3t",
   :prompt "Logo URL",
   :descr "Where that logo at? Interwebs?",
   :fields
   [{:id 567926257450,
     :idstr "hi6qwy3w",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :response [{:state "vlogo"}]}],
   :response {:notes-state ""}}
  {:id 739253242143,
   :idstr "jpv6kuj1",
   :prompt "Year Founded",
   :descr "The year the vendor was founded",
   :fields
   [{:id 851290874042,
     :idstr "k5c2v4lo",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :response [{:state "1900"}]}],
   :response {:notes-state ""}}
  {:id 739612789895,
   :idstr "jp14m7dl",
   :prompt "Funding Status",
   :descr
   "What funding stage the vendor is at/if it has reached profitability ",
   :fields
   [{:id 851290874044,
     :idstr "k5c2v4lq",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :response [{:state "early"}]}],
   :response {:notes-state ""}}
  {:id 739620209898,
   :idstr "jp1818os",
   :prompt "Headquarters Location",
   :descr "Location of the vendor's headquarters",
   :fields
   [{:id 851290874046,
     :idstr "k5c2v4ls",
     :fname "value",
     :ftype "s",
     :fsubtype "single",
     :list? false,
     :response [{:state "space"}]}],
   :response {:notes-state ""}}],
 :title nil,
 :id 851293464129,
 :ftype "vendor-profile",
 :doc-dsubtype "vendor-profile1",
 :fsubtype "vendor-profile1"}


;; create-preposal-req-form

#_
{:from-org-id 852106324668,
 :from-user-id 852106304667,
 :prod-id 272814695158}


;; create-preposal-req-form => create-form-from-template

#_
{:from-org-id 852106324668,
 :from-user-id 852106304667,
 :prod-id 272814695158,
 :form-template-id 370382503635,
 :title "Preposal Request 70722",
 :status "init",
 :subject 272814695158}


#_
{:value {:from-org-id 852106324668,
         :from-user-id 852106304667,
         :prod-id 272814695158,
         :form-template-id 370382503635,
         :title "Preposal Request 70722",
         :status "init",
         :subject 272814695158}
 :children [:...form_prompts...]}

;; upsert-form-template

#_
{:deleted nil,
 :updated "2019-03-26T15:00:00+00:00",
 :created "2019-03-26T15:00:00+00:00",
 :idstr "jmyy0nls",
 :title "Vendor Profile",
 :id 732891594222,
 :descr "Vendor Profile",
 :ftype "vendor-profile",
 :fsubtype "vendor-profile1"}


;; upsert-form-template-prompts

#_
[{:deleted nil,
  :updated "2019-03-06T10:40:00+00:00",
  :fields
  [{:prompt-id 558587521452,
    :deleted nil,
    :fname "value",
    :updated "2019-03-06T10:40:00+00:00",
    :created "2019-03-06T10:40:00+00:00",
    :idstr "hewa1gcj",
    :id 558587801457,
    :list? false,
    :descr nil,
    :ftype "n",
    :sort 0,
    :fsubtype "int"}],
  :created "2019-03-06T10:40:00+00:00",
  :idstr "hewavgam",
  :prompt "Employee Count",
  :id 558587521452,
  :form-template-id 732891594222,
  :descr "About how many people does your organzation employ?",
  :sort 100}
 {:deleted nil,
  :updated "2019-03-06T10:40:00+00:00",
  :fields
  [{:prompt-id 558587631454,
    :deleted nil,
    :fname "value",
    :updated "2019-03-06T10:40:00+00:00",
    :created "2019-03-06T10:40:00+00:00",
    :idstr "hewa3lid",
    :id 558587901459,
    :list? false,
    :descr nil,
    :ftype "s",
    :sort 0,
    :fsubtype "single"}],
  :created "2019-03-06T10:40:00+00:00",
  :idstr "hewaxs58",
  :prompt "Website",
  :id 558587631454,
  :form-template-id 732891594222,
  :descr
  "At which universal resource locator (URL) may your marketing website be found?",
  :sort 100}
 {:deleted nil,
  :updated "2019-03-07T10:40:00+00:00",
  :fields
  [{:prompt-id 567926257447,
    :deleted nil,
    :fname "value",
    :updated "2019-03-07T10:40:00+00:00",
    :created "2019-03-07T10:40:00+00:00",
    :idstr "hi6qwy3w",
    :id 567926257450,
    :list? false,
    :descr nil,
    :ftype "s",
    :sort 0,
    :fsubtype "single"}],
  :created "2019-03-07T10:40:00+00:00",
  :idstr "hi6qwy3t",
  :prompt "Logo URL",
  :id 567926257447,
  :form-template-id 732891594222,
  :descr "Where that logo at? Interwebs?",
  :sort 100}
 {:deleted nil,
  :updated "2019-03-27T14:28:43.033+00:00",
  :fields
  [{:prompt-id 739253242143,
    :deleted nil,
    :fname "value",
    :updated "2019-03-28T13:57:57.679+00:00",
    :created "2019-03-27T13:29:55.15+00:00",
    :idstr "jpwaa8ir",
    :id 739259512145,
    :list? false,
    :descr "",
    :ftype "s",
    :sort nil,
    :fsubtype "single"}],
  :created "2019-03-27T13:28:52.474+00:00",
  :idstr "jpv6kuj1",
  :prompt "Year Founded",
  :id 739253242143,
  :form-template-id 732891594222,
  :descr "The year the vendor was founded",
  :sort 100}
 {:deleted nil,
  :updated "2019-03-27T14:29:58.093+00:00",
  :fields
  [{:prompt-id 739612789895,
    :deleted nil,
    :fname "value",
    :updated "2019-03-27T14:29:58.246+00:00",
    :created "2019-03-27T14:29:32.144+00:00",
    :idstr "jp1695k7",
    :id 739617219897,
    :list? false,
    :descr "",
    :ftype "s",
    :sort nil,
    :fsubtype "single"}],
  :created "2019-03-27T14:28:47.811+00:00",
  :idstr "jp14m7dl",
  :prompt "Funding Status",
  :id 739612789895,
  :form-template-id 732891594222,
  :descr
  "What funding stage the vendor is at/if it has reached profitability ",
  :sort 100}
 {:deleted nil,
  :updated "2019-03-27T14:30:24.018+00:00",
  :fields
  [{:prompt-id 739620209898,
    :deleted nil,
    :fname "value",
    :updated "2019-03-27T14:30:24.156+00:00",
    :created "2019-03-27T14:30:15.336+00:00",
    :idstr "jp19uqxa",
    :id 739621539900,
    :list? false,
    :descr "",
    :ftype "s",
    :sort nil,
    :fsubtype "single"}],
  :created "2019-03-27T14:30:02.036+00:00",
  :idstr "jp1818os",
  :prompt "Headquarters Location",
  :id 739620209898,
  :form-template-id 732891594222,
  :descr "Location of the vendor's headquarters",
  :sort 100}]


#_

{:data {:terms {:product/goal {:value "We need everything."}
                :product/budget {:value 2400
                                 :period "Annual"}
                :product/someterm {:value "Hello sir.\nWhat's up?"}}
        :prompt-ids {126786722 {:value "Justanother Value"}}}
 :ftype "ftype"
 :update-doc-id 123
 :round-id 456
 :from-org-id 567}

#_{:value {:ftype "ftype"
           :id 123
           :round-id 456
           :from-org-id 567}
   :children [{:value {}
               :children [{:value {:prompt-id 111}
                           :children [{:value {:fname "value"
                                               :ftype "xxx"
                                               :sval "We need everything."
                                               :pf-id 2323}}]}]}
              {:value {}
               :children [{:value {:prompt-id 222}
                           :children [{:value {:fname "value"
                                               :ftype "n"
                                               :nval 2400
                                               :pf-id 1212}}
                                      {:value {:fname "period"
                                               :ftype "e-period"
                                               :sval "Annual"
                                               :pf-id 3434}}]}]}]}

;; TODO support new doc that includes existring responses

#_
{:pre {}
 :new {:post {:action :insert
              :insert-fn #(:insert-doc)}
       :new {:post {:action nil
                    :finalize-fn #(:insert-doc-resp)
                    :given-children-group-fn :prompt-id}
             :pre {:select-fn #(:select-response-by........)}
             :new {:post {:action :insert
                          :insert-fn #(:insert-response)
                          :existing-children-group-fn :prompt-id
                          :given-children-group-fn :prompt-id}
                   :new {:post {:action :insert
                                :insert-fn #(:insert-response-field)
                                :existing-children-group-fn :pf-id
                                :given-children-group-fn :prompt-id}}}}}}
