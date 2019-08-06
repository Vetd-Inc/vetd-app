(ns com.vetd.app.buyers
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.auth :as auth]
            [com.vetd.app.rounds :as rounds]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.docs :as docs]
            [taoensso.timbre :as log]
            [clojure.string :as s]))

(defn product-id->name
  [product-id]
  (-> [[:products {:id product-id}
        [:pname]]]
      ha/sync-query
      vals
      ffirst
      :pname))

(defn search-prods-vendors->ids
  [q cat-ids filter-map]
  (if (not-empty q)
    (let [{:keys [features groups discounts-available-to-groups]} filter-map
          {:keys [free-trial]} features
          ids (db/hs-query
               {:select [[:p.id :pid]
                         [(honeysql.core/raw "coalesce(p.score, 1.0)") :nscore]]
                :from [[:products :p]]
                :join (concat [[:orgs :o] [:= :o.id :p.vendor_id]]
                              (when free-trial
                                [[:docs_to_fields :d2f] [:and
                                                         [:= :d2f.doc_subject :p.id]
                                                         [:= :d2f.doc_dtype "product-profile"]
                                                         [:= :d2f.prompt_term "product/free-trial?"]
                                                         [:= :d2f.resp_field_sval "yes"]]])
                              (when (not-empty groups)
                                [[:group_org_memberships :gom] [:in :gom.group_id groups]
                                 [:stack_items :si] [:= :gom.org_id :si.buyer_id]])
                              (when (not-empty discounts-available-to-groups)
                                [[:group_discounts :gd] [:and
                                                         [:in :gd.group_id discounts-available-to-groups]
                                                         [:= :gd.product_id :p.id]]]))
                :left-join (when (not-empty cat-ids)
                             [[:product_categories :pc] [:= :p.id :pc.prod_id]])
                :where [:and
                        [:= :p.deleted nil]
                        [:= :o.deleted nil]
                        [:or
                         [(keyword "~*") :p.pname (str ".*?\\m" q ".*")]
                         [(keyword "~*") :o.oname (str ".*?\\m" q ".*")]
                         (when (not-empty cat-ids)	
                           [:in :pc.cat_id cat-ids])]]
                :order-by [[:nscore :desc]]
                ;; this will be paginated on the frontend
                :limit 500})
          ;; could not the above be written to avoid the need for 'distinct'
          pids (distinct (map :pid ids))]
      {:product-ids pids})
    {:product-ids []}))

(defn search-category-ids
  [q]
  (if (not-empty q)
    (let [initials (when (#{3 4} (count q))
                     [(keyword "~*") :cname
                      (apply str (for [c q]
                                   (str "\\m" c ".*?")))])
          wh [(keyword "~*") :cname (str ".*?\\m" q ".*")]
          wh' (if initials
                [:or wh initials]
                wh)]
      (mapv :id
            (db/hs-query {:select [:id]
                          :from [:categories]
                          :where [:and
                                  [:= :deleted nil]
                                  wh']
                          :limit 5})))))

(defn select-rounds-by-ids
  [b-id v-ids]
  (db/hs-query {:select [:*]
                :from [:rounds]
                :where [:and
                        [:= :buyer-id b-id]
                        [:in :vendor-id v-ids]]}))

(defn insert-preposal-req
  [buyer-id vendor-id]
  (db/insert! :preposal_reqs
              {:id (ut/uuid-str)
               :buyer-id buyer-id
               :vendor-id vendor-id
               :created (ut/now)}))

#_(select-prep-reqs-by-ids 3 [1 2 3 4])

(defn invert-vendor-data
  [m]
  (let [m1 (ut/fmap #(group-by (some-fn :vendor-id :id) %)
                    m)
        paths (for [[k v] m1
                    [k2 v2] v]
                [[k2 k] (first v2)])]
    (reduce (fn [agg [ks v]]
              (assoc-in agg ks v))
            {}
            paths)))

(defn insert-round
  [buyer-id title]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :rounds
                    {:id id
                     :idstr idstr
                     :buyer_id buyer-id
                     :status "initiation"
                     :title title
                     :created (ut/now-ts)
                     :updated (ut/now-ts)})
        first)))

