(ns com.vetd.app.admin
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.rounds :as rounds]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.docs :as docs]
            [taoensso.timbre :as log]
            clojure.data))

(defn search-orgs->ids
  [q]
  (if (not-empty q)
    (let [ids (db/hs-query {:select [[:o.id :oid]]
                            :from [[:orgs :o]]
                            :where [(keyword "~*") :o.oname (str ".*?\\m" q ".*")]
                            :limit 30})
          vids (->> ids
                    (map :oid)
                    distinct)]
      {:org-ids vids})
    {:org-ids []}))



(defn delete-product-id-from-round
  [product-id round-id]
  (db/hs-exe! {:update :round_product
               :set {:deleted (ut/now-ts)}
               :where [:and
                       [:= :round_id round-id]
                       [:= :product_id product-id]]}))

(defmethod com/handle-ws-inbound :a/search
  [{:keys [query]} ws-id sub-fn]
  (search-orgs->ids query))

(defmethod com/handle-ws-inbound :a/create-preposal-req
  [{:keys [prep-req]} ws-id sub-fn]
  (docs/create-preposal-req-form prep-req))

(defmethod com/handle-ws-inbound :a/create-form-template-prompt
  [{:keys [form-template-id]} ws-id sub-fn]
  (docs/create-blank-form-template-prompt form-template-id))

(defmethod com/handle-ws-inbound :a/create-prompt-field
  [{:keys [prompt-id]} ws-id sub-fn]
  (docs/insert-default-prompt-field prompt-id {:sort 0}))

(defmethod com/handle-ws-inbound :a/dissoc-template-prompt
  [{:keys [form-template-prompt-id]} ws-id sub-fn]
  (docs/delete-template-prompt form-template-prompt-id))

(defmethod com/handle-ws-inbound :a/add-existing-form-template-prompt
  [{:keys [form-template-prompt-id prompt-id]} ws-id sub-fn]
  (docs/insert-form-template-prompt form-template-prompt-id
                                    prompt-id
                                    100))

(defmethod com/handle-ws-inbound :a/delete-form-prompt-field
  [{:keys [prompt-field-id]} ws-id sub-fn]
  (docs/delete-form-prompt-field prompt-field-id))

(defmethod com/handle-ws-inbound :a/create-form-from-template
  [{:keys [form-template-id]} ws-id sub-fn]
  (docs/create-form-from-template {:form-template-id form-template-id}))

(defmethod com/handle-ws-inbound :a/update-any
  [{:keys [entity]} ws-id sub-fn]
  (db/update-any! entity))

(defmethod com/handle-ws-inbound :a/set-round-products
  [{:keys [product-ids round-id]} ws-id sub-fn]
  (let [existing-prod-ids (->> [[:rounds {:id round-id}
                                 [[:products {:deleted nil
                                              :ref-deleted nil}
                                   [:id]]]]]
                               ha/sync-query
                               :rounds
                               first
                               :products
                               (map :id))
        [remove-ids add-ids] (clojure.data/diff (set existing-prod-ids)
                                                (set product-ids))]
    (doseq [product-id remove-ids]
      (delete-product-id-from-round product-id round-id))
    (doseq [product-id add-ids]
      (rounds/invite-product-to-round product-id round-id))
    (rounds/sync-round-vendor-req-forms round-id)))
