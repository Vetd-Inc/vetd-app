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
                                               :fsubtype :list? :sort]]]]]]]}])
        pname& (r/atom nil)
        save-doc-fn& (atom nil)
        popup-open? (r/atom false)]
    (fn []
      (if (= :loading @prod-prof-form&)
        [cc/c-loader]
        (let [prod-prof-form (-> @prod-prof-form& :forms first)
              {:keys [id pname form-docs] :as p} (-> @prods& :products first)
              _ (when (nil? @pname&)
                  (reset! pname& pname))
              {:keys [doc-product] :as form-doc} (first form-docs)
              form-doc' (when form-doc
                          (assoc form-doc
                                 :product
                                 doc-product))]
          [:div.container-with-sidebar
           [:div.sidebar
            [:> ui/Segment
             [:> ui/Button {:color "blue"
                            :fluid true
                            :on-click #(do (rf/dispatch [:v/save-product {:id id
                                                                          :pname @pname&}])
                                           (@save-doc-fn&))}
              "Save Changes"]
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
                         [:> ui/Button {:color "white"
                                        :fluid true
                                        :on-click #(swap! popup-open? not)}
                          "Delete"])}]]
            ;; [:> ui/Segment {:class "top-categories"}
            ;;  [:h4 "Jump To"]
            ;;  [:div
            ;;   [:a.blue {:on-click (fn [e]
            ;;                         (.stopPropagation e)
            ;;                         (rf/dispatch [:scroll-to :current-stack]))}
            ;;    "Current Stack"]]
            ;;  [:div
            ;;   [:a.blue {:on-click (fn [e]
            ;;                         (.stopPropagation e)
            ;;                         (rf/dispatch [:scroll-to :previous-stack]))}
            ;;    "Previous Stack"]]]
            ]
           [:div.inner-container
            [:> ui/Segment {:class "detail-container"}
             (when id
               [:<>
                [:h2 "Edit Product - " pname]
                [:> ui/Form {:as "div"}
                 [:> ui/FormField
                  "Product Name"
                  [ui/input {:value @pname&
                             :placeholder "Product Name"
                             :spellCheck false
                             :on-change (fn [this]
                                          (reset! pname& (-> this .-target .-value)))}]]
                 [docs/c-form-maybe-doc
                  (docs/mk-form-doc-state
                   (or form-doc'
                       (assoc prod-prof-form :product {:id id}))
                   nil)
                  {:return-save-fn& save-doc-fn&
                   :c-wrapper [:div]}]]])]]])))))
