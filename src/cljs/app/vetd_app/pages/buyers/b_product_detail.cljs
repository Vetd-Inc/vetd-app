(ns vetd-app.pages.buyers.b-product-detail
  (:require [vetd-app.flexer :as flx]
            [vetd-app.ui :as ui]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

;; Events
(rf/reg-event-fx
 :b/nav-product-detail
 (fn [_ [_ product-idstr]]
   {:nav {:path (str "/b/products/" product-idstr)}}))

(rf/reg-event-db
 :b/route-product-detail
 (fn [db [_ product-idstr]]
   (assoc db
          :page :b/product-detail
          :page-params {:product-idstr product-idstr})))

;; Subscriptions
(rf/reg-sub
 :product-idstr
 :<- [:page-params] 
 (fn [{:keys [product-idstr]}] product-idstr))

;; Components
(defn c-rounds
  "Given a product map, display the Round data."
  [product]
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
     [:> ui/Icon {:name "right arrow"}]]))

(defn c-categories
  "Given a product map, display the categories as tags."
  [product]
  [:<>
   (for [c (:categories product)]
     ^{:key (:id c)}
     [:> ui/Label {:class "category-tag"}
      (:cname c)])])

(defn c-display-field
  [props field-key field-value] 
  [:> ui/GridColumn props
   [:> ui/Segment {:style {:padding "40px 20px 10px 20px"}}
    [:> ui/Label {:attached "top"} field-key]
    field-value]])

(defn c-product
  "Component to display Preposal details."
  [{:keys [id pname logo long-desc vendor forms rounds categories] :as product}]
  (let [requested-preposal? (not-empty forms)]
    [:div.detail-container
     [:> ui/Header {:size "huge"}
      pname " " [:small " by " (:oname vendor)]]
     [:> ui/Image {:class "product-logo"
                   :src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]
     [c-rounds product]
     (if requested-preposal?
       [:> ui/Label {:style {:marginRight 15}}
        "Preposal Requested"]
       [:> ui/Button {:onClick #(rf/dispatch [:b/create-preposal-req id])
                      :color "gray"
                      :style {:marginRight 15}}
        "Request a Preposal"])
     [c-categories product]
     [:> ui/Grid {:columns "equal"
                  :style {:margin "20px 0 0 0"}}
      [:> ui/GridRow
       [c-display-field {:width 12} "Product Description" long-desc]]
      [:> ui/GridRow
       [c-display-field {:width 6} "Pitch" "Unavailable (Request a Preposal)"]
       [c-display-field {:width 6} "Estimated Price" "Unavailable (Request a Preposal)"]]
      (when (not= "" (:url vendor))
        [:> ui/GridRow
         [c-display-field nil (str "About " (:oname vendor))
          [:span "Website: " [:a {:href (str "http://" (:url vendor)) ; todo: fragile
                                  :target "_blank"}
                              (:url vendor)]]]])]]))

(defn c-page []
  (let [product-idstr& (rf/subscribe [:product-idstr])
        org-id& (rf/subscribe [:org-id])
        products& (rf/subscribe [:gql/sub
                                 {:queries
                                  [[:products {:idstr @product-idstr&}
                                    [:id :pname :logo :short-desc :long-desc
                                     [:vendor [:id :oname :url]]
                                     [:forms {:ftype "preposal" ; preposal requests
                                              :from-org-id @org-id&}
                                      [:id]]
                                     [:rounds {:buyer-id @org-id&
                                               :status "active"}
                                      [:id :created :status]]
                                     [:categories [:id :idstr :cname]]]]]}])]
    (fn []
      [:div.container-with-sidebar
       [:div.sidebar
        [:> ui/Button {:on-click #(rf/dispatch [:b/nav-search])
                       :color "gray"
                       :icon true
                       :labelPosition "left"}
         "Back to Search"
         [:> ui/Icon {:name "left arrow"}]]]
       [:> ui/Segment {:class "inner-container"}
        (if (= :loading @products&)
          [:> ui/Loader {:active true :inline true}]
          [c-product (-> @products& :products first)])
        ]])))
