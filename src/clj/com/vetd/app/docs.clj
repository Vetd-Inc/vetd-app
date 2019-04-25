(ns com.vetd.app.docs
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]
            clojure.data
            clojure.set))

#_ (def handle-doc-creation nil)
(defmulti handle-doc-creation (fn [{:keys [dtype]} handler-args] (keyword dtype)))

(defmethod handle-doc-creation :default [_ _])

(def ^:dynamic *docs-created* nil)

(defmacro with-doc-handling
  [handler-args & body]
  `(let [dc&# (atom #{})
         r# (binding [*docs-created* dc&#]
              (do ~@body))
         dc# @dc&#]
     (future (doseq [d# dc#]
               (handle-doc-creation d# ~handler-args)))
     r#))

(defmacro tree-assoc-fn
  [key-bindings & body]
  `(fn [x#]
     (let [{:keys ~key-bindings :as x#} (assoc x#
                                               :parent
                                               (-> x# :parents last))
           r# (do ~@body)]
       (apply assoc x# r#))))

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
    (when *docs-created*
      (swap! *docs-created* conj d))
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

;; TODO add sort field??
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

(defn infer-field-type-kw
  [{:keys [sval nval dval jval]}]
  (cond
    sval :sval
    nval :nval
    dval :dval
    jval :jval))

(defn expand-response-fields
  [{:keys [sval nval dval jval] :as response-fields}]
  (let [vals (or sval nval dval jval)
        val-kw (infer-field-type-kw response-fields)]
    (if (sequential? vals)
      (map-indexed (fn [idx v]
                     (assoc response-fields
                            :idx idx
                            val-kw v))
                   vals)
      [response-fields])))

(defn insert-response-fields
  [resp-id response-fields]
  (doseq [rf (expand-response-fields response-fields)]
    (insert-response-field resp-id rf)))

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
      (insert-response-fields resp-id
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
              [:prompts {:ref-deleted nil}
               [:id :rpid :sort]]]]]
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
    (doseq [{:keys [fields] prompt-id :id sort' :sort :as prompt} new-prompts]
      (mapv #(upsert-prompt-field % true) fields)
      (upsert-prompt prompt use-id?))    
    (doseq [{:keys [fields] prompt-id :id sort' :sort :as prompt} new-form-prompts]
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

(defn doc->appliable--find-form
  [{:keys [dtype dsubtype update-doc-id] :as d}]
  (when (or dtype dsubtype update-doc-id)
    (let [form-fields [:id :ftype :fsubtype
                       [:prompts
                        [:id :term
                         [:fields
                          [:id :fname :ftype :fsubtype]]]]]
          form-args (merge {:_order_by {:created :desc}
                            :_limit 1
                            :deleted nil}
                           (when dtype
                             {:ftype dtype})
                           (when dsubtype
                             {:fsubtype dsubtype}))]
      (if update-doc-id
        (-> [[:docs {:id update-doc-id}
              [[:form form-fields]]]]
            ha/sync-query
            :docs
            first
            :form)
        (-> [[:forms form-args
              form-fields]]
            ha/sync-query
            :forms
            first)))))

(defn fields->appliable-tree
  [resp-fields fields]
  (let [fields-by-fname (group-by :fname fields)]
    (vec (for [[k value] resp-fields]
           (let [{:keys [fname ftype fsubtype id]} (-> k name fields-by-fname first)]
             {:item (merge {:fname fname
                             :ftype ftype
                             :fsubtype fsubtype
                             :prompt-field-id id}
                            (convert-field-val value
                                               ftype
                                               fsubtype))})))))

(defn response-prompts->appliable-tree
  [{:keys [terms prompt-ids]} prompts]
  (let [responses (merge terms prompt-ids)
        grouped-prompts (-> (merge (group-by (comp keyword :term) prompts)
                                   (group-by :id prompts))
                            (dissoc nil))]
    (vec (for [[k r] responses]
           (let [{prompt-id :id :keys [fields]} (-> k grouped-prompts first)]
             (if prompt-id
               {:item {}
                :children [{:item {:prompt-id prompt-id}
                             :children (fields->appliable-tree r fields)}]}
               (throw (Exception. (str "Could not find prompt with term or id: " k)))))))))

;; TODO set responses.subject
(defn doc->appliable-tree
  [{:keys [data dtype dsubtype update-doc-id] :as d}]
  (if-let [{:keys [id ftype fsubtype prompts]} (doc->appliable--find-form d)]
    {:handler-args d
     :item (merge (select-keys d ;; TODO hard-coded fields sucks -- Bill
                                [:title :dtype :descr :notes :from-org-id
                                 :from-user-id :to-org-id :to-user-id
                                 :dtype :dsubtype :form-id :subject])
                  {:form-id id
                   :dtype ftype
                    :dsubtype fsubtype}
                   (when update-doc-id
                     {:id update-doc-id}))
     :children (response-prompts->appliable-tree data
                                                 prompts)}
    (throw (Exception. (str "Couldn't find form by querying with: "
                            {:ftype dtype
                             :fsubtype dsubtype
                             :doc-id update-doc-id})))))


(declare apply-tree)

(defn apply-tree-child-reducer
  [m v' agg [k vs]]
  (if-let [mk (and m (m k))]
    (assoc agg
           k
           (mapv #(dissoc (apply-tree mk
                                    (merge v' %))
                          :parents)
                 vs))
    agg))

(defn apply-tree-map
  [m {:keys [item children parents] :as v}]
  (let [parents' (conj (or parents [])
                       item)
        v' (-> v
               (dissoc :item
                       :children)
               (assoc :parents parents'))]
    (assoc v
           :children
           (reduce (partial apply-tree-child-reducer
                            m
                            v')
                   {}
                   (if (map? children)
                     children
                     {(ffirst m) children})))))

(defn apply-tree
  [[& ops] {:keys [handler-args] :as v}]
  (with-doc-handling handler-args
    (loop [[head & tail] (flatten ops)
           v' v]
      (if head
        (recur (flatten tail)
               (if (map? head)
                 (apply-tree-map head v')
                 (head v')))
        v'))))

(defn create-doc [d]
  (->> d
       doc->appliable-tree
       (apply-tree
        [(tree-assoc-fn [item]
                        [:item (insert-doc item)])
         {:doc-responses [{:response
                           [(tree-assoc-fn [item]
                                           [:item (insert-response item)])
                            {:fields
                             [(tree-assoc-fn [parent item]
                                             [:item (insert-response-fields (:id parent)
                                                                           item)])]}]}
                          (tree-assoc-fn [item children parent]
                                         [:item
                                          (->> children
                                               :response
                                               first
                                               :item
                                               :id
                                               (insert-doc-response (:id parent)))])]}])))


(defn group-doc-responses--existing
  [{:keys [prompt-id fields]}]
  [prompt-id
   (->> fields
        (mapv (fn [{:keys [sval nval dval jval prompt-field]}]
                [(:fname prompt-field)
                 (or sval nval dval jval)]))
        (sort-by first))])

(defn group-doc-responses--given
  [{:keys [children]}]
  (let [[{item :item kids :children :as kid1}] children
        prompt-id (-> kid1 :item :prompt-id)]
    [prompt-id
     (->> kids
          (mapv (fn [{:keys [item]}]
                  (let [{:keys [sval nval dval jval fname]} item]
                    [fname
                     (or sval nval dval jval)])))
          (sort-by first))]))

(defn group-match-reducer
  [grouped-a grouped-b agg k]
  (let [a (grouped-a k)
        b (grouped-b k)]
    (cond (nil? a) (update agg :b conj b)
          (nil? b) (update agg :a conj a)
          :else (update agg :common conj [a b]))))

(defn group-match
  [a group-by-a-fn b group-by-b-fn]
  (let [grouped-a (->> a
                       (group-by group-by-a-fn)
                       (ut/fmap first))
        grouped-b (->> b
                       (group-by group-by-b-fn)
                       (ut/fmap first))
        all-keys (-> (merge grouped-a grouped-b)
                     keys)]
    (reduce (partial group-match-reducer
                     grouped-a
                     grouped-b)
            {:a #{}
             :b #{}
             :both #{}}
            all-keys)))

(defn group-doc-responses
  [existing-responses given-doc-resps]
  (-> (group-match existing-responses
                   group-doc-responses--existing
                   given-doc-resps
                   group-doc-responses--given)
      (clojure.set/rename-keys
       {:a :delete
        :b :new-responses
        :both :no-change})
      (update :delete
              (partial mapv (fn [{:keys [fields] :as d}]
                              {:item (dissoc d :fields)
                               :children fields})))))

;; TODO support reusing existing responses
(defn update-doc [d]
  (->> d
       doc->appliable-tree
       (apply-tree
        [(tree-assoc-fn [item children]
                        (-> item
                            ha/walk-clj-kw->sql-field
                            (select-keys [:doc_id :user_id :id :user_id :resp_id])
                            (db/update-any! :docs))
                        [:children (-> item
                                       :id
                                       get-child-responses
                                       (group-doc-responses children))])
         {:delete [(tree-assoc-fn [item]
                                  (->> item :ref-id (update-deleted :doc_resp))
                                  [])]
          :new-responses [{:response [(tree-assoc-fn [item]
                                                     [:item (insert-response item)])
                                      {:fields [(tree-assoc-fn [item parent]
                                                               [:item (insert-response-fields
                                                                       (:id parent)
                                                                       item)])]}]}
                          (tree-assoc-fn [item children parent]
                                         [:item
                                          (->> children
                                               :response
                                               first
                                               :item
                                               :id
                                               (insert-doc-response (:id parent)))])]}])))
