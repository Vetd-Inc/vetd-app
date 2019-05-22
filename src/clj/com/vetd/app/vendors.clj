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
  (when-let [logo-url "http://crosspixel.net/wp-content/uploads/2018/05/CrossPixel-logo-notagline-300x68.png"
             #_(some-> [[:docs {:id prod-profile-doc-id #_1216149702684}
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
    (let [baos (java.io.ByteArrayOutputStream.)
          _ (ut/$- -> logo-url
                     (http/get {:as :stream})
                     :body
                     mimg/load-image
                     ((rzimg/resize-fn 150 150 image-resizer.scale-methods/automatic))
                     (mimg/write baos "png")
                     #_(com/s3-put "vetd-logos"
                                   new-file-name))
          ba (.toByteArray baos)
          new-file-name (format "%s.png"
                                (com/md5-hex ba))]
      (com/s3-put "vetd-logos" new-file-name ba)
      (println new-file-name))))

#_(process-product-logo 1216149702684)

(defmethod docs/handle-doc-update :product-profile
  [{:keys [id]} & _]
  (try
    (process-product-logo id)
    (catch Exception e
      (log/error e))))

#_(

   (def resp1 (http/get "http://crosspixel.net/wp-content/uploads/2018/05/CrossPixel-logo-notagline-300x68.png"
                        {:as :stream}))

   (def img1 (-> resp1 :body slurp))


   (def bi1
     (mimg/load-image (:body (http/get "http://crosspixel.net/wp-content/uploads/2018/05/CrossPixel-logo-notagline-300x68.png"
                                       {:as :stream}))))

   (def img1' (mimg/resize bi1 150))

   (def ni1 (mimg/new-image 150 150))

   (mimg/height img1')



   (let [rz ((rzimg/resize-fn 150 150 image-resizer.scale-methods/automatic) bi1)
         h (mimg/height rz)
         w (mimg/width rz)
         x (/ (- 75 w) 2)
         y (/ (- 75 h) 2)]
     (println [w h x y])
     (mimg/write rz "/opt/code/test1.png" "png"))


   (-> bi1
       ((rzimg/resize-fn 150 150 image-resizer.scale-methods/automatic))
       ((pdimg/pad-fn 150))
       ((crimg/crop-width-fn 150))
       (mimg/write "/opt/code/test1.png" "png"))


   (mimg/fill! ni1  0x000000)



   (mimg/write img1' "/opt/code/test1.png" "png")
   )

