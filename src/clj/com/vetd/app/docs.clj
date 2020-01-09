(ns com.vetd.app.docs
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.journal :as journal]
            [com.vetd.app.db :as db]
            [com.vetd.app.proc-tree :as ptree :refer [tree-assoc-fn]]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]
            [mikera.image.core :as mimg]
            [image-resizer.resize :as rzimg]
            [clojure.string :as s]
            clojure.data
            [clojure.data.codec.base64 :as b64-codec]
            clojure.set))

#_ (def handle-doc-creation nil)
(defmulti handle-doc-creation
  "Gets called just before exiting a `with-doc-handling` form. Called
  once per CREATED form. Allows for custom logic by doc type."
  (fn [{:keys [dtype]} handler-args] (keyword dtype)))

(defmethod handle-doc-creation :default [_ _])
(def ^:dynamic *docs-created* nil)

#_ (def handle-doc-update nil)
(defmulti handle-doc-update
  "Gets called just before exiting a `with-doc-handling` form. Called
  once per UPDATED form. Allows for custom logic by doc type."
  (fn [{:keys [dtype]} & [handler-args]] (keyword dtype)))

(defmethod handle-doc-update :default [_ _])
(def ^:dynamic *docs-updated* nil)

(defmacro with-doc-handling
  "This keeps track of any document creation/updating that occurs within
  the form. Before exiting, calls are made to `handle-doc-creation`
  and `handle-doc-update` for each doc that has been created or
  updated, respectively."
  [handler-args & body]
  `(let [dc&# (atom #{})
         du&# (atom #{})
         r# (binding [*docs-created* dc&#
                      *docs-updated* du&#]
              (do ~@body))
         dc# @dc&#
         du# @du&#]
     (do
       ;; future
       (doseq [d# dc#]
         (try
           (journal/push-entry (assoc d#
                                      :jtype (->> d#
                                                  :dtype
                                                  name
                                                  (str "doc-created-")
                                                  keyword)))
           (catch Throwable e#))
         (handle-doc-creation d# ~handler-args))
       (doseq [d# du#]
         (handle-doc-update d# ~handler-args)))
     r#))

(defn proc-tree
  "This is a convenience so we always remember to perform
  `ptree/proc-tree` within a `with-doc-handling`."
  [ops {:keys [handler-args] :as v}]
  (with-doc-handling handler-args
    (ptree/proc-tree ops v)))

(defn convert-field-val
  "Takes a value `v` and a field type `ftype` and returns a map of
  values for writing to db."
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
    "i" {:sval nil
         :nval nil
         :dval nil
         :jval v}
    "u" {:sval (if-let [data-url-parts (re-seq #"(data:image/.*;base64,)(.*)" v)]
                 (let [baos (java.io.ByteArrayOutputStream.)
                       _ (-> data-url-parts
                             first
                             (nth 2)
                             .getBytes
                             b64-codec/decode
                             (java.io.ByteArrayInputStream.)
                             mimg/load-image
                             ((rzimg/resize-fn 150 150 image-resizer.scale-methods/automatic))
                             (mimg/write baos "png"))
                       ba (.toByteArray baos)
                       new-file-name (format "%s.png"
                                             (com/md5-hex ba))]
                   (com/s3-put "vetd-logos" new-file-name ba)
                   (log/info (format "Product logo processed: '%s'" new-file-name))
                   new-file-name)
                 v)
         :nval nil
         :dval nil
         :jval nil}
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

(defn propagate-prompt
  [form-prompt-ref-id target-form-id]
  (let [{prompt-id :prompt_id sort' :sort} (-> {:select [:sort :prompt_id]
                                                :from [:form_prompt]
                                                :where [:= :id form-prompt-ref-id]}
                                               db/hs-query
                                               first)]
    (insert-form-prompt target-form-id
                        prompt-id
                        sort')))

(defn get-max-prompt-sort-by-form-template-id
  [form-template-id]
  (some->> [[:form-templates {:id form-template-id}
             [[:prompts [:sort]]]]]
           ha/sync-query
           :form-templates
           first
           :prompts
           (map :sort)
           not-empty
           (apply max)))

(defn insert-form-template-prompt
  [form-template-id prompt-id & [sort']]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :form_template_prompt
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :form_template_id form-template-id
                     :prompt_id prompt-id
                     :sort (or sort'
                               (some-> form-template-id
                                       get-max-prompt-sort-by-form-template-id
                                       inc)
                               0)})
        first)))

(defn insert-response
  [{:keys [org-id prompt-id notes user-id subject subject-type]}]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :responses
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :deleted nil
                     :prompt_id prompt-id
                     :notes notes
                     :user_id user-id
                     :subject subject
                     :subject_type subject-type})
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

;; TODO make less gross
(defn insert-response-field*
  [resp-id
   {:keys [prompt-field-id sval nval dval jval ftype fsubtype]}
   idx
   {:keys [state]}]
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
                (when idx
                  {:idx idx})
                ;; TODO support multiple response fields (for where list? = true)
                (when state
                  (convert-field-val state ftype fsubtype)))
         (db/insert! :resp_fields)
         first)))

(defn insert-response-field
  [resp-id {:keys [response] :as resp-field}]
  (->> response
       (map-indexed (fn [idx rf-val]
                      (insert-response-field* resp-id
                                              resp-field
                                              idx
                                              rf-val)))
       doall))

(defn infer-field-type-kw
  "Field type guessing priority"
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

;; TODO make less gross
(defn insert-response-fields
  [resp-id response-fields]
  (if (:response response-fields)
    [(insert-response-field resp-id response-fields)]
    (vec
     (for [rf (expand-response-fields response-fields)]
       (insert-response-field* resp-id rf nil nil)))))

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
                      :fname "value"
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
  "Inserts a response, and its fields, and associates it with a doc"
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

(defn update-deleted [tbl-kw id]
  (db/hs-exe! {:update tbl-kw
               :set {:deleted (ut/now-ts)}
               :where [:= :id id]}))

(def delete-template-prompt (partial update-deleted :form_template_prompt))
(def delete-form-prompt-field (partial update-deleted :prompt_fields))
(def delete-form-prompt (partial update-deleted :form_prompt))
(def delete-form-template-prompt (partial update-deleted :form_template_prompt))
(def delete-doc-response (partial update-deleted :doc_resp))

(defn find-latest-form-template-id [where]
  "When there's multiple form templates that match some criteria, select the most recently created."
  (-> {:select [:id]
       :from [:form_templates]
       :where where
       :order-by [[:created :desc]]
       :limit 1}
      db/hs-query
      first
      :id))

(defn product-id->vendor-id [product-id]
  (-> {:select [:vendor_id]
       :from [:products]
       :where [:= :id product-id]
       :order-by [[:created :desc]]
       :limit 1}
      db/hs-query
      first
      :vendor_id))

(defn create-preposal-req-form
  [{:keys [prod-id] :as prep-req}]
  (create-form-from-template
   (merge {:form-template-id (find-latest-form-template-id [:= :ftype "preposal"])
           :to-org-id (product-id->vendor-id prod-id)
           :title (str "Preposal Request " (gensym ""))
           :status "init"
           :subject prod-id}
          prep-req)))

(defn create-doc-from-form-doc
  [{:keys [id doc-title prompts from-org from-user to-org to-user
           doc-descr doc-notes doc-dtype doc-dsubtype product] :as form-doc}]
  (with-doc-handling form-doc
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
                                       :fields fields})))))

(defn- get-child-responses
  [doc-id]
  (some->> [[:docs {:id doc-id}
             [:id
              [:responses
               {:deleted nil
                :ref-deleted nil}
               [:id :deleted :ref-id :prompt-id
                [:fields {:deleted nil
                          :_order_by {:idx :asc}}
                 [:sval :nval :dval :jval
                  [:prompt-field [:fname]]]]]]]]]
           ha/sync-query
           :docs
           first
           :responses))

(defn response-fields-eq?
  [old-field {:keys [response ftype]}]
  (let [state (mapv :state response)]
    (case ftype
      "s" (= (mapv :sval old-field)
             state)
      "n" (= (mapv :nval old-field)
             (ut/->long state))
      "d" (= (mapv (comp str :dval) old-field) ;; HACK
             state)
      "e" (= (mapv :sval old-field)
             state)
      ("i" "j") (= (mapv :jval old-field)
                   state)
      "u" (= (mapv :sval old-field)
             state))))

(defn update-response-from-form-doc
  [doc-id {old-fields :fields :keys [ref-id]} {prompt-id :id new-fields :fields :keys [response]}] 
  ;; TODO handle if old or new fields are missing
  (cond (and (not-empty old-fields) (not-empty new-fields))
        (let [old-fields' (group-by (comp :fname :prompt-field) old-fields)
              new-fields' (group-by :fname new-fields)
              resp-ids (keys new-fields')]
          (when (->> resp-ids
                     (map #(response-fields-eq? (-> % old-fields')
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

(defn update-doc* [d]
  (try
    (db/update-any! d :docs)
    (when *docs-updated*
      (swap! *docs-updated* conj d))    
    (catch Exception e
      (com/log-error e))))

(defn update-doc-from-form-doc
  [{:keys [id doc-id doc-title responses from-org from-user to-org to-user
           doc-descr doc-notes doc-dtype doc-dsubtype product]
    :as form-doc}]
  (with-doc-handling form-doc
    (update-doc*
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
      :to_user_id (:id from-user)})
    (update-responses-from-form-doc form-doc)))

(defn create-blank-form-template-prompt
  [form-template-id]
  (let [{:keys [id]} (insert-prompt {:prompt "Empty Prompt"
                                     :descr "Empty Description"})]
    (insert-form-template-prompt form-template-id
                                 id)))

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

(defn select-form-prompt-id [prompt-id form-id & fields]
  (-> {:select (or [:id] fields)
       :from [:form_prompt]
       :where [:and
               [:= :deleted nil]
               [:= :form_id form-id]
               [:= :prompt_id prompt-id]]}
      db/hs-query
      first
      :id))

(defn select-doc-response-id [response-id doc-id]
  (-> {:select [:id]
       :from [:doc_resp]
       :where [:and
               [:= :deleted nil]
               [:= :resp_id response-id]
               [:= :doc_id doc-id]]}
      db/hs-query
      first
      :id))


(defn delete-form-prompt-by-ids [prompt-id form-id]
  (delete-form-prompt
   (select-form-prompt-id prompt-id form-id)))

(defn delete-doc-response-by-ids [response-id doc-id]
  (delete-doc-response
   (select-doc-response-id response-id doc-id)))


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
  [{:keys [dtype dsubtype update-doc-id form-id] :as d}]
  (when (or dtype dsubtype update-doc-id form-id)
    (let [form-fields [:id :ftype :fsubtype
                       [:prompts
                        [:id :term
                         [:fields
                          [:id :fname :ftype :fsubtype]]]]]
          form-args (if form-id
                      {:id form-id}
                      (merge {:_order_by {:created :desc}
                              :_limit 1
                              :deleted nil}
                             (when dtype
                               {:ftype dtype})
                             (when dsubtype
                               {:fsubtype dsubtype})))]
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

;; TODO support list?=true / multiple values per field
(defn fields->proc-tree
  "Prepare fields for proc-tree"
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

(defn response-prompts->proc-tree
  "Prepare response prompts for proc-tree"
  [{:keys [terms prompt-ids]} prompts]
  (let [responses (merge terms prompt-ids)
        grouped-prompts (-> (merge (group-by (comp keyword :term) prompts)
                                   (group-by :id prompts))
                            (dissoc nil))]
    (vec (remove nil?
                 (for [[k r] responses]
                   (let [{prompt-id :id :keys [fields]} (-> k grouped-prompts first)]
                     (when prompt-id
                       {:item {}
                        :children [{:item {:prompt-id prompt-id}
                                    :children (fields->proc-tree r fields)}]})))))))

;; TODO set responses.subject
(defn doc->proc-tree
  "Prepare doc for proc-tree"
  [{:keys [data dtype dsubtype update-doc-id] :as d}]
  (if-let [{:keys [id ftype fsubtype prompts]} (doc->appliable--find-form d)]
    {:handler-args d
     :item (merge (select-keys d ;; TODO hard-coded fields, sucks -- Bill
                               [:title :dtype :descr :notes :from-org-id
                                :from-user-id :to-org-id :to-user-id
                                :dtype :dsubtype :form-id :subject])
                  {:form-id id
                   :dtype ftype
                   :dsubtype fsubtype}
                  (when update-doc-id
                    {:id update-doc-id}))
     :children (response-prompts->proc-tree data
                                            prompts)}
    (throw (Exception. (str "Couldn't find form by querying with: "
                            {:ftype dtype
                             :fsubtype dsubtype
                             :doc-id update-doc-id})))))

(defn create-doc [d]
  (->> d
       doc->proc-tree
       (proc-tree
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
  "Some kind of grouping multiplexer and diff'er"
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
       doc->proc-tree
       (proc-tree
        [(tree-assoc-fn [item children]
                        (-> item
                            ha/walk-clj-kw->sql-field
                            (select-keys [:doc_id :user_id :id :user_id :resp_id])
                            update-doc*)
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

(defn round-init-doc-id->create-form-proc-tree
  [round-init-doc-id]
  (let [{:keys [response-prompts] :as doc} (-> [[:docs {:id round-init-doc-id}
                                                 [:title
                                                  [:response-prompts
                                                   {:prompt-term "rounds/requirements"}
                                                   [[:fields [:id :sval :idx]]]]]]]
                                               ha/sync-query
                                               :docs
                                               first)]
    {:item doc
     :children (->> response-prompts first :fields
                    (map (fn [field] {:item field})))}))

(defn get-prompts-by-id [id]
  (-> [[:prompts
        {:id id}
        [:id]]]
      ha/sync-query
      :prompts))

(defn get-prompts-by-sval [sval]
  (-> [[:prompts
        {:prompt sval} 
        [:id]]]
      ha/sync-query
      :prompts))

(defn get-prompts-by-term [term]
  (-> [[:prompts
        {:term term} 
        [:id]]]
      ha/sync-query
      :prompts))

;; TODO the prompt-id for existing prompts should be present in field jval -- Bill
(defn group-by-prompt-exists
  [prompts]
  (ut/$- ->> prompts
         (map (fn [{:keys [item]}]
                {:item
                 (merge (-> item
                            :sval ;; the sval is used to carry over the prompt's id
                            (#(when-not (s/starts-with? % "new-topic/")
                                (get-prompts-by-id %)))
                            first)
                        (dissoc item :id))}))
         (group-by (comp nil? :id :item))
         (clojure.set/rename-keys $
                                  {false :prompt-exists
                                   true :prompt-new})))

(defn create-round-req-prompt&fields [requirement-text]
  (let [{:keys [id] :as prompt} (insert-prompt {:prompt requirement-text})]
    (insert-prompt-field {:prompt-id id
                          :fname "value"
                          :ftype "s"
                          :fsubtype "multi"
                          :list? false})
    prompt))

(defn create-form-template-from-round-doc
  [round-id round-init-doc-id]
  (->> round-init-doc-id
       round-init-doc-id->create-form-proc-tree
       (proc-tree
        [(tree-assoc-fn [item children]
                        [:item (-> item
                                   (assoc :ftype "round-product-requirements"
                                          :fsubtype (str "round-product-requirements-" round-id))
                                   insert-form-template)
                         :children (group-by-prompt-exists children)])
         {:prompt-exists [] ;; [] acts like `identity`
          :prompt-new [(tree-assoc-fn [item]
                                      (let [{:keys [sval idx]} item]
                                        [:item (-> sval
                                                   ;; In frontend, new topics are given a fake id
                                                   ;; like so: "new-topic/Topic Text That User Entered"
                                                   (s/replace #"new-topic/" "")
                                                   create-round-req-prompt&fields
                                                   (assoc :idx idx))]))]}
         (tree-assoc-fn [children]
                        [:children (->> children vals (apply concat))])
         {:prompt-all [(tree-assoc-fn [item children parent]
                                      [:item (insert-form-template-prompt (:id parent)
                                                                          (:id item)
                                                                          (:idx item))])]}])
       :item))


(defn upsert-prompts-to-form
  [prompts {form-id :id existing :prompts}]
  (let [id-set (->> existing
                    (map :id)
                    set)]
    (doseq [{prompt-id :id sort' :sort} prompts]
      (when-not (id-set prompt-id)
        (insert-form-prompt form-id prompt-id sort')))
    form-id))

(defn merge-template-to-forms
  "Find all prompts for a given form templates and upsert them to all
  forms it has spawned. Use this to propoagate templates changes to
  forms."
  [req-form-template-id]
  (let [prompts (->> [[:form-templates {:id req-form-template-id}
                       [[:prompts [:id :sort]]]]]
                     ha/sync-query
                     :form-templates
                     first
                     :prompts)]
    (->> [[:forms {:form-template-id req-form-template-id}
           [:id
            [:prompts [:id]]]]]
         ha/sync-query
         :forms
         (map (partial upsert-prompts-to-form prompts))
         doall)))

;; TODO check not deleted
(defn select-missing-prompt-responses-by-doc-id
  [doc-id]
  (->> {:select [[:p.id :prompt-id]
                 [:p.prompt :prompt-term]
                 [:f.id :form-id]]
        :from [[:docs :d]]
        :join [[:forms :f]
               [:= :d.form_id :f.id]
               [:form_prompt :fp]
               [:= :fp.form_id :f.id]
               [:prompts :p]
               [:= :p.id :fp.prompt_id]]
        :left-join [[:doc_resp :dr]
                    [:= :dr.doc_id :d.id]
                    [:responses :r]
                    [:and
                     [:= :r.id :dr.resp_id]
                     [:= :r.prompt_id :fp.prompt_id]]]
        :where [:and
                [:= :d.id doc-id]
                [:= :r.id nil]]}
       db/hs-query))

(defn find-prompt-field-value
  [{:keys [nval dval sval jval] :as m}]
  (or jval dval nval sval))

(defn select-reusable-response-fields
  "Find existing response fields that can be reused for a new doc (round) based on author, recipient and which product is the subject."
  [subject from-org-id to-org-id prompt-rows]
  (let [prompt-ids (->> prompt-rows (map :prompt-id) distinct)
        prompt-terms (->> prompt-rows (map :prompt-term) distinct)
        prompt-field-ids (->> prompt-rows (map :prompt-field-id) distinct)]
    (->> {:select [[:prompt_id :prompt-id]
                   [:prompt_term :prompt-term]
                   [:response_id :response-id]
                   [:resp_field_nval :nval]
                   [:resp_field_dval :dval]
                   [:resp_field_sval :sval]
                   [:resp_field_jval :jval]
                   [:resp_field_idx :idx]]
          :from [:docs_to_fields]
          :where [:and
                  [:or
                   [:and
                    [:in :doc_subject subject]
                    [:or
                     [:= :doc_dtype "product-profile"]
                     [:and
                      [:= :doc_dtype "preposal"]
                      [:= :doc_to_org_id from-org-id]]]]
                   [:and
                    [:= :doc_dtype "vendor-profile"]
                    [:= :doc_from_org_id to-org-id]]]
                  [:or
                   [:in :prompt_id prompt-ids]
                   [:in :prompt_term prompt-terms]]]}
         db/hs-query
         (group-by #(or (:prompt-term %)
                        (:prompt-id %)))
         vals
         (map first)
         (map #(assoc %
                      :value (find-prompt-field-value %))))))

(defn reuse-responses
  "add doc_resp references to doc that point to reusable responses"
  [doc-id responses]
  (doseq [{:keys [response-id]} responses]
    (insert-doc-response doc-id response-id)))

(defn select-round-product-ids-by-doc-id
  [doc-id]
  (->> {:select [[:rp.product_id :product-id]]
        :modifiers [:distinct]
        :from [[:docs :d]]
        :join [[:round_product :rp]
               [:and
                [:= :d.subject :rp.id]
                [:= :rp.deleted nil]]]
        :where [:= :d.id doc-id]}
       db/hs-query
       (map :product-id)))

(defn auto-pop-missing-responses-by-doc-id
  [doc-id]
  (let [product-ids (select-round-product-ids-by-doc-id doc-id)
        {:keys [from-org-id to-org-id]} (->> [[:form-docs {:doc-id doc-id}
                                               [:from-org-id :to-org-id]]]
                                             ha/sync-query
                                             :form-docs
                                             first)]
    (->> doc-id
         select-missing-prompt-responses-by-doc-id
         (select-reusable-response-fields product-ids from-org-id to-org-id)
         (reuse-responses doc-id))))


(defn set-form-template-prompts-order
  [form-template-id prompt-ids]
  (let [indexed (map-indexed vector prompt-ids)]
    (doseq [[idx id] indexed]
      (db/update-where :form_template_prompt
                       {:sort idx}
                       [:and
                        [:= :prompt_id id]
                        [:= :form_template_id form-template-id]]))))
