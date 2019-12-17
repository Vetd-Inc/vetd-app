(ns vetd-app.vendors.pages.product-detail
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 :v/nav-product-detail
 (fn [{:keys [db]} [_ product-id]]
   (let [product-idstr (util/base31->str product-id)]
     {:nav {:path (str "/v/products/" product-idstr)}
      :analytics/track {:event "Navigate"
                        :props {:category "Navigation"
                                :label "Vendors Product Detail"
                                :product-idstr product-idstr}}})))

(rf/reg-event-fx
 :v/route-product-detail
 (fn [{:keys [db]} [_ product-idstr]]
   {:db (assoc db
               :page :v/product-detail
               :page-params {:product-idstr product-idstr})
    :analytics/page {:name "Vendors Product Detail"
                     :props {:product-idstr product-idstr}}}))

(rf/reg-event-fx
 :v/save-product
 (fn [{:keys [db]} [_ product]]
   {:ws-send {:payload {:cmd :v/save-product
                        :product product}}}))
(rf/reg-event-fx
 :v/delete-product
 (fn [{:keys [db]} [_ product-id]]
   {:ws-send {:payload {:cmd :v/delete-product
                        :return {:handler :v/delete-product.return}
                        :product-id product-id}}}))

(rf/reg-event-fx
 :v/delete-product.return
 (constantly
  {:toast {:type "success"
           :title "Product Deleted"}
   :dispatch [:v/nav-products]}))

(defn c-product
  [{:keys [id pname form-doc created updated actions]}]
  (let [pname& (r/atom pname)
        save-doc-fn& (atom nil)
        popup-open? (r/atom false)]
    (fn [{:keys [id pname form-doc created updated actions]}]
      [:> ui/Form {:as "div"}
       [:> ui/FormField
        "Product Name"
        [ui/input {:value (or @pname& pname) ;; necessary for some reason??
                   :placeholder "Product Name"
                   :spellCheck false
                   :on-change (fn [this]
                                (reset! pname& (-> this .-target .-value)))}]]
       [docs/c-form-maybe-doc
        (docs/mk-form-doc-state form-doc actions)
        {:return-save-fn& save-doc-fn&
         :c-wrapper [:div]}]
       [:> ui/Grid {:style {:margin-top 7}}
        [:> ui/GridRow
         [:> ui/GridColumn {:width 12}
          [:> ui/Button {:color "blue"
                         :fluid true
                         :on-click #(do (rf/dispatch [:v/save-product {:id id
                                                                       :pname @pname&}])
                                        (@save-doc-fn&))}
           "Save Changes"]]
         [:> ui/GridColumn {:width 4}
          [:> ui/Popup
           {:position "bottom right"
            :on "click"
            :open @popup-open?
            :on-close #(reset! popup-open? false)
            :content (r/as-element
                      [:div
                       [:h5 "Are you sure you want to delete this product (" pname ")?"]
                       [:> ui/ButtonGroup {:fluid true}
                        [:> ui/Button {:on-click #(reset! popup-open? false)}
                         "Cancel"]
                        [:> ui/Button {:on-click (fn []
                                                   (reset! popup-open? false)
                                                   (rf/dispatch [:v/delete-product id]))
                                       :color "red"}
                         "Delete"]]])
            :trigger (r/as-element
                      [:> ui/Button {:color "red"
                                     :fluid true
                                     :on-click #(swap! popup-open? not)}
                       "Delete"])}]]]]])))

(defn c-page []
  (let [product-idstr& (rf/subscribe [:product-idstr])
        prods& (rf/subscribe [:gql/sub
                              {:queries
                               [[:products {:idstr @product-idstr&
                                            :deleted nil
                                            :_order_by {:created :desc}
                                            :_limit 1}
                                 [:id :pname :short-desc :long-desc :logo :url :created :updated
                                  [:categories {:ref-deleted nil}
                                   [:id]]
                                  [:form-docs {:ftype "product-profile"}
                                   [:id :title :ftype :fsubtype
                                    :doc-id :doc-title
                                    [:doc-product
                                     [:id]]
                                    [:prompts {:ref-deleted nil
                                               :_order_by {:sort :asc}}
                                     [:id :idstr :prompt :descr :sort :term
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
                                            [:id :idstr :prompt :descr :sort :ref-id
                                             [:fields {:_order_by {:sort :asc}
                                                       :deleted nil}
                                              [:id :idstr :fname :ftype
                                               :fsubtype :list? :sort]]]]]]]}])]
    (fn []
      (if (= :loading @prod-prof-form&)
        [cc/c-loader]
        [:> ui/Grid {:stackable true}
         [:> ui/GridRow
          [:> ui/GridColumn {:computer 4 :mobile 16}]
          [:> ui/GridColumn {:computer 8 :mobile 16}
           [:> ui/Segment {:class "detail-container"}
            (let [prod-prof-form (-> @prod-prof-form& :forms first)
                  {:keys [id pname form-docs] :as p} (-> @prods& :products first)
                  {:keys [doc-product] :as form-doc} (first form-docs)
                  form-doc' (when form-doc
                              (assoc form-doc
                                     :product
                                     doc-product))]
              (when id
                [:<>
                 [:h2 pname]
                 [c-product (assoc p :form-doc (or form-doc'
                                                   (assoc prod-prof-form :product {:id id})))]]))]]
          [:> ui/GridColumn {:computer 4 :mobile 16}]]]))))
