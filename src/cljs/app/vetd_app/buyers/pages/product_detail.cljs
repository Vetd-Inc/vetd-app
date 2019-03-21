(ns vetd-app.buyers.pages.product-detail
  (:require [vetd-app.buyers.components :as c]
            [vetd-app.ui :as ui]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]))

;; Events
(rf/reg-event-fx
 :b/nav-product-detail
 (fn [_ [_ product-idstr]]
   {:nav {:path (str "/b/products/" product-idstr)}}))

(rf/reg-event-fx
 :b/route-product-detail
 (fn [{:keys [db]} [_ product-idstr]]
   {:db (assoc db
               :page :b/product-detail
               :page-params {:product-idstr product-idstr})
    :analytics/page {:name "Buyers Product Detail"
                     :props {:product-idstr product-idstr}}}))

;; Subscriptions
(rf/reg-sub
 :product-idstr
 :<- [:page-params] 
 (fn [{:keys [product-idstr]}] product-idstr))

;; Components
(defn c-product
  "Component to display Preposal details."
  [{:keys [id pname long-desc url logo vendor forms rounds categories] :as product}]
  (let [requested-preposal? (not-empty forms)]
    [:div.detail-container
     [:> ui/Header {:size "huge"}
      pname " " [:small " by " (:oname vendor)]]
     [:> ui/Image {:class "product-logo"
                   :src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]
     [c/c-rounds product]
     (if requested-preposal?
       [:> ui/Label {:style {:marginRight 15}}
        "Preposal Requested"]
       [:> ui/Popup
        {:content (str "Get a pricing estimate, personalized pitch, and more from " (:oname vendor) ".")
         :header "What is a Preposal?"
         :position "bottom left"
         :trigger (r/as-element
                   [:> ui/Button {:onClick #(rf/dispatch [:b/create-preposal-req product vendor])
                                  :color "gray"
                                  :style {:marginRight 15}}
                    "Request a Preposal"])}])
     [c/c-categories product]
     [:> ui/Grid {:columns "equal"
                  :style {:margin "20px 0 0 0"}}
      [:> ui/GridRow
       [c/c-display-field {:width 12} "Product Description"
        [:<> (or long-desc "No description available.")
         (when (not-empty url)
           [:p "Website: " [:a {:href (str "http://" url) ; todo: fragile
                                :target "_blank"}
                            [:> ui/Icon {:name "external square"
                                         :color "blue"}]
                            url]])]]]
      [:> ui/GridRow
       [c/c-display-field {:width 6} "Pitch" "Unavailable (Request a Preposal)"]
       [c/c-display-field {:width 6} "Estimated Price" "Unavailable (Request a Preposal)"]]
      (when (not= "" (:url vendor))
        [:> ui/GridRow
         [c/c-display-field nil (str "About " (:oname vendor))
          [:span "Website: " [:a {:href (str "http://" (:url vendor)) ; todo: fragile
                                  :target "_blank"}
                              [:> ui/Icon {:name "external square"
                                           :color "blue"}]
                              (:url vendor)]]]])]]))

(defn c-page []
  (let [product-idstr& (rf/subscribe [:product-idstr])
        org-id& (rf/subscribe [:org-id])
        products& (rf/subscribe [:gql/sub
                                 {:queries
                                  [[:products {:idstr @product-idstr&}
                                    [:id :pname :logo :short-desc :long-desc :url
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
          [c-product (-> @products& :products first)])]])))
