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
  (insert-round-product round-id product-id)
#_  (docs/create-form-from-template {:form-template-id :###### })
  ;; TODO create form
#_  (create-round-product-form-from-round-init-doc product-id round-id))

(defn sync-round-vendor-req-forms
  [round-id]
  (let [{:keys [buyer-id] :as round} (-> [[:rounds {:id round-id}
                                           [:buyer-id
                                            [:req-form-template 
                                             [:id :ftype :fsubtype
                                              [:prompts
                                               [:id]]]]]]]
                                         ha/sync-query
                                         :rounds
                                         first)
        {:keys [ftype fsubtype prompts] form-template-id :id} (:req-form-template round)
        products (-> [[:rounds {:id round-id}
                       [[:products
                         [:id :vendor-id :ref-deleted
                          [:forms {:ftype ftype
                                   :fsubtype fsubtype
                                   :deleted nil}
                           [:id
                            [:prompts [:id]]]]]]]]]
                     ha/sync-query
                     :rounds
                     first
                     :products)
        to-add (filter (fn [{:keys [forms ref-deleted]}]
                         (and (empty? forms)
                              (nil? ref-deleted)))
                       products)
        to-remove (filter (fn [{:keys [forms ref-deleted]}]
                            (not (or (empty? forms)
                                     (nil? ref-deleted))))
                          products)]
    #_    [round ftype fsubtype prompts form-template-id to-add to-remove]
    (doseq [{:keys [vendor-id id]} to-add]
      (clojure.pprint/pprint #_docs/create-form-from-template {:form-template-id form-template-id
                                                               :from-org-id buyer-id
                                                               :to-org-id vendor-id
                                                               :title (format "Round Req Form -- round %d / prod %d "
                                                                              round-id
                                                                              vendor-id)}))
    (doseq [{:keys [forms id]} to-remove]
      (->> forms first :id
           (docs/update-deleted :round_product)))))

#_ [:products [:id]]

#_ (clojure.pprint/pprint  (sync-round-vendor-req-forms 1000787839305))
