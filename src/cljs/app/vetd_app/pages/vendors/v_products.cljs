(ns vetd-app.pages.vendors.v-products
  (:require [vetd-app.ui :as ui]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :v/nav-products
 (fn [{:keys [db]} _]
   {:nav {:path "/v/products/"}}))

(rf/reg-event-db
 :v/route-products
 (fn [db [_ query-params]]
   (assoc db
          :page :v/products
          :query-params query-params)))

(rf/reg-event-fx
 :v/new-product
 (fn [{:keys [db]} [_ vendor-id]]
   {:ws-send {:payload {:cmd :v/new-product
                        :vendor-id vendor-id}}}))

(rf/reg-event-fx
 :v/save-product
 (fn [{:keys [db]} [_ product]]
   {:ws-send {:payload {:cmd :v/save-product
                        :product product}}}))

(rf/reg-event-fx
 :v/delete-product
 (fn [{:keys [db]} [_ product-id]]
   {:ws-send {:payload {:cmd :v/delete-product
                        :product-id product-id}}}))

(defn c-product
  [{:keys [id pname short-desc long-desc logo url created updated]}]
  (let [org-id& (rf/subscribe [:org-id])
        pname& (r/atom pname)
        short-desc& (r/atom short-desc)
        long-desc& (r/atom long-desc)
        logo& (r/atom logo)
        url& (r/atom url)]
    (fn []
      [:div {:style {:width "400px"}}
       [:> ui/Form {:style {:margin "10px"
                            :padding "10px"
                            :border "solid 1px #666666"}}
        [:div "created: " created]
        [:div "updated: " updated]       
        [:> ui/FormField
         [:> ui/Input {:value @pname&
                       :placeholder "Product Name"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! pname& (.-value this)))}]]
        [:> ui/FormField
         [:> ui/TextArea {:value @short-desc&
                          :style {:height "100px"}
                          :placeholder "Short Description"
                          :spellCheck false
                          :onChange (fn [_ this] (reset! short-desc&  (.-value this)))}]]
        [:> ui/FormField
         [:> ui/TextArea {:value @long-desc&
                          :style {:height "100px"}                        
                          :placeholder "Long Description"
                          :spellCheck false
                          :onChange (fn [_ this] (reset! long-desc&  (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:value @logo&
                       :type "url"
                       :placeholder "Logo"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! logo& (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:value @url&
                       :type "url"
                       :placeholder "Product Website"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! url& (.-value this)))}]]
        [:> ui/Button {:color "teal"
                       :fluid true
                       :on-click #(rf/dispatch [:v/save-product {:id id
                                                                 :pname @pname&
                                                                 :short-desc @short-desc&
                                                                 :long-desc @long-desc&
                                                                 :logo @logo&
                                                                 :url @url&}])}
         "Save Product"]
        [:> ui/Button {:color "red"
                       :fluid true
                       :on-click #(rf/dispatch [:v/delete-product id])}
         "DELETE  Product"]]])))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        prods& (rf/subscribe [:gql/sub
                              {:queries
                               [[:products {:vendor-id @org-id&
                                            :_order_by {:created :desc}
                                            :deleted nil}
                                 [:id
                                  :pname
                                  :short-desc
                                  :long-desc
                                  :logo
                                  :url
                                  :created
                                  :updated]]]}])]
    (fn []
      (def p1 @prods&)
      [:div
       [:> ui/Button {:color "teal"
                      :fluid true
                      :on-click #(rf/dispatch [:v/new-product @org-id&])}
        "New Product"]
       (for [p (:products @prods&)]
         ^{:key (str "form" (:id p))}
         [c-product p])])))
