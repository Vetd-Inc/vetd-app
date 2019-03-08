(ns vetd-app.pages.buyers.b-preposal-detail
  (:require [vetd-app.flexer :as flx]
            [vetd-app.ui :as ui]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

;; Events
(rf/reg-event-fx
 :b/nav-preposal-detail
 (fn [_ [_ preposal-id]]
   {:nav {:path (str "/b/preposals/" preposal-id)}}))

(rf/reg-event-db
 :b/route-preposal-detail
 (fn [db [_ preposal-id]]
   (assoc db
          :page :b/preposal-detail
          :page-params {:preposal-id preposal-id})))

;; Subscriptions
(rf/reg-sub
 :preposal-id
 :<- [:page-params] 
 (fn [{:keys [preposal-id]}] preposal-id))

;; Components
(defn c-preposal
  "Component to display preposal details."
  [{:keys [id product from-org responses] :as preposal}]
  (let [pricing-estimate-value (docs/get-field-value responses "Pricing Estimate" "value" :nval)
        pricing-estimate-unit (docs/get-field-value responses "Pricing Estimate" "unit" :sval)
        pricing-model (docs/get-field-value responses "Pricing Model" "value" :sval)
        free-trial? (= "yes" (docs/get-field-value responses "Do you offer a free trial?" "value" :sval))
        free-trial-terms (docs/get-field-value responses "Please describe the terms of your trial" "value" :sval)
        pitch (docs/get-field-value responses "Pitch" "value" :sval)
        employee-count (docs/get-field-value responses "Employee Count" "value" :sval)
        website (docs/get-field-value responses "Website" "value" :sval)]
    [:div.detail-container
     [:> ui/Header {:size "huge"}
      (:pname product) " " [:small " by " (:oname from-org)]]
     [:> ui/Image {:class "product-logo" ; todo: make config var 's3-base-url'
                   :style {:position "absolute"
                           :right 0
                           :top 0
                           :padding 12}
                   :src (str "https://s3.amazonaws.com/vetd-logos/" (:logo product))}]
     (if (not-empty (:rounds product))
       [:> ui/Label {:color "teal"
                     :size "medium"
                     :ribbon "top left"}
        "VetdRound In Progress"]
       [:> ui/Button {:onClick #(rf/dispatch [:b/start-round :product (:id product)])
                      :color "blue"
                      :icon true
                      :labelPosition "right"
                      :style {:marginRight 15}}
        "Start VetdRound"
        [:> ui/Icon {:name "right arrow"}]])
     (for [c (:categories product)]
       ^{:key (:id c)}
       [:> ui/Label {:class "category-tag"}
        (:cname c)])
     (when free-trial? [:> ui/Label {:class "free-trial-tag"
                                     :color "gray"
                                     :size "small"
                                     :tag true}
                        "Free Trial"])
     [:> ui/Grid {:columns "equal"
                  :style {:margin "20px 0 0 0"}}
      [:> ui/GridRow
       [:> ui/GridColumn {:width 10}
        [:> ui/Segment {:style {:padding "40px 20px 10px 20px"}}
         [:> ui/Label {:attached "top"
                       :align "left"}
          "Pitch"]
         pitch]]]
      [:> ui/GridRow
       [:> ui/GridColumn {:width 16}
        [:> ui/Segment {:style {:padding "40px 20px 10px 20px"}}
         [:> ui/Label {:attached "top"}
          "Product Description"]
         (:long-desc product)]]]
      [:> ui/GridRow
       [:> ui/GridColumn {:width 4}
        [:> ui/Segment {:style {:padding "40px 20px 10px 20px"}}
         [:> ui/Label {:attached "top"}
          "Estimated Price"]
         (format/currency-format pricing-estimate-value)
         " / "
         pricing-estimate-unit]]
       (when (not= "" free-trial-terms)
         [:> ui/GridColumn {:width 6}
          [:> ui/Segment {:style {:padding "40px 20px 10px 20px"}}
           [:> ui/Label {:attached "top"}
            "Free Trial Terms"]
           free-trial-terms]])
       (when (not= "" pricing-model)
         [:> ui/GridColumn {:width 6}
          [:> ui/Segment {:style {:padding "40px 20px 10px 20px"}}
           [:> ui/Label {:attached "top"}
            "Pricing Model"]
           pricing-model]])]
      [:> ui/GridRow
       [:> ui/GridColumn
        [:> ui/Segment {:style {:padding "40px 20px 10px 20px"}}
         [:> ui/Label {:attached "top"}
          (str "About " (:oname from-org))]
         (when (not= "" website) ; todo: better design on these fields
           [:span "Website: " website [:br]])
         (when (not= "" employee-count)
           [:span "Number of Employees: " employee-count])]]]]]))

(defn c-page []
  (let [preposal-id& (rf/subscribe [:preposal-id])
        org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:docs {:dtype "preposal"
                                        :id @preposal-id&}
                                 [:id :idstr :title
                                  [:product [:id :pname :logo :short-desc :long-desc
                                             [:rounds {:buyer-id @org-id&
                                                       :status "active"}
                                              [:id :created :status]]
                                             [:categories [:id :idstr :cname]]]]
                                  [:from-org [:id :oname]]
                                  [:from-user [:id :uname]]
                                  [:to-org [:id :oname]]
                                  [:to-user [:id :uname]]
                                  [:responses
                                   [:id :prompt-id :notes
                                    [:prompt
                                     [:id :prompt]]
                                    [:fields
                                     [:id :pf-id :idx :sval :nval :dval
                                      [:prompt-field [:id :fname]]]]]]]]]}])]
    (fn []
      [:div.container-with-sidebar
       [:div.sidebar
        [:> ui/Button {:on-click #(rf/dispatch [:b/nav-preposals])
                       :color "gray"
                       :icon true
                       :labelPosition "left"}
         "All Preposals"
         [:> ui/Icon {:name "left arrow"}]]]
       [:> ui/Segment {:class "inner-container"}
        (if (= :loading @preps&)
          [:> ui/Loader {:active true :inline true}]
          [c-preposal (-> @preps& :docs first)])]])))
