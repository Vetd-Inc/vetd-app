(ns vetd-app.vendors.pages.products
  (:require [vetd-app.ui :as ui]
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

(defn c-product-row
  [{:keys [id pname created updated]}]
  (fn [{:keys [id pname created updated]}]
    [:div {:on-click #(rf/dispatch [:v/nav-product-detail id])
           :style {:cursor :pointer
                   :width "800px"
                   :margin "10px"
                   :padding "10px"
                   :border "solid 1px #666666"}}
     [:div "Name: " pname]
     [:div "Created: " (.toString (js/Date. created))]
     [:div "Updated: " (.toString (js/Date. updated))]]))

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
      [:div
       [:> ui/Button {:color "teal"
                      :fluid true
                      :on-click #(rf/dispatch [:v/new-product @org-id&])}
        "New Product"]
       (for [{:keys [id] :as p} (:products @prods&)]
         ^{:key (str "product" id)}
         [:div [c-product-row p]])])))
