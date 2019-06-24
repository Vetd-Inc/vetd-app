(ns tasks
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.docs :as docs]
            [com.vetd.app.util :as ut]))

;; - count vendors/products without profiles
;; - limited select vendors/products without profiles
;; - find profile form
;; - create doc&response from profile


(defn count-vendors-without-profile []
  (db/hs-query
   {:select [:%count.*]
    :from [[:orgs :o]]
    :left-join [[:docs :d]
                [:and
                 [:= :d.from_org_id :o.id]
                 [:= :d.dtype "vendor-profile"]
                 [:= :d.deleted nil]]]
    :where [:and
            [:= :o.vendor_qm true]
            [:= :d.id nil]
            [:= :o.deleted nil]]}))

(defn count-products-without-profile []
  (db/hs-query
   {:select [:%count.*]
    :from [[:products :p]]
    :left-join [[:docs :d]
                [:and
                 [:= :d.subject :p.id]
                 [:= :d.dtype "product-profile"]
                 [:= :d.deleted nil]]]
    :where [:and
            [:= :d.id nil]
            [:= :p.deleted nil]]}))

#_(count-products-without-profile)

(defn get-vendors-without-profile-with-limit [limit]
  (db/hs-query
   {:select [:o.*]
    :from [[:orgs :o]]
    :left-join [[:docs :d]
                [:and
                 [:= :d.from_org_id :o.id]
                 [:= :d.dtype "vendor-profile"]
                 [:= :d.deleted nil]]]
    :where [:and
            [:= :o.vendor_qm true]
            [:= :d.id nil]
            [:= :o.deleted nil]]
    :limit limit}))

(defn get-products-without-profile-with-limit [limit]
  (db/hs-query
   {:select [:p.*]
    :from [[:products :p]]
    :left-join [[:docs :d]
                [:and
                 [:= :d.subject :p.id]
                 [:= :d.dtype "product-profile"]
                 [:= :d.deleted nil]]]
    :where [:and
            [:= :d.id nil]
            [:= :p.deleted nil]]
    :limit limit}))

(defn get-latest-vendor-profile-form []
  (-> [[:forms
        {:ftype "vendor-profile"
         :deleted nil
         :_order_by {:created :desc}
         :_limit 1}
        [:id
         :title
         :ftype
         :fsubtype
         [:prompts {:deleted nil}
          [:id 
           :prompt
           :sort
           [:fields {:deleted nil}
            [:id 
             :fname
             :ftype
             :fsubtype
             :list?
             :sort]]]]]]]
      ha/sync-query
      :forms
      first))

(defn get-latest-product-profile-form []
  (-> [[:forms
        {:ftype "product-profile"
         :deleted nil
         :_order_by {:created :desc}
         :_limit 1}
        [:id
         :title
         :ftype
         :fsubtype
         [:prompts {:deleted nil}
          [:id 
           :prompt
           :sort
           [:fields {:deleted nil}
            [:id 
             :fname
             :ftype
             :fsubtype
             :list?
             :sort]]]]]]]
      ha/sync-query
      :forms
      first))

;; top-level doc fields:  doc-title from-org doc-dtype doc-dsubtype

;; response-field fields: response ([{:state X}])

(defn apply-values-to-prompts
  [v {:keys [prompt fields] :as p}]
  (when-let [resp-fields (v prompt)]
    (assoc p
           :fields
           (mapv (fn [{:keys [fname] :as f}]
                   (if-let [x (resp-fields fname)]
                     (assoc f
                            :response [{:state x}])
                     f))
                 fields))))

(defn apply-vendor-values-to-form
  [{:keys [id oname] :as v} {:keys [prompts ftype fsubtype] :as form}]
  (assoc form
         :doc-title (format "Vendor Profile for %s" oname)
         :to-org {:id id}
         :doc-dtype ftype
         :doc-dsubtype fsubtype
         :prompts (keep (partial apply-values-to-prompts v)
                        prompts)))

(defn mk-vendor-values-from-db-rec
  [{:keys [oname id url]}]
  {"Website" {"value" url}
   :id id
   :oname oname})

(defn create-vendor-profiles [n]
  (let [form (get-latest-vendor-profile-form)]
    (doseq [v (get-vendors-without-profile-with-limit n)]
      (try
        (println (:id v) (:oname v))
        (-> v
            mk-vendor-values-from-db-rec
            (apply-vendor-values-to-form form)
            docs/create-doc-from-form-doc)
        (catch Throwable t
          (clojure.pprint/pprint t))))
    :done))

#_ (create-vendor-profiles 20000)

(defn apply-product-values-to-form
  [{:keys [pname product] :as v} {:keys [prompts ftype fsubtype] :as form}]
  (assoc form
         :doc-title (format "Product Profile for %s" pname)
         :doc-dtype ftype
         :doc-dsubtype fsubtype
         :product product
         :prompts (keep (partial apply-values-to-prompts v)
                        prompts)))

(defn mk-product-values-from-db-rec
  [{:keys [pname id long_desc logo url]}]
  {:id id
   :pname pname
   :product {:id id}
   "Describe your product or service" {"value" long_desc}
   "Product Logo" {"value" logo}
   "Product Website" {"value" url}})

(defn create-product-profiles [n]
  (let [form (get-latest-product-profile-form)]
    (doseq [v (get-products-without-profile-with-limit n)]
      (try
        (println (:id v) (:pname v))
        (-> v
            mk-product-values-from-db-rec
            (apply-product-values-to-form form)
            docs/create-doc-from-form-doc)
        (catch Throwable t
          (clojure.pprint/pprint t))))
    :done))

#_ (create-product-profiles 50000)


#_
(clojure.pprint/pprint 
 (get-latest-vendor-profile-form))

(def last-id--convert-ids->base31& (atom 0))
(def tables--convert-ids->base31& (atom []))
(def last-table--convert-ids->base31& (atom nil))


(defn convert-ids->base31
  []
  (when (empty? @tables--convert-ids->base31&)
    (do (reset! tables--convert-ids->base31&
                (->> (db/select-all-table-names-MZ "vetd")
                     (mapv keyword)
                     sort))
        (reset! last-table--convert-ids->base31& nil)
        (reset! last-id--convert-ids->base31& 0)))
  (loop [[table-kw & tail] @tables--convert-ids->base31&
         last-id @last-id--convert-ids->base31&]
    (if table-kw
      (let [ids (->> (db/hs-query {:select [:id]
                                   :from [table-kw]
                                   :where [:> :id last-id]})
                     (map :id))]
        (println (java.util.Date.))
        (println table-kw)
        (println (first ids))
        (try
          (let [c (volatile! 0)]
            (doseq [id ids]
              (db/hs-exe! {:update table-kw
                           :set {:idstr (ut/base31->str id)}
                           :where [:= :id id]})
              (reset! last-id--convert-ids->base31& id)
              (when (zero? (mod (vswap! c inc) 100))
                (println (java.util.Date.))
                (println id))))
          (reset! last-table--convert-ids->base31& table-kw)
          (catch Throwable t
            (clojure.pprint/pprint t)))
        (recur tail 0))
      (do
        (println (java.util.Date.))      
        (println "DONE")))))

#_ (convert-ids->base31)

;; get up and running
#_ (do (create-vendor-profiles 20000)
       (create-product-profiles 50000)
       (convert-ids->base31))
