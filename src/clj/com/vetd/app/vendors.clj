(ns com.vetd.app.vendors
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
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

(defmethod com/handle-ws-inbound :v/create-preposal
  [req ws-id sub-fn]
  (create-preposal req))

(defmethod com/handle-ws-inbound :save-profile
  [{:keys [vendor-id long-desc]} ws-id sub-fn]
#_  (create-profile vendor-id long-desc))
