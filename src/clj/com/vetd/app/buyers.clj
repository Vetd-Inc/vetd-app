(ns com.vetd.app.buyers
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.auth :as auth]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.docs :as docs]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as aws-creds]
            [taoensso.timbre :as log]))

(def sns (aws/client {:api :sns
                      :region "us-east-1"
                      :credentials-provider (aws-creds/basic-credentials-provider
                                             ;; TODO cycle creds and keep in env
                                             {:access-key-id "AKIAIJN3D74NBHJIAARQ"
                                              :secret-access-key "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"})}))

(defn search-prods-vendors->ids
  [q]
  (if (not-empty q)
    (let [ids (db/hs-query {:select [[:p.id :pid] [:o.id :vid]]
                            :from [[:products :p]]
                            :join [[:orgs :o] [:= :o.id :p.vendor_id]]
                            :where [:or
                                    [(keyword "~*") :p.pname (str ".*?" q ".*")]
                                    [(keyword "~*") :o.oname (str ".*?" q ".*")]]
                            :limit 30})
          pids (map :pid ids)
          vids (->> ids
                    (map :vid)
                    distinct)]
      {:product-ids pids
       :vendor-ids vids})
      {:product-ids []
       :vendor-ids []}))

(defn search-category-ids
  [q]
  (if (not-empty q)
    (mapv :id
     (db/hs-query {:select [:id]
                   :from [:categories]
                   :where [(keyword "~*") :cname (str ".*?" q ".*")]
                   :limit 5}))))

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
  [buyer-id]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :rounds
                    {:id id
                     :idstr idstr
                     :buyer_id buyer-id
                     :status "active"
                     :created (ut/now-ts)
                     :updated (ut/now-ts)})
        first)))

(defn insert-round-product
  [round-id prod-id]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :round_product
                    {:id id
                     :idstr idstr
                     :round_id round-id
                     :product_id prod-id
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
  [buyer-id eid etype]
  (let [{:keys [id] :as r} (insert-round buyer-id)]
    (case etype
      :product (insert-round-product id eid)
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
        (aws/invoke sns {:op :Publish
                         :request {:TopicArn "arn:aws:sns:us-east-1:744151627940:ui-req-new-prod-cat"
                                   :Subject "Vetd Round Started"
                                   :Message msg}}))
      (catch Throwable t))
    r))

(defn send-new-prod-cat-req [uid oid req]
  (let [user-name (-> uid auth/select-user-by-id :uname)
        org-name (-> oid auth/select-org-by-id :oname)]
    (aws/invoke sns {:op :Publish
                     :request {:TopicArn "arn:aws:sns:us-east-1:744151627940:ui-req-new-prod-cat"
                               :Subject "New Product/Category Request"
                               :Message (format
                                         "New Product/Category Request
Request Text '%s'
Org '%s'
User '%s'
"
                                         req org-name user-name)}})))

(defn send-vendor-profile-req [vendor-id buyer-id]
  (let [vendor-name (-> vendor-id auth/select-org-by-id :oname)
        buyer-name (-> buyer-id auth/select-org-by-id :oname)]
    (aws/invoke sns {:op :Publish
                     :request {:TopicArn "arn:aws:sns:us-east-1:744151627940:ui-misc"
                               :Subject "Vendor Profile Request"
                               :Message (format
                                         "Vendor Profile Request
Buyer: '%s'
Vendor: '%s'
"
                                         buyer-name vendor-name)}})))


(defn send-setup-call-req [buyer-id product-id]
  (let [product-name (-> [[:products {:id product-id} [:pname]]]
                        ha/sync-query
                        vals
                        ffirst
                        :pname)
        buyer-name (-> buyer-id auth/select-org-by-id :oname)]
    (aws/invoke sns {:op :Publish
                     :request {:TopicArn "arn:aws:sns:us-east-1:744151627940:ui-misc"
                               :Subject "Setup Call Request"
                               :Message (format
                                         "Setup Call Request
Buyer: '%s'
Product: '%s'
"
                                         buyer-name product-name)}})))

