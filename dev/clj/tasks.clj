(ns tasks
  (:require [com.vetd.app.db :as db]))

;; - count vendors/products without profiles
;; - limited select vendors/products without profiles
;; - find profile form
;; - create doc&response from profile

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
          [:= :d.from_org_id nil]
          [:= :o.deleted nil]]})


(db/hs-query
 {:select [:*]
  :from [[:forms :f]]
  :where [:and
          [:= :f.ftype "vendor-profile"]
          [:= :f.deleted nil]]
  :order-by {:f.created :desc}
  :limit 1})





























































