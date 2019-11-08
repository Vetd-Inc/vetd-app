(ns vetd-app.buyers.pages.round-detail.subs
  (:require [re-frame.core :as rf]))

(defn mk-round-detail-gql
  [round-idstr org-id]
  [:gql/sub
   {:queries
    [[:rounds {:idstr round-idstr
               :deleted nil}
      [:id :idstr :created :status :title :initiation-form-prefill
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
