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
      ;; TODO call sync-round-vendor-req-forms too, once we're ready  
  (insert-round-product round-id product-id))

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
        rps (-> [[[:round-product {:id round-id}
                   [:id :product-id :deleted
                    [:vendor-response-form-docs
                     [:id]]]]]]
                ha/sync-query
                :round-product)
        prod-id->exists (->> rps
                             (group-by :product-id)
                             (ut/fmap (partial some
                                               (comp nil? :deleted))))
        to-add (filter (fn [{:keys [product-id] forms :vendor-response-form-docs}]
                         (and (empty? forms)
                              (prod-id->exists product-id)))
                       rps)
        to-remove (filter (fn [{:keys [product-id] forms :vendor-response-form-docs}]
                            (not (or (empty? forms)
                                     (prod-id->exists id))))
                          rps)]
    (doseq [{:keys [vendor-id id ref-id]} to-add]
      (docs/create-form-from-template {:form-template-id form-template-id
                                       :from-org-id buyer-id
                                       :to-org-id vendor-id
                                       :subject ref-id
                                       :title (format "Round Req Form -- round %d / prod %d "
                                                      round-id
                                                      vendor-id)}))
    (doseq [{:keys [forms id]} to-remove]
      (->> forms first :id
           (docs/update-deleted :round_product)))))