(defn insert-round-category
  [round-id category-id]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :round_category
                    {:id id
                     :idstr idstr
                     :round_id round-id
                     :category_id category-id
                     :created (ut/now-ts)
                     :updated (ut/now-ts)})
        first)))

(defn create-round
  [buyer-id title eid etype]
  (let [{:keys [id] :as r} (insert-round buyer-id title)]
    (case etype
      ;; TODO call sync-round-vendor-req-forms too, once we're ready
      :product (rounds/invite-product-to-round eid id) 
      :category (insert-round-category id eid))
    (try
      (let [msg (with-out-str
                  (clojure.pprint/with-pprint-dispatch clojure.pprint/code-dispatch
                    (-> [[:rounds {:id id}
                          [:id :created
                           [:buyer [:oname]]
                           [:products [:pname]]
                           [:categories [:cname]]]]]
                        ha/sync-query
                        vals
                        ffirst
                        clojure.pprint/pprint)))]
        ;; TODO make msg human friendly
        (com/sns-publish :ui-start-round "Vetd Round Started" msg))
      (catch Throwable t))
    r))

(defn send-new-prod-cat-req [uid oid req]
  (let [user-name (-> uid auth/select-user-by-id :uname)
        org-name (-> oid auth/select-org-by-id :oname)]
    (com/sns-publish :ui-req-new-prod-cat
                     "New Product/Category Request"
                     (format
                      "New Product/Category Request
Request Text '%s'
Org '%s'
User '%s'
"
                      req org-name user-name))))

(defn send-complete-profile-req [etype eid field-key buyer-id]
  (let [ename (if (= :vendor etype)
                (-> eid auth/select-org-by-id :oname)
                (product-id->name eid))
        buyer-name (-> buyer-id auth/select-org-by-id :oname)]
    (com/sns-publish :ui-misc
                     (str "Complete " (name etype) " Profile Request")
                     (str "Complete " (name etype) " Profile Request\n"
                          "buyer: " buyer-name "\n"
                          (name etype) ": " ename " (ID: " eid ")\n"
                          "field name: " field-key))))


(defn send-setup-call-req [buyer-id product-id]
  (let [{:keys [pname rounds]} (-> [[:products {:id product-id}
                                     [:pname
                                      [:rounds {:deleted nil}
                                       [:idstr]]]]]
                                   ha/sync-query
                                   :products
                                   first)]
    (com/sns-publish :ui-misc
                     "Setup Call Request"
                     (format
                      "Setup Call Request
Buyer (Org): '%s'
Product: '%s'
Round URLs (if any):
%s"
                      (-> buyer-id auth/select-org-by-id :oname) ; buyer name
                      pname
                      (->> (for [{:keys [idstr]} rounds]
                             (str "https://app.vetd.com/b/rounds/" idstr))
                           (clojure.string/join "\n"))))))

(defn send-ask-question-req [product-id message round-id requirement-text buyer-id]
  (com/sns-publish :ui-misc
                   "Ask a Question Request"
                   (str "Ask a Question Request"
                        "\nBuyer (Org): " (-> buyer-id auth/select-org-by-id :oname) ; buyer name
                        "\nProduct: " (product-id->name product-id) ; product name
                        (when round-id (str  "\nRound ID: " round-id))
                        (when requirement-text (str  "\nRequirement: " requirement-text))
                        "\nMessage:\n" message)))

(defn send-prep-req
  [{:keys [to-org-id to-user-id from-org-id from-user-id prod-id] :as prep-req}]
  (com/sns-publish :ui-misc
                   "PrePosal Request"
                   (format
                    "PrePosal Request
Buyer (Org): '%s'
Buyer User: '%s'
Product: '%s'"
                    (-> from-org-id auth/select-org-by-id :oname) ; buyer org name
                    (-> from-user-id auth/select-user-by-id :uname) ; buyer user name
                    (product-id->name prod-id)))) ; product name

