(ns com.vetd.app.rounds
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.docs :as docs]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.proc-tree :as ptree]
            [com.vetd.app.util :as ut]))


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

(defn invite-product-to-round
  [product-id round-id]
  (insert-round-product round-id product-id))

(defn sync-round-vendor-req-forms-to-add
  [prod-id->exists {:keys [product-id] forms :vendor-response-form-docs}]
  (and (empty? forms)
       (prod-id->exists product-id)))

(defn sync-round-vendor-req-forms-to-remove
  [prod-id->exists {:keys [product-id] forms :vendor-response-form-docs}]
  (not (or (empty? forms)
           (prod-id->exists product-id))))

(defn sync-round-vendor-req-forms
  [round-id]
  (let [{:keys [buyer-id req-form-template] :as round} (-> [[:rounds {:id round-id}
                                                             [:buyer-id
                                                              [:req-form-template
                                                               [:id]]]]]
                                                           ha/sync-query
                                                           :rounds
                                                           first)
        form-template-id (:id req-form-template)
        rps (-> [[:round-product {:round-id round-id}
                  [:id :product-id :deleted
                   [:vendor-response-form-docs
                    [:id :doc-id]]
                   [:product
                    [:vendor-id]]]]]
                ha/sync-query
                :round-product)
        prod-id->exists (->> rps
                             (group-by :product-id)
                             (ut/fmap (partial some
                                               (comp nil? :deleted))))
        to-add (filter (partial sync-round-vendor-req-forms-to-add
                                prod-id->exists)
                       rps)
        to-remove (filter (partial sync-round-vendor-req-forms-to-remove
                                   prod-id->exists)
                          rps)
        added (-> (for [{:keys [id vendor-id product]} to-add]
                    (docs/create-form-from-template {:form-template-id form-template-id
                                                     :from-org-id buyer-id
                                                     :to-org-id (:vendor-id product)
                                                     :subject id
                                                     :title (format "Round Req Form -- round %d / prod %d "
                                                                    round-id
                                                                    vendor-id)}))
                  doall)]
    (doseq [{:keys [id] forms :vendor-response-form-docs :as r} to-remove]
      (docs/update-deleted :round_product id)
      (doseq [{form-id :id doc-id :doc-id} forms]
        (when doc-id
          (docs/update-deleted :docs doc-id))
        (docs/update-deleted :forms form-id)))
    {:to-add to-add
     :added added
     :to-remove to-remove}))


#_ (clojure.pprint/pprint
    (get-auto-pop-data 272814695158
                       "product-profile"))
