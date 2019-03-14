(ns com.vetd.app.vendors
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.docs :as docs]
            [taoensso.timbre :as log]))

(defn create-preposal
  [prep-req-id txt]
  #_(let [[{:keys [buyer-id vendor-id]}]
        (db/hs-query {:select [:buyer-id :vendor-id]
                      :from [:preposal_reqs]
                      :where [:= :id prep-req-id]})]
    (db/insert! :preposals
                {:id (ut/uuid-str)
                 :buyer-id buyer-id
                 :vendor-id vendor-id
                 :created (ut/now)
                 :pitch txt})))

(defn create-preposal
  [{:keys [buyer-org-id buyer-user-id pitch price-val price-unit]}]
  #_(let [[{:keys [buyer-id vendor-id]}]
        (db/hs-query {:select [:buyer-id :vendor-id]
                      :from [:preposal_reqs]
                      :where [:= :id prep-req-id]})]
    (db/insert! :preposals
                {:id (ut/uuid-str)
                 :buyer-id buyer-id
                 :vendor-id vendor-id
                 :created (ut/now)
                 :pitch txt})))

;; TODO <=========================================
#_(defn create-profile
  [vendor-id long-desc]
  (let [[{:keys [buyer-id vendor-id]}]
        (db/hs-query {:select [:buyer-id :vendor-id]
                      :from [:preposal_reqs]
                      :where [:= :id prep-req-id]})]
    (db/insert! :preposals
                {:id (ut/uuid-str)
                 :buyer-id buyer-id
                 :vendor-id vendor-id
                 :created (ut/now)
                 :pitch txt})))

(defn insert-product
  [{:keys [vendor-id pname short-desc long-desc logo url]}]
  (let [[id idstr] (ut/mk-id&str)]
    (db/insert! :products
                {:id id
                 :idstr idstr
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :vendor_id vendor-id
                 :pname pname
                 :short_desc short-desc
                 :long_desc long-desc
                 :logo logo
                 :url url})))

(defn update-product
  [{:keys [id pname short-desc long-desc logo url]}]
  (db/hs-exe! {:update :products
               :set {:updated (ut/now-ts)
                     :pname pname
                     :short_desc short-desc
                     :long_desc long-desc
                     :logo logo
                     :url url}
               :where [:= :id id]}))

(defn insert-product-category
  [prod_id cat_id]
  (let [[id idstr] (ut/mk-id&str)]
    (db/insert! :product_categories
                {:id id
                 :idstr idstr
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :prod_id prod_id
                 :cat_id cat_id})))

(defn replace-product-categories
  [{:keys [id categories]}]
  (db/hs-exe! {:delete-from :product_categories
               :where [:= :prod_id id]})  
  (doseq [c categories]
    (insert-product-category id c)))

(defn delete-product
  [prod-id]
  (db/hs-exe! {:update :products
               :set {:deleted (ut/now-ts)}
               :where [:= :id prod-id]}))

(defmethod com/handle-ws-inbound :v/create-preposal
  [req ws-id sub-fn]
  (create-preposal req))

(defmethod com/handle-ws-inbound :v/save-profile
  [{:keys [vendor-id long-desc]} ws-id sub-fn]
#_  (create-profile vendor-id long-desc))

(defmethod com/handle-ws-inbound :save-form-doc
  [{:keys [form-doc]} ws-id sub-fn]
  (if-let [doc-id (:doc-id form-doc)]
    (docs/update-doc-from-form-doc form-doc)
    (docs/create-doc-from-form-doc form-doc)))

(defmethod com/handle-ws-inbound :v/new-product
  [{:keys [vendor-id]} ws-id sub-fn]
  (insert-product {:vendor-id vendor-id
                   :pname (str "PRODUCT " (ut/now))
                   :short-desc "Short Description"
                   :long-desc "Long Description"
                   :logo "logo"
                   :url "url"}))

(defmethod com/handle-ws-inbound :v/save-product
  [{:keys [product]} ws-id sub-fn]
  (update-product product)
  (replace-product-categories product))

(defmethod com/handle-ws-inbound :v/delete-product
  [{:keys [product-id]} ws-id sub-fn]
  (delete-product product-id))
