(ns com.vetd.app.vendors
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.docs :as docs]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [clj-http.client :as http]
            [mikera.image.core :as mimg]
            [image-resizer.resize :as rzimg]
            [image-resizer.pad :as pdimg]
            [image-resizer.crop :as crimg]))




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




(defn process-product-logo [prod-profile-doc-id]
  (let [{:keys [subject] :as doc} (-> [[:docs {:id prod-profile-doc-id}
                                        [:subject
                                         [:response-prompts {:prompt-term "product/logo"
                                                             :ref-deleted nil
                                                             :deleted nil}
                                          [[:fields [:sval]]]]]]]
                                      ha/sync-query
                                      :docs
                                      first)
        logo-url (some-> doc
                         :response-prompts
                         first
                         :fields
                         first
                         :sval)]
    (if logo-url
      (let [baos (java.io.ByteArrayOutputStream.)
            _ (ut/$- -> logo-url
                     (http/get {:as :stream})
                     :body
                     mimg/load-image
                     ((rzimg/resize-fn 150 150 image-resizer.scale-methods/automatic))
                     (mimg/write baos "png"))
            ba (.toByteArray baos)
            new-file-name (format "%s.png"
                                  (com/md5-hex ba))]
        (com/s3-put "vetd-logos" new-file-name ba)
        (db/update-any! {:id subject
                         :logo new-file-name}
                        :products)
        (log/info (format "Product logo processed: '%s' '%s'" new-file-name subject)))
      (log/error  (format "NO Product logo found in profile doc: '%s'" subject)))))

(defn process-product-categories [prod-profile-doc-id]
  (let [{:keys [subject] :as doc} (-> [[:docs {:id prod-profile-doc-id}
                                        [:subject
                                         [:response-prompts {:prompt-term "product/categories"
                                                             :ref-deleted nil
                                                             :deleted nil}
                                          [[:fields [:sval]]]]]]]
                                      ha/sync-query
                                      :docs
                                      first)
        cats (some->> doc
                          :response-prompts
                          first
                          :fields
                          (map :sval))]
    (if-not (empty? cats)
      1
      2)))

(defmethod docs/handle-doc-update :product-profile
  [{:keys [id]} & _]
  (try
    (process-product-logo id) ;; TODO only if logo url changed???
    (catch Exception e
      (log/error e))))

(defmethod docs/handle-doc-creation :product-profile
  [{:keys [id]} & _]
  (try
    (process-product-logo id)
    (catch Exception e
      (log/error e))))
