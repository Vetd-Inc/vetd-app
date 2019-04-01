(ns tasks
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha] ))

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
  (db/hs-query
   {:select [:*]
    :from [[:forms :f]]
    :where [:and
            [:= :f.ftype "vendor-profile"]
            [:= :f.deleted nil]]
    :order-by {:f.created :desc}
    :limit 1}))

(defn get-latest-vendor-profile-form []
  (ha/sync-query [[:forms
                   {:ftype "vendor-profile"
                    :deleted nil
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
                        :sort]]]]]]]))

