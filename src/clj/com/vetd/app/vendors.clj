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

(defn insert-category
  [category-name]
  (let [[id idstr] (ut/mk-id&str)]
    (->> {:id id
          :idstr idstr
          :created (ut/now-ts)
          :updated (ut/now-ts)
          :deleted nil
          :cname category-name}
         (db/insert! :categories)
         first)))


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
  (let [current (->> [[:products {:id id}
                       [[:categories [:id]]]]]
                     ha/sync-query
                     :products
                     first
                     :categories
                     (map :id)
                     set)
        [del add] (->> categories
                       set
                       (clojure.data/diff current))]
    (when-not (empty? del)
      (db/update-deleted-where :product_categories
                               [:and
                                [:= :prod_id id]
                                [:in :cat_id (vec del)]]))  
    (doseq [c add]
      (insert-product-category id c))))

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

(defmethod com/handle-ws-inbound :v/remove-prompt-from-form
  [{:keys [prompt-id form-id]} ws-id sub-fn]
  (docs/delete-form-prompt-by-ids prompt-id form-id))

(defmethod com/handle-ws-inbound :v/remove-response-from-doc
  [{:keys [response-id doc-id]} ws-id sub-fn]
  (docs/delete-doc-response-by-ids response-id doc-id))

(defmethod com/handle-ws-inbound :v/propagate-prompt
  [{:keys [form-prompt-ref-id target-form-id]} ws-id sub-fn]
  (docs/propagate-prompt form-prompt-ref-id target-form-id))

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
    (if logo-url ;; TODO only do this if logo-url changed
      (when (.startsWith logo-url "http")
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
          (log/info (format "Product logo processed: '%s' '%s'" new-file-name subject))))
      (com/log-error  (format "NO Product logo found in profile doc: '%s'" subject)))))


(defn process-product-categories [prod-profile-doc-id]
  (let [{:keys [subject] :as doc} (-> [[:docs {:id prod-profile-doc-id}
                                        [:subject
                                         [:response-prompts {:prompt-term "product/categories"
                                                             :ref-deleted nil
                                                             :deleted nil}
                                          [[:fields [:jval]]]]]]]
                                      ha/sync-query
                                      :docs
                                      first)
        cats (some->> doc
                      :response-prompts
                      first
                      :fields
                      (map :jval))]
    (when subject
      (let [cats' (map (fn [{:keys [id text]}]
                         (or id
                             (-> text
                                  insert-category
                                  :id)))
                       cats)]
        (replace-product-categories {:id subject
                                     :categories cats'})))))


(defn resp-field-empty? [{:keys [nval sval dval jval]}]
  (and (empty? sval)
       (nil? nval)
       (nil? dval)
       (nil? jval)))

(defn calc-product-profile-score-by-doc-id
  [product-profile-doc-id]
  (let [[head :as fields] (db/hs-query
                           {:select [[:prompt_id :prompt-id]
                                     [:prompt_term :prompt-term]
                                     [:prompt_field_fname :prompt-field-fname]
                                     [:resp_field_nval :nval]
                                     [:resp_field_sval :sval]
                                     [:resp_field_dval :dval]
                                     [:resp_field_jval :jval]]
                            :from [:docs_to_fields]
                            :where [:= :doc_id product-profile-doc-id]})
        {:keys [product-id]} head]
    (if (->> fields
             (remove resp-field-empty?)
             empty?)
      0.0
      1.0)))

(defn update-product-profile-score
  [product-id product-profile-doc-id]
  (db/update-any! {:id product-id
                   :profile_score
                   (if product-profile-doc-id
                     (calc-product-profile-score-by-doc-id product-profile-doc-id)
                     0.0)
                   :profile_score_updated (ut/now-ts)}
                  :products))

(defn select-products-to-update-profile-score [limit]
  (db/hs-query
   {:select [[:p.id :product-id]
             [:%max.dtf.doc_id :doc-id]]
    :from [[:products :p]]
    :left-join [[:docs_to_fields :dtf]
                [:and
                 [:= :dtf.doc_subject :p.id]
                 [:= :dtf.doc_dtype "product-profile"]]]
    :where [:or
            [:= :p.profile_score nil]
            [:< :p.profile_score_updated :dtf.doc_updated]
            [:< :p.profile_score_updated :dtf.response_updated]
            [:< :p.profile_score_updated :dtf.resp_field_updated]]
    :group-by [:p.id]
    :limit limit}))

(defn random-select-products-to-update-profile-score
  [& [n]]
  (some-> (or n 10)
          select-products-to-update-profile-score
          not-empty
          shuffle
          first))

(defn random-update-product-profile-score [& [n]]
  (if-let [{:keys [product-id doc-id]}
           (-> (or n 10)
               random-select-products-to-update-profile-score
               not-empty)]
    (first (update-product-profile-score product-id doc-id))
    0))

(defn update-all-missing-product-profile-scores* [a b]
  (->> (range a)
       (pmap (fn [_]
               (random-update-product-profile-score b)))
       (reduce + 0)))

;; TODO?? This is crazy slow and could definitely be improved, but... doesn't matter??
(defn update-all-missing-product-profile-scores []
  (try
    (loop [done 0
           n (update-all-missing-product-profile-scores* 100 200)]
      (log/info (format "Updated %d missing product profile scores so far..." done))
      (if (zero? n)
        done
        (let [n' (try (update-all-missing-product-profile-scores* 100 200)
                      (catch Throwable e
                        (Thread/sleep 10000)
                        (update-all-missing-product-profile-scores* 10 20)))]
          (recur (+ done n) n'))))
    (catch Throwable e
      (com/log-error e))))

#_(update-all-missing-product-profile-scores)

(defmethod docs/handle-doc-update :product-profile
  [{:keys [id subject]} & _]
  (try
    (process-product-categories id)        
    (catch Throwable e
      (com/log-error e)))
  (try
    (process-product-logo id) ;; TODO only if logo url changed???
    (catch Throwable e
      (com/log-error e)))
  (try
    (update-product-profile-score subject id)    
    (catch Throwable e
      (com/log-error e))))

(defmethod docs/handle-doc-creation :product-profile
  [{:keys [id subject]} & _]
  (try
    (process-product-categories id)        
    (catch Throwable e
      (com/log-error e)))
  (try
    (process-product-logo id) ;; TODO only if logo url changed???
    (catch Throwable e
      (com/log-error e)))
  (try
    (update-product-profile-score subject id)    
    (catch Throwable e
      (com/log-error e))))
