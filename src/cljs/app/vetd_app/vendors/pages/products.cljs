(ns vetd-app.vendors.pages.products
  (:require [vetd-app.util :as util]
            [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :v/nav-products
 (constantly
  {:nav {:path "/v/products"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Vendors Products"}}}))

(rf/reg-event-fx
 :v/route-products
 (fn [{:keys [db]}]
   {:db (assoc db :page :v/products)}))

(rf/reg-event-fx
 :v/new-product
 (fn [{:keys [db]} [_ vendor-id]]
   {:ws-send {:payload {:cmd :v/new-product
                        :vendor-id vendor-id}}}))

(defn c-product
  [{:keys [id pname created]}]
  (let [nav-click (fn [e]
                    (.stopPropagation e)
                    (rf/dispatch [:v/nav-product-detail id]))]
    [:> ui/Item {:onClick nav-click}
     [:> ui/ItemContent
      [:> ui/ItemHeader
       [:> ui/Button {:onClick nav-click
                      :color "blue"
                      :icon true
                      :labelPosition "right"
                      :floated "right"}
        "Edit Product"
        [:> ui/Icon {:name "right arrow"}]]
       pname
       [:div {:style {:margin-top 3
                      :font-weight 400}}
        [:small
         "Created: " (util/relative-datetime (.getTime (js/Date. created))
                                             {:trim-day-of-week? true})]]]]]))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        prods& (rf/subscribe [:gql/sub
                              {:queries
                               [[:products {:vendor-id @org-id&
                                            :_order_by {:created :desc}
                                            :deleted nil}
                                 [:id
                                  :pname
                                  :created
                                  :updated]]]}])]
    (fn []
      (if (= :loading @prods&)
        [cc/c-loader]
        [:> ui/Grid {:stackable true}
         [:> ui/GridRow
          [:> ui/GridColumn {:computer 4 :mobile 16}]
          [:> ui/GridColumn {:computer 8 :mobile 16}
           [:> ui/Segment {:class "detail-container"}
            [:> ui/Button {:color "teal"
                           :fluid true
                           :on-click #(rf/dispatch [:v/new-product @org-id&])}
             "New Product"]]]
          [:> ui/GridColumn {:computer 4 :mobile 16}]]
         [:> ui/GridRow
          [:> ui/GridColumn {:computer 4 :mobile 16}]
          [:> ui/GridColumn {:computer 8 :mobile 16}
           (if (seq (:products @prods&))
             [:> ui/ItemGroup {:class "results"}
              (for [{:keys [id] :as p} (:products @prods&)]
                ^{:key (str "product" id)}
                [c-product p])]
             [:> ui/Segment {:placeholder true
                             :class "how-vetd-works"}
              [:> ui/Header {:icon true}
               [:> ui/Icon {:name "th list"}]
               "You don't have any products."]])]
          [:> ui/GridColumn {:computer 4 :mobile 16}]]]))))
