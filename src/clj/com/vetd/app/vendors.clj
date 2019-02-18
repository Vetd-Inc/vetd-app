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

{:doc-title nil,
  :from-user {:id 420325126261, :uname "Bill Piel"},
  :prompts
  [{:id 370382503629,
    :prompt "Pricing Estimate",
    :descr
    "In what range would you expect this buyer's costs to fall?",
    :fields
    [{:id 370382503630,
      :fname "value",
      :ftype "n",
      :fsubtype "int",
      :list? false,
      :response {:state "default value????"}}
     {:id 370382503632,
      :fname "unit",
      :ftype "e-price-per",
      :fsubtype nil,
      :list? false,
      :response {:state "default value????"}}],
    :response {:note-state ""}}
   {:id 370382503633,
    :prompt "Pitch",
    :descr "Why do we believe you are a fit for this product?",
    :fields
    [{:id 370382503634,
      :fname "value",
      :ftype "s",
      :fsubtype "multi",
      :list? false,
      :response {:state "default value????"}}],
    :response {:note-state ""}}],
  :title "Preposal Request 54794",
  :from-org {:id 273818389861, :oname "Vetd"},
  :product {:id 272814707844, :pname "Stride"},
  :id 420327446264, 
  :responses [], 
  :doc-id nil}
