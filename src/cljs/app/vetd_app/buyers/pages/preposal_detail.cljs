(ns vetd-app.buyers.pages.preposal-detail
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]))

;; Events
(rf/reg-event-fx
 :b/nav-preposal-detail
 (fn [_ [_ preposal-idstr]]
   {:nav {:path (str "/b/preposals/" preposal-idstr)}}))

(rf/reg-event-fx
 :b/route-preposal-detail
 (fn [{:keys [db]} [_ preposal-idstr]]
   {:db (assoc db
               :page :b/preposal-detail
               :page-params {:preposal-idstr preposal-idstr})
    :analytics/page {:name "Buyers Preposal Detail"
                     :props {:preposal-idstr preposal-idstr}}}))

;; Subscriptions
(rf/reg-sub
 :preposal-idstr
 :<- [:page-params] 
 (fn [{:keys [preposal-idstr]}] preposal-idstr))

;; Components
(defn c-preposal-header-segment
  [{:keys [vendor rounds pname logo] :as product} preposal-v-fn product-v-fn]
  (let [pricing-estimate-value (preposal-v-fn :preposal/pricing-estimate "value" :nval)
        pricing-estimate-unit (preposal-v-fn :preposal/pricing-estimate "unit")
        pricing-estimate-details (preposal-v-fn :preposal/pricing-estimate "details")
        pitch (preposal-v-fn :preposal/pitch)]
    [:> ui/Segment {:class "detail-container"}
     [:h1.product-title
      pname " " [:small " by " (:oname vendor)]]
     [:> ui/Image {:class "product-logo"
                   :src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]
     (when (not-empty rounds)
       [bc/c-round-in-progress {:round-idstr (-> rounds first :idstr)
                                :props {:ribbon "left"}}])
     [bc/c-categories product]
     (when (= "Yes" (product-v-fn :product/free-trial?))
       [bc/c-free-trial-tag])
     [:> ui/Grid {:columns "equal"
                  :style {:margin-top 4}}
      [:> ui/GridRow
       [:> ui/GridColumn {:width 12}
        (or (util/parse-md (product-v-fn :product/description))
            [:p "No description available."])
        [:br]
        [:h3.display-field-key "Preposal Pitch"]
        (util/parse-md pitch)
        [:br]
        [:h3.display-field-key "Preposal Pricing Estimate"]
        (if pricing-estimate-value
          [:<>
           (util/currency-format pricing-estimate-value) " / " pricing-estimate-unit
           (when (not-empty pricing-estimate-details)
             (str " - " pricing-estimate-details))]
          pricing-estimate-details)]
       [:> ui/GridColumn {:width 4}
        (when-let [website-url (product-v-fn :product/website)]
          [:<>
           [bc/c-external-link website-url "Product Website"]
           [:br]
           [:br]])
        (when-let [demo-url (product-v-fn :product/demo)]
          [:<>
           [bc/c-external-link demo-url "Watch Demo Video"]
           [:br]
           [:br]])]]]]))

(defn c-preposal
  "Component to display Preposal details."
  [{:keys [product response-prompts] :as preposal}]
  (let [preposal-v-fn (partial docs/get-value-by-term response-prompts)
        product-v-fn (partial docs/get-value-by-term (-> product
                                                         :form-docs
                                                         first
                                                         :response-prompts))
        c-display-field (bc/requestable
                         (partial bc/c-display-field* {:type :product
                                                       :id (:id product)
                                                       :name (:pname product)}))
        {:keys [vendor]} product]
    [:<>
     [c-preposal-header-segment product preposal-v-fn product-v-fn]
     [bc/c-pricing c-display-field product-v-fn]
     [bc/c-vendor-profile (-> vendor :docs-out first) (:id vendor) (:oname vendor)]
     [bc/c-onboarding c-display-field product-v-fn]
     [bc/c-client-service c-display-field product-v-fn]
     [bc/c-reporting c-display-field product-v-fn]
     [bc/c-market-niche c-display-field product-v-fn]]))

(defn c-page []
  (let [preposal-idstr& (rf/subscribe [:preposal-idstr])
        org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:docs {:dtype "preposal"
                                        :idstr @preposal-idstr&}
                                 [:id :idstr :title
                                  [:product
                                   [:id :pname :logo
                                    [:form-docs {:ftype "product-profile"
                                                 :_order_by {:created :desc}
                                                 :_limit 1
                                                 :doc-deleted nil}
                                     [:id
                                      [:response-prompts {:ref-deleted nil}
                                       [:id :prompt-id :prompt-prompt :prompt-term
                                        [:response-prompt-fields
                                         [:id :prompt-field-fname :idx
                                          :sval :nval :dval]]]]]]
                                    [:rounds {:buyer-id @org-id&}
                                     [:id :idstr :created :status]]
                                    [:categories [:id :idstr :cname]]
                                    [:vendor
                                     [:id :oname :url
                                      [:docs-out {:dtype "vendor-profile"
                                                  :_order_by {:created :desc}
                                                  :_limit 1}
                                       [:id
                                        [:response-prompts {:ref-deleted nil}
                                         [:id :prompt-id :prompt-prompt :prompt-term
                                          [:response-prompt-fields
                                           [:id :prompt-field-fname :idx
                                            :sval :nval :dval]]]]]]]]]]
                                  [:from-org [:id :oname]]
                                  [:from-user [:id :uname]]
                                  [:to-org [:id :oname]]
                                  [:to-user [:id :uname]]
                                  [:response-prompts {:ref-deleted nil}
                                   [:id :prompt-id :prompt-prompt :prompt-term
                                    [:response-prompt-fields
                                     [:id :prompt-field-fname :idx
                                      :sval :nval :dval]]]]]]]}])]
    (fn []
      [:div.container-with-sidebar
       [:div.sidebar
        [:div {:style {:padding "0 15px"}}
         [bc/c-back-button {:on-click #(rf/dispatch [:b/nav-preposals])}
          "All Preposals"]]
        (when-not (= :loading @preps&)
          (let [{:keys [product]} (-> @preps& :docs first)]
            (when (empty? (:rounds product))
              [:> ui/Segment
               [bc/c-start-round-button {:etype :product
                                         :eid (:id product)
                                         :ename (:pname product)
                                         :props {:fluid true}}]
               [bc/c-setup-call-button product (:vendor product)]
               [bc/c-ask-a-question-button product (:vendor product)]])))]
       [:div.inner-container
        (if (= :loading @preps&)
          [cc/c-loader]
          (if-let [preposal (-> @preps& :docs first)]
            [c-preposal preposal]
            "Sorry, that preposal cannot be found."))]])))
