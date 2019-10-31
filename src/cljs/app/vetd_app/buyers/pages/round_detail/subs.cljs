(ns vetd-app.buyers.pages.round-detail.subs
  (:require [re-frame.core :as rf]))

(defn mk-round-detail-gql
  [round-idstr org-id]
  [:gql/sub
   {:queries
    [[:rounds {:idstr round-idstr
               :deleted nil}
      [:id :idstr :created :status :title
       [:buyer
        [:id :oname]]
       ;; requirements (topics) form template
       [:req-form-template
        [:id
         [:prompts {:ref-deleted nil
                    :_order_by {:sort :asc}}
          [:id :idstr :prompt :term :descr :sort]]]]
       ;; round initiation form response
       [:init-doc
        [:id
         [:response-prompts {:ref-deleted nil}
          [:id :prompt-id :prompt-prompt :prompt-term
           [:response-prompt-fields
            [:id :prompt-field-fname :idx
             :sval :nval :dval]]]]]]
       ;; products in the round
       [:round-product {:deleted nil
                        :_order_by {:sort :asc}}
        [:id :result :reason :sort
         [:product
          [:id :idstr :pname
           [:docs {:dtype "preposal"    ; completed preposals
                   :to-org-id org-id}
            [:id :idstr]]
           [:vendor
            [:id :oname]]]]
         ;; requirements (topics) responses from vendors
         [:vendor-response-form-docs
          [:id :title :doc-id :doc-title
           :ftype :fsubtype
           [:doc-from-org [:id :oname]]
           [:doc-to-org [:id :oname]]
           [:response-prompts {:ref-deleted nil}
            [:id :prompt-id :prompt-prompt :prompt-term
             [:response-prompt-fields
              [:id :prompt-field-fname :idx :resp-id
               :sval :nval :dval]]
             [:subject-of-response-prompt
              {:deleted nil
               :prompt-term "round.response/rating"}
              [[:response-prompt-fields
                {:deleted nil}
                [:nval]]]]]]]]]]]]]}])

;; (rf/reg-sub
;;  :b/round.data
;;  (fn [round-idstr org-id]
;;    [(rf/subscribe (mk-round-detail-gql round-idstr org-id))])
;;  (fn [[round]]
;;    (println round)
;;    round))

(rf/reg-sub
 :b/round.buyer-name
 (fn [{:keys [round]}]
   (:buyer-name round)))

(rf/reg-sub
 :b/round.buyer?
 (fn [{:keys [org-id round]}]
   (= org-id (:buyer-id round))))

(rf/reg-sub
 :b/round.complete?
 (fn [{:keys [round]}]
   (= "complete" (:status round))))

(rf/reg-sub
 :b/round.read-only?
 :<- [:b/round.buyer?]
 :<- [:b/round.complete?]
 (fn [[buyer? complete?]]
   (or (not buyer?)
       complete?)))

(def curated-topics-terms
  [ ;; "preposal/pitch"
   "preposal/pricing-estimate"
   "product/cancellation-process"
   "product/case-studies"
   ;; "product/categories"
   "product/clients"
   "product/competitive-differentiator"
   "product/competitors"
   "product/data-security"
   "product/demo"
   ;; "product/description"
   ;; "product/free-trial-terms"
   "product/free-trial?"
   "product/ideal-client"
   "product/integrations"
   "product/kpis"
   ;; "product/logo"
   "product/meeting-frequency"
   "product/minimum-contract"
   "product/num-clients"
   "product/onboarding-estimated-time"
   "product/onboarding-process"
   "product/onboarding-team-involvement"
   "product/payment-options"
   "product/point-of-contact"
   "product/price-range"
   "product/pricing-model"
   "product/reporting"
   "product/roadmap"
   "product/tagline"
   ;; "product/website"
   "vendor/employee-count"
   "vendor/funding"
   "vendor/headquarters"
   ;; "vendor/logo"
   ;; "vendor/website"
   "vendor/year-founded"])

;; TODO use this pattern elsewhere in app
(def topics-gql
  [:gql/q
   {:queries
    [[:prompts {:term curated-topics-terms
                :deleted nil
                :_limit 500              ;; sanity check
                :_order_by {:term :asc}} ;; a little easier to read
      [:id :prompt :term]]]}])

(rf/reg-sub
 :b/topics.loading?
 :<- topics-gql
 (fn [x] (= x :loading)))

(rf/reg-sub
 :b/topics.data
 :<- topics-gql
 (fn [x]
   (if-not (= x :loading)
     (:prompts x)
     [])))

(rf/reg-sub
 :b/topics.data-as-dropdown-options
 :<- [:b/topics.data]
 (fn [data]
   (map #(hash-map :key (:term %)
                   :text (:prompt %)
                   :value (:term %))
        data)))