(defn set-preposal-result [id result reason]
  "Set the result of a preposal (0 - rejected, nil - live)."
  (db/update-any! {:id id
                   :result result
                   :reason reason}
                  :docs))

(defn set-round-product-result [round-id product-id result reason]
  "Set the result of a product in a round (0 - disqualified, 1 - winner)."
  (do (when (= 1 result)
        (try (let [{:keys [idstr buyer products]} (-> [[:rounds {:id round-id}
                                                        [:idstr
                                                         [:buyer [:oname]]
                                                         [:products {:id product-id}
                                                          [:pname]]]]]
                                                      ha/sync-query
                                                      vals
                                                      ffirst)]
               (com/sns-publish :ui-misc
                                "Round Winner Declared"
                                (format
                                 "Round Winner Declared
Buyer: '%s'
Product: '%s'
Round URL: https://app.vetd.com/b/rounds/%s"
                                 (:oname buyer)
                                 (-> products first :pname)
                                 idstr)))
             (catch Exception e
               (com/log-error e))))
      (when-let [id (->> [[:round-product {:round-id round-id
                                           :product-id product-id
                                           :deleted nil}
                           [:id]]]
                         ha/sync-query
                         :round-product
                         first
                         :id)]
        (db/update-any! {:id id
                         :result result
                         :reason reason}
                        :round_product))
      (when (= 1 result)    ; additional effects of declaring a winner
        (let [rps (->> [[:round-product {:round-id round-id
                                         :result nil
                                         :deleted nil}
                         [:id]]]
                       ha/sync-query
                       :round-product)]
          ;; disqualify any live products in the round
          (doseq [{:keys [id]} rps]
            (db/update-any! {:id id
                             :result 0
                             :reason "A different product was declared winner."}
                            :round_product))
          ;; update round status
          (db/update-any! {:id round-id
                           :status "complete"}
                          :rounds)))))

