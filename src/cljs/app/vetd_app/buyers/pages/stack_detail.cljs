(ns vetd-app.buyers.pages.stack-detail
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

;;;; Subscriptions
(rf/reg-sub :org-idstr
 :<- [:page-params] 
 (fn [{:keys [org-idstr]}] org-idstr))

;;;; Events
(rf/reg-event-fx :b/nav-stack-detail
 (fn [_ [_ org-idstr]]
   {:nav {:path (str "/b/stacks/" org-idstr)}}))

(rf/reg-event-fx :b/route-stack-detail
 (fn [{:keys [db]} [_ org-idstr]]
   {:db (assoc db
               :page :b/stack-detail
               :page-params {:org-idstr org-idstr})
    :analytics/page {:name "Buyers Stack Detail"
                     :props {:org-idstr org-idstr}}}))

;;;; Components
(defn c-stack-item
  [{:keys [price-amount price-period renewal-date] :as stack-item}]
  (fn [{:keys [id rating price-amount price-period
               renewal-date renewal-reminder status
               product] :as stack-item}]
    (let [{product-id :id
           product-idstr :idstr
           :keys [pname short-desc logo vendor]} product]
      [:> ui/Item {:on-click #(rf/dispatch [:b/nav-product-detail product-idstr])}
       [bc/c-product-logo logo]
       [:> ui/ItemContent
        [:> ui/ItemHeader
         pname " " [:small " by " (:oname vendor)]]
        [:<>
         [:> ui/ItemExtra {:style {:color "rgba(0, 0, 0, 0.85)"
                                   :font-size 14
                                   :line-height "14px"}}
          [:> ui/Grid {:class "stack-item-grid"}
           [:> ui/GridRow {:class "field-row"}
            [:> ui/GridColumn {:width 13}
             [bc/c-categories product]]
            [:> ui/GridColumn {:width 3
                               :style {:text-align "right"}}
             [:div {:style {:margin-bottom 4}}
              "Their Rating"]
             [:> ui/Rating {:rating rating
                            :maxRating 5
                            :size "huge"
                            :disabled true}]]]]]]]])))

(defn c-page []
  (let [org-idstr& (rf/subscribe [:org-idstr])
        org-stack& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:orgs {:idstr @org-idstr&
                                            :_limit 1}
                                     [:id :oname
                                      [:stack-items {:_order_by {:created :desc}
                                                     :deleted nil
                                                     ;; TODO infinite scroll
                                                     :_limit 100}
                                       [:id :idstr :status
                                        :price-amount :price-period :rating
                                        :renewal-date :renewal-reminder
                                        [:product
                                         [:id :pname :idstr :logo
                                          [:vendor
                                           [:id :oname :idstr :short-desc]]
                                          [:categories {:ref-deleted nil}
                                           [:id :idstr :cname]]]]]]]]]}])]
    (fn []
      (if (= :loading @org-stack&)
        [cc/c-loader]
        (let [{:keys [oname stack-items] :as org} (first (:orgs @org-stack&))]
          [:div.container-with-sidebar.public-stack
           [:div.sidebar
            [:div {:style {:padding "0 15px"}}
             [bc/c-back-button "Back"]]
            [:> ui/Segment {:class "top-categories"}
             [:h4 "Jump To"]
             [:div
              [:a.blue {:on-click #(rf/dispatch [:scroll-to :current-stack])}
               "Current Stack"]]
             [:div
              [:a.blue {:on-click #(rf/dispatch [:scroll-to :previous-stack])}
               "Previous Stack"]]]]
           [:div.inner-container
            [:> ui/Segment {:class "detail-container"}
             [:h1 oname "'" (when (not= (last oname) "s") "s") " Stack"]]
            [:div.stack
             [:h2 "Current"]
             [:span.scroll-anchor {:ref (fn [this]
                                          (rf/dispatch [:reg-scroll-to-ref :current-stack this]))}]
             [:> ui/ItemGroup {:class "results"}
              (let [current-stack-items (filter (comp (partial = "current") :status) stack-items)]
                (if (seq current-stack-items)
                  (for [stack-item current-stack-items]
                    ^{:key (:id stack-item)}
                    [c-stack-item stack-item])
                  [:div {:style {:margin-left 14
                                 :margin-right 14}}
                   "This organization doesn't have any products in their current stack."]))]]
            [:div.stack
             [:h2 "Previous"]
             [:span.scroll-anchor {:ref (fn [this]
                                          (rf/dispatch [:reg-scroll-to-ref :previous-stack this]))}]
             [:> ui/ItemGroup {:class "results"}
              (let [previous-stack-items (filter (comp (partial = "previous") :status) stack-items)]
                (if (seq previous-stack-items)
                  (for [stack-item previous-stack-items]
                    ^{:key (:id stack-item)}
                    [c-stack-item stack-item])
                  [:div {:style {:margin-left 14
                                 :margin-right 14}}
                   "This organization hasn't listed any previously used products."]))]]]])))))
