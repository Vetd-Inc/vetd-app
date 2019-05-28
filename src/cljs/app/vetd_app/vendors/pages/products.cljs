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
   {:db (assoc db :page :v/products)
    :analytics/page {:name "Vendors Products"}}))

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
  [{:keys [id pname form-doc created updated]}]
  (let [pname& (r/atom pname)
        save-doc-fn& (atom nil)]
    (fn [{:keys [id pname form-doc created updated]}]
      [:div {:style {:width "800px"}}
       [:> ui/Form {:as "div"
                    :style {:margin "10px"
                            :padding "10px"
                            :border "solid 1px #666666"}}
        [:div "created: " created]
        [:div "updated: " updated]       
        [:> ui/FormField
         "Product Name"
         [:> ui/Input {:defaultValue @pname&
                       :placeholder "Product Name"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! pname& (.-value this)))}]]
        [docs/c-form-maybe-doc
         (docs/mk-form-doc-state form-doc)
         {:return-save-fn& save-doc-fn&
          :c-wrapper [:div]}]
        [:> ui/Button {:color "teal"
                       :fluid true
                       :on-click #(do
                                    (rf/dispatch [:v/save-product {:id id
                                                                   :pname @pname&}])
                                    (@save-doc-fn&))}
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
                                  :updated
                                  [:categories {:ref-deleted nil} [:id]]
                                  [:form-docs {:ftype "product-profile"}
                                   [:id :title :ftype :fsubtype
                                    :doc-id :doc-title
                                    [:doc-product [:id]]
                                    [:prompts {:ref-deleted nil
                                               :_order_by {:sort :asc}}
                                     [:id :idstr :prompt :descr :sort
                                      [:fields {:deleted nil
                                                :_order_by {:sort :asc}}
                                       [:id :idstr :fname :ftype
                                        :fsubtype :list? :sort]]]]
                                    [:responses {:deleted nil
                                                 :ref-deleted nil}
                                     [:id :prompt-id :notes
                                      [:fields {:deleted nil
                                                :_order_by {:idx :asc}}
                                       [:id :pf-id :idx :sval :nval :dval :jval]]]]]]]]]}])
        prod-prof-form& (rf/subscribe [:gql/q
                                       {:queries
                                        [[:forms {:ftype "product-profile"
                                                  :_order_by {:created :desc}
                                                  :_limit 1
                                                  :deleted nil}
                                          [:id :title :ftype :fsubtype
                                           [:prompts {:_order_by {:sort :asc}
                                                      :deleted nil
                                                      :ref-deleted nil}
                                            [:id :idstr :prompt :descr :sort
                                             [:fields {:_order_by {:sort :asc}
                                                       :deleted nil}
                                              [:id :idstr :fname :ftype
                                               :fsubtype :list? :sort]]]]]]]}])]
    (fn []
      (let [prod-prof-form (-> @prod-prof-form&
                               :forms
                               first )]
        [:div
         [:> ui/Button {:color "teal"
                        :fluid true
                        :on-click #(rf/dispatch [:v/new-product @org-id&])}
          "New Product"]
         (for [{:keys [id form-docs] :as p} (:products @prods&)]
           (let [{:keys [doc-product] :as form-doc} (first form-docs)
                 form-doc' (when form-doc
                             (assoc form-doc
                                    :product
                                    doc-product))]
             ^{:key (str "product" id)}
             [:div
              [c-product (assoc p
                                :form-doc
                                (or form-doc'
                                    (assoc prod-prof-form
                                           :product {:id id})))]]))]))))