(defn add-requirement-to-round
  "Add requirement to round by Round ID or by the form template ID of requirements form template."
  [requirement-text & [{:keys [round-id form-template-id]}]]
  (let [req-form-template-id (or form-template-id
                                 (-> [[:rounds {:id round-id}
                                       [:req-form-template-id]]]
                                     ha/sync-query
                                     vals
                                     ffirst
                                     :req-form-template-id))
        {:keys [id]} (-> requirement-text
                         docs/get-prompts-by-sval
                         first
                         (or (docs/create-round-req-prompt&fields requirement-text)))
        existing-prompts (docs/select-form-template-prompts-by-parent-id req-form-template-id)]
    (when-not (some #(= id (:prompt-id %)) existing-prompts)
      (docs/insert-form-template-prompt req-form-template-id id)
      (docs/merge-template-to-forms req-form-template-id))))

;; TODO there could be multiple preposals/rounds per buyer-vendor pair

;; TODO use session-id to verify permissions!!!!!!!!!!!!!
(defmethod com/handle-ws-inbound :b/search
  [{:keys [buyer-id query filter-map]} ws-id sub-fn]
  (let [cat-ids (search-category-ids query)]
    (ut/$- -> query
           (search-prods-vendors->ids $ cat-ids filter-map)
           (assoc :category-ids cat-ids))))

;; Start a round for either a Product or a Category
;; TODO record which user started round
(defmethod com/handle-ws-inbound :b/start-round
  [{:keys [buyer-id title etype eid]} ws-id sub-fn]
  (create-round buyer-id title eid etype))

;; Request Preposal              TODO this can be refactored to something like :b/preposals.request
(defmethod com/handle-ws-inbound :b/create-preposal-req
  [{:keys [prep-req]} ws-id sub-fn]
  (send-prep-req prep-req)
  (docs/create-preposal-req-form prep-req))

;; [Reject]/[Undo Reject] a Preposal
(defmethod com/handle-ws-inbound :b/preposals.set-result
  [{:keys [id result reason buyer-id] :as req} ws-id sub-fn]
  (set-preposal-result id result reason)
  {})

;; Request an addition to our Products / Categories
(defmethod com/handle-ws-inbound :b/req-new-prod-cat
  [{:keys [user-id org-id req]} ws-id sub-fn]
  (send-new-prod-cat-req user-id org-id req))

;; Request that a vendor complete their Company/Product Profile
(defmethod com/handle-ws-inbound :b/request-complete-profile
  [{:keys [etype eid field-key buyer-id]} ws-id sub-fn]
  (send-complete-profile-req etype eid field-key buyer-id))

;; Have Vetd set up a phone call for the buyer with the vendor
(defmethod com/handle-ws-inbound :b/setup-call
  [{:keys [buyer-id product-id]} ws-id sub-fn]
  (send-setup-call-req buyer-id product-id))

;; Ask a question about a specific product
(defmethod com/handle-ws-inbound :b/ask-a-question
  [{:keys [product-id message round-id requirement-text buyer-id]} ws-id sub-fn]
  (send-ask-question-req product-id message round-id requirement-text buyer-id))

(defmethod com/handle-ws-inbound :b/round.add-requirements
  [{:keys [round-id requirements]} ws-id sub-fn]
  (let [{:keys [idstr buyer req-form-template-id]} (-> [[:rounds {:id round-id}
                                                         [:idstr :req-form-template-id
                                                          [:buyer [:oname]]]]]
                                                       ha/sync-query
                                                       vals
                                                       ffirst)]
    (doseq [requirement requirements]
      (add-requirement-to-round requirement {:form-template-id req-form-template-id}))
    (com/sns-publish :ui-misc
                     "New Topics Added to Round"
                     (format
                      "New Topics Added to Round
Buyer: '%s'
Topics: '%s'
Round URL: https://app.vetd.com/b/rounds/%s"
                      (:oname buyer)
                      (s/join ", " requirements)
                      idstr))
    {}))

(defmethod com/handle-ws-inbound :save-doc
  [{:keys [data ftype update-doc-id from-org-id] :as req} ws-id sub-fn]
  (if (nil? update-doc-id)
    (docs/create-doc req)
    (docs/update-doc req)))

;; result - 0 (disqualify), 1 (winner), nil (undisqualify, etc...)
(defmethod com/handle-ws-inbound :b/round.declare-result
  [{:keys [round-id product-id buyer-id result reason] :as req} ws-id sub-fn]
  (set-round-product-result round-id product-id result reason)
  {})

(defmethod com/handle-ws-inbound :save-response
  [{:keys [subject subject-type term user-id round-id org-id fields] :as req} ws-id sub-fn]
  (when-not (= term :round.response/rating)
    (throw (Exception. (format "NOT IMPLEMENTED: term = %s"
                               term))))
  (let [rating-prompt-id 1093760230399 ;; HACK -- hard-coded id
        rating-prompt-field-id 1093790890400]
    (db/update-deleted-where :responses
                             [:and
                              [:= :prompt_id rating-prompt-id]
                              [:= :subject subject]])
    (let [{:keys [value]} fields
          {:keys [id]} (-> req
                           (assoc :prompt-id rating-prompt-id)
                           docs/insert-response)]
      (docs/insert-response-field id
                                  {:prompt-field-id rating-prompt-field-id
                                   :idx 0
                                   :sval nil
                                   :nval value
                                   :dval nil
                                   :jval nil}))))

(defn notify-round-init-form-completed
  [doc-id]
  (let [round (-> [[:docs {:id doc-id}
                    [[:rounds
                      [:id :created
                       [:buyer [:oname]]
                       [:products [:pname]]
                       [:categories [:cname]]
                       [:init-doc
                        [:id
                         [:response-prompts {:ref-deleted nil}
                          [:id :prompt-id :prompt-prompt :prompt-term
                           [:response-prompt-fields
                            [:id :prompt-field-fname :idx
                             :sval :nval :dval]]]]]]]]]]]
                  ha/sync-query
                  vals
                  ffirst
                  :rounds)]
    (com/sns-publish
     :ui-misc
     "Vendor Round Initiation Form Completed"
     (str "Vendor Round Initiation Form Completed\n\n"
          (str "Buyer (Org): " (-> round :buyer :oname)
               "\nProducts: " (->> round :products (map :pname) (interpose ", ") (apply str))
               "\nCategories: " (->> round :categories (map :cname) (interpose ", ") (apply str))
               "\n-- Form Data --"
               (apply str
                      (for [rp (-> round :init-doc :response-prompts)]
                        (str "\n" (:prompt-prompt rp) ": "
                             (->> rp :response-prompt-fields (map :sval) (interpose ", ") (apply str))))))))))

(defn set-round-products-order [round-id product-ids]
  (doall
   (map-indexed
    (fn [idx product-id]
      (db/update-where :round_product
                       {:sort idx}
                       [:and
                        [:= :round_id round-id]
                        [:= :product_id product-id]]))
    product-ids)))

;; additional side effects upon creating a round-initiation doc
(defmethod docs/handle-doc-creation :round-initiation
  [{:keys [id]} {:keys [round-id]}]
  (let [{form-template-id :id} (try (docs/create-form-template-from-round-doc round-id id)
                                    (catch Throwable t
                                      (com/log-error t)))]
    
    ;; TODO invite pre-selected product, if there is one
    (try
      (db/update-any! {:id round-id
                       :doc_id id
                       :req_form_template_id form-template-id
                       :status "in-progress"}
                      :rounds)
      (catch Throwable t
        (com/log-error t)))
    (try
      (notify-round-init-form-completed id)
      (catch Throwable t
        (com/log-error t)))))

(defmethod com/handle-ws-inbound :b/round.add-products
  [{:keys [round-id product-ids product-names buyer-id]} ws-id sub-fn]
  (when-not (empty? product-ids)
    (com/sns-publish
     :ui-misc
     "Product(s) Added to Round"
     (str "Product(s) Added to Round\n\n"
          "Round ID: " round-id
          "\nRound Link: https://app.vetd.com/b/rounds/" (ut/base31->str round-id)
          "\nProduct(s) Added: " (s/join ", " product-ids))))
  (when-not (empty? product-names)
    (com/sns-publish
     :ui-misc
     "Nonexistent Product(s) Added to Round"
     (str "Nonexistent Product(s) Added to Round\n\n"
          "Round ID: " round-id
          "\nRound Link: https://app.vetd.com/b/rounds/" (ut/base31->str round-id)
          "\nNonexistent Product(s) Added: " (s/join ", " product-names))))
  (when-not (empty? product-ids)
    (doseq [product-id product-ids]
      (rounds/invite-product-to-round product-id round-id))
    (rounds/sync-round-vendor-req-forms round-id))
  {})

(defmethod com/handle-ws-inbound :b/set-round-products-order
  [{:keys [product-ids user-id org-id round-id]} ws-id sub-fn]
  (set-round-products-order round-id product-ids))

(defmethod com/handle-ws-inbound :b/round.share
  [{:keys [round-id round-title email-addresses buyer-id]} ws-id sub-fn]
  (com/sns-publish
   :ui-misc
   "Share VetdRound"
   (str "Share VetdRound"
        "\n\nBuyer Name: " (-> buyer-id auth/select-org-by-id :oname)
        "\nRound ID: " round-id
        "\nRound Link: https://app.vetd.com/b/rounds/" (ut/base31->str round-id)
        "\nRound Title: " round-title
        "\nEmail Addresses: " (s/join ", " email-addresses)))
  {})
