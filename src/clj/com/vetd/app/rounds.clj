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
  (let [{:keys [ftype fsubtype prompts] :as req-ft} (-> [[:rounds {:id round-id}
                                                          [[:req-form-template 
                                                            [:id :ftype :fsubtype
                                                             [:prompts
                                                              [:id]]]]]]]
                                                        ha/sync-query
                                                        :rounds
                                                        first
                                                        :req-form-template)
        products (-> [[:rounds {:id round-id}
                                                [[:products
                                                  [:id
                                                   [:forms {:ftype ftype
                                                            :fsubtype fsubtype
                                                            :deleted nil}
                                                    [[:prompts [:id]]]]]]]]]
                                              ha/sync-query
                                              :rounds
                                              first
                                              :products)]
    [req-ft products]))

#_ [:products [:id]]

#_ (clojure.pprint/pprint  (sync-round-vendor-req-forms 1000787839305))




















































