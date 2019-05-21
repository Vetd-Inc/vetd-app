(ns com.vetd.app.vendors
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.docs :as docs]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [clj-http.client :as http]
            [mikera.image.core :as mimg]
            [image-resizer.resize :as rzimg]))



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
  [{:keys [id pname ]}]
  (db/hs-exe! {:update :products
               :set {:updated (ut/now-ts)
                     :pname pname}
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
  ;; TODO fix this
  #_(replace-product-categories product))

(defmethod com/handle-ws-inbound :v/delete-product
  [{:keys [product-id]} ws-id sub-fn]
  (delete-product product-id))

1216149702684

(defmethod docs/handle-doc-update :product-profile
  [{:keys [id]} & _]
  (when-let [logo (some-> [[:docs {:id id #_1216149702684}  ;; TODO replace id
                            [[:response-prompts {:prompt-term "product/logo"}
                              [[:fields [:sval]]]]]]]
                          ha/sync-query
                          :docs
                          first
                          :response-prompts
                          first
                          :fields
                          first
                          :sval)]
    (def logo1 logo)))


(http )