(defn send-ask-question-req [product-id message buyer-id]
  (let [product-name (-> [[:products {:id product-id} [:pname]]]
                        ha/sync-query
                        vals
                        ffirst
                        :pname)
        buyer-name (-> buyer-id auth/select-org-by-id :oname)]
    (aws/invoke sns {:op :Publish
                     :request {:TopicArn "arn:aws:sns:us-east-1:744151627940:ui-misc"
                               :Subject "Ask a Question Request"
                               :Message (format
                                         "Ask a Question Request
Buyer: '%s'
Product: '%s'
Message:
%s
"
                                         buyer-name product-name message)}})))

(defn send-prep-req
  [{:keys [to-org-id to-user-id from-org-id from-user-id prod-id] :as prep-req}]
  (let [product-name (-> [[:products {:id prod-id} [:pname]]]
                         ha/sync-query
                         vals
                         ffirst
                         :pname)
        buyer-name (-> from-org-id auth/select-org-by-id :oname)
        from-user-name (-> from-user-id auth/select-user-by-id :uname)]
    (aws/invoke sns {:op :Publish
                     :request {:TopicArn "arn:aws:sns:us-east-1:744151627940:ui-misc"
                               :Subject "Preposal Request"
                               :Message (format
                                         "Preposal Request
Buyer: '%s'
Buyer User: '%s'

Product: '%s'
"
                                         buyer-name from-user-name
                                         product-name)}})))

;; TODO there could be multiple preposals/rounds per buyer-vendor pair

;; TODO use session-id to verify permissions!!!!!!!!!!!!
(defmethod com/handle-ws-inbound :b/search
  [{:keys [buyer-id query]} ws-id sub-fn]
  (-> query
      search-prods-vendors->ids
      (assoc :category-ids
             (search-category-ids query))))

(defmethod com/handle-ws-inbound :b/create-preposal-req
  [{:keys [prep-req]} ws-id sub-fn]
  (send-prep-req prep-req)
  (docs/create-preposal-req-form prep-req))

;; TODO record which user started round
(defmethod com/handle-ws-inbound :b/start-round
  [{:keys [etype buyer-id eid]} ws-id sub-fn]
  (create-round buyer-id eid etype))

(defmethod com/handle-ws-inbound :b/req-new-prod-cat
  [{:keys [user-id org-id req]} ws-id sub-fn]
  (send-new-prod-cat-req user-id org-id req))

(defmethod com/handle-ws-inbound :b/request-vendor-profile
  [{:keys [vendor-id buyer-id]} ws-id sub-fn]
  (send-vendor-profile-req vendor-id buyer-id))

(defmethod com/handle-ws-inbound :b/setup-call
  [{:keys [buyer-id product-id]} ws-id sub-fn]
  (send-setup-call-req buyer-id product-id))

(defmethod com/handle-ws-inbound :b/ask-a-question
  [{:keys [product-id message buyer-id]} ws-id sub-fn]
  (send-ask-question-req product-id message buyer-id))

(defmethod docs/handle-doc-creation :round-initiation
  [{:keys [id]}]
  (Thread/sleep 7000) ;;HACK wait til related inserts are probably done
  ;; TODO update round rec status
  (try
    (let [msg (with-out-str
                (clojure.pprint/with-pprint-dispatch clojure.pprint/code-dispatch
                  (-> [[:docs {:id id}
                        [:rounds
                         [:id :created
                          [:buyer [:oname]]
                          [:products [:pname]]
                          [:categories [:cname]]]]]]
                      ha/sync-query
                      vals
                      ffirst
                      clojure.pprint/pprint)))]
      ;; TODO make msg human friendly
      (aws/invoke sns {:TopicArn "arn:aws:sns:us-east-1:744151627940:ui-misc"
                       :Subject "Vendor Round Requirements Form Completed"
                       :Message (str "Vendor Round Requirements Form Completed\n\n"
                                     msg)}))
    (catch Throwable t)))
