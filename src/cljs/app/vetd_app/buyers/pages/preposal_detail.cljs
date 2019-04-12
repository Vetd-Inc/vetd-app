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
(defn c-preposal
  "Component to display Preposal details."
  [{:keys [id product from-org responses] :as preposal}]
  (let [preposal-pricing-estimate-value (docs/get-field-value responses
                                                              "Pricing Estimate"
                                                              "value"
                                                              :nval)
        preposal-pricing-estimate-unit (docs/get-field-value responses
                                                             "Pricing Estimate"
                                                             "unit"
                                                             :sval)
        preposal-pricing-estimate-details (docs/get-field-value responses
                                                                "Pricing Estimate"
                                                                "details"
                                                                :sval)
        preposal-pitch (docs/get-field-value responses "Pitch" "value" :sval)
        product-profile-responses (-> product :form-docs first :responses)
        v (fn [prompt & [field value]]
            (docs/get-field-value product-profile-responses prompt (or field "value") (or value :sval)))
        vendor (:vendor product)
        rounds (:rounds product)
        pname (:pname product)
        logo (:logo product)]
    [:<>
     [:> ui/Segment {:class "detail-container"}
      [:h1.product-title
       pname " " [:small " by " (:oname vendor)]]
      [:> ui/Image {:class "product-logo"
                    :src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]
      (if (not-empty (:rounds product))
        [bc/c-round-in-progress {:props {:ribbon "left"}}])
      [bc/c-categories product]
      (when (= "Yes" (v "Do you offer a free trial?"))
        [bc/c-free-trial-tag])
      [:> ui/Grid {:columns "equal"
                   :style {:margin-top 0}}
       [:> ui/GridRow
        [bc/c-display-field {:width 11} "Description"
         [:<> (or (v "Describe your product or service") "No description available.")
          [:br] ; TODO this is hacky, and causes a console warning
          [:br]
          [:h3.display-field-key "Pitch"]
          [:p preposal-pitch]]]
        [:> ui/GridColumn {:width 5}
         [:> ui/Grid {:columns "equal"
                      :style {:margin-top 0}}
          [:> ui/GridRow
           (when (bc/has-data? (v "Product Website"))
             [bc/c-display-field {:width 16} "Website"
              [:a {:href (str (when-not (.startsWith (v "Product Website") "http") "http://") (v "Product Website"))
                   :target "_blank"}
               [:> ui/Icon {:name "external square"
                            :color "blue"}]
               "Visit Product Website"]])]
          [:> ui/GridRow
           (when (bc/has-data? (v "Product Demo"))
             [bc/c-display-field {:width 16} "Demo"
              [:a {:href (v "Product Demo")
                   :target "_blank"}
               [:> ui/Icon {:name "external square"
                            :color "blue"}]
               "Watch Video"]])]]]]]]
     [bc/c-pricing product v
      :preposal-estimate (if preposal-pricing-estimate-value
                           [:<> (util/currency-format preposal-pricing-estimate-value) " / " preposal-pricing-estimate-unit
                            (when (and preposal-pricing-estimate-details
                                       (not= "" preposal-pricing-estimate-details))
                              (str " - " preposal-pricing-estimate-details))]
                           preposal-pricing-estimate-details)]
     [bc/c-onboarding product v]
     [bc/c-client-service product v]
     [bc/c-reporting product v]
     [bc/c-market-niche product v]]))

(defn c-page []
  (let [preposal-idstr& (rf/subscribe [:preposal-idstr])
        org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:docs {:dtype "preposal"
                                        :idstr @preposal-idstr&}
                                 [:id :idstr :title
                                  [:product [:id :pname :logo
                                             [:form-docs {:ftype "product-profile"
                                                          :_order_by {:created :desc}
                                                          :_limit 1}
                                              [:id 
                                               [:responses
                                                [:id :prompt-id :notes
                                                 [:prompt
                                                  [:id :prompt]]
                                                 [:fields
                                                  [:id :pf-id :idx :sval :nval :dval
                                                   [:prompt-field [:id :fname]]]]]]]]
                                             [:rounds {:buyer-id @org-id&}
                                              [:id :created :status]]
                                             [:categories [:id :idstr :cname]]
                                             [:vendor
                                              [:id :oname :url
                                               [:docs-out {:dtype "vendor-profile"
                                                           :_order_by {:created :desc}
                                                           :_limit 1}
                                                [:id 
                                                 [:responses {:ref-deleted nil}
                                                  [:id :prompt-id :notes
                                                   [:prompt
                                                    [:id :prompt]]
                                                   [:fields
                                                    [:id :pf-id :idx :sval :nval :dval
                                                     [:prompt-field [:id :fname]]]]]]]]]]]]
                                  [:from-org [:id :oname]]
                                  [:from-user [:id :uname]]
                                  [:to-org [:id :oname]]
                                  [:to-user [:id :uname]]
                                  [:responses {:ref-deleted nil}
                                   [:id :prompt-id :notes
                                    [:prompt
                                     [:id :prompt]]
                                    [:fields {:deleted nil}
                                     [:id :pf-id :idx :sval :nval :dval
                                      [:prompt-field [:id :fname]]]]]]]]]}])]
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
          (let [preposal (-> @preps& :docs first)
                {:keys [docs-out id oname]} (-> preposal :product :vendor)]
            [:<>
             [c-preposal preposal]
             [bc/c-vendor-profile (first docs-out) id oname]]))]])))
