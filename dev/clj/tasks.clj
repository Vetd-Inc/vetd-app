(ns tasks
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.docs :as docs] ))

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

;; top-level doc fields:  doc-title from-org doc-dtype doc-dsubtype

;; response-field fields: response ([{:state X}])

(defn apply-vendor-values-to-prompts
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
         :prompts (keep (partial apply-vendor-values-to-prompts v)
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

#_ (create-vendor-profiles 1)

#_
(clojure.pprint/pprint 
 (get-latest-vendor-profile-form))
