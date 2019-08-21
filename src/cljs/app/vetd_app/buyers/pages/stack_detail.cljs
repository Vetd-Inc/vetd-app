(ns vetd-app.buyers.pages.stack-detail
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.common.fx :as cfx]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

;;;; Subscriptions
(rf/reg-sub
 :org-idstr
 :<- [:page-params] 
 (fn [{:keys [org-idstr]}] org-idstr))

;;;; Events
(rf/reg-event-fx
 :b/nav-stack-detail
 (fn [_ [_ org-idstr]]
   {:nav {:path (str "/b/stack/" org-idstr)}}))

(rf/reg-event-fx
 :b/route-stack-detail
 (fn [{:keys [db]} [_ org-idstr]]
   {:db (assoc db
               :page :b/stack-detail
               :page-params {:org-idstr org-idstr})
    :analytics/page {:name "Buyers Stack Detail"
                     :props {:org-idstr org-idstr}}}))

;;;; Components
(defn c-stack-item
  [{:keys [price-amount price-period renewal-date] :as stack-item}]
  (let [stack-items-editing?& (rf/subscribe [:b/stack.items-editing])
        bad-input& (rf/subscribe [:bad-input])
        subscription-type& (r/atom price-period)
        price& (atom price-amount)
        ;; TODO fragile (expects particular string format of date from server)
        renewal-date& (atom (when renewal-date (subs renewal-date 0 10)))]
    (fn [{:keys [id rating price-amount price-period
                 renewal-date renewal-reminder status
                 product] :as stack-item}]
      (let [{product-id :id
             product-idstr :idstr
             :keys [pname short-desc logo 
                    form-docs vendor]} product
            product-v-fn (->> form-docs
                              first
                              :response-prompts
                              (partial docs/get-value-by-term))]
        [:> ui/Item {:class (when (@stack-items-editing?& id) "editing")
                     :on-click #(when-not (@stack-items-editing?& id)
                                  (rf/dispatch [:b/nav-product-detail product-idstr]))}
         [bc/c-product-logo logo]
         [:> ui/ItemContent
          [:> ui/ItemHeader
           [:<>
            ;; Edit button
            [:> ui/Label {:on-click (fn [e]
                                      (.stopPropagation e)
                                      (rf/dispatch [:b/stack.edit-item id]))
                          :as "a"
                          :style {:float "right"}}
             [:> ui/Icon {:name "edit outline"}]
             "Edit"]
            ;; Move to (Previous/Current) button
            (let [dest-status (if (= status "current") "previous" "current")]
              [:> ui/Popup
               {:position "bottom right"
                :on "click"
                :content (r/as-element
                          [:div.account-actions
                           [:> ui/Button {:on-click (fn [e]
                                                      (.stopPropagation e)
                                                      (rf/dispatch [:b/stack.move-item id dest-status]))
                                          :color "white"
                                          :fluid true
                                          :icon true
                                          :labelPosition "left"}
                            (str "To " (s/capitalize dest-status))
                            [:> ui/Icon {:name (str "angle double " (if (= status "current") "down" "up"))}]]
                           [:> ui/Button {:on-click (fn [e]
                                                      (.stopPropagation e)
                                                      (rf/dispatch [:b/stack.delete-item id]))
                                          :color "red"
                                          :fluid true
                                          :icon true
                                          :labelPosition "left"}
                            "Delete"
                            [:> ui/Icon {:name "x"}]]])
                :trigger (r/as-element
                          [:> ui/Label {:on-click (fn [e] (.stopPropagation e))
                                        :as "a"
                                        :style {:float "right"
                                                :margin-right 7}}
                           [:> ui/Icon {:name "caret down"}]
                           "Move"])}]
              )]
           ;; Product by Vendor heading
           pname " " [:small " by " (:oname vendor)]]
          [:<>
           [:> ui/ItemExtra {:style {:color "rgba(0, 0, 0, 0.85)"
                                     :font-size 14
                                     :line-height "14px"}}
            [:> ui/Grid {:class "stack-item-grid"}
             [:> ui/GridRow {:class "field-row"}
              [:> ui/GridColumn {:width 3}
               (when (or price-amount
                         (= price-period "free"))
                 (str (when (= price-period "other") "Estimated ")
                      "Price"))]
              [:> ui/GridColumn {:width 8}
               (when (and (= price-period "annual")
                          renewal-date)
                 "Annual Renewal")]
              [:> ui/GridColumn {:width 5
                                 :style {:text-align "right"}}
               "Your Rating"]]
             [:> ui/GridRow {:style {:margin-top 6}}
              [:> ui/GridColumn {:width 3}
               (if (= price-period "free")
                 "Free"
                 (when price-amount
                   [:<>
                    "$" (util/decimal-format price-amount)
                    (when price-period
                      [:<>
                       " / "
                       (case price-period
                         "annual" "year"
                         "other" "year"
                         "monthly" "month")])]))]
              [:> ui/GridColumn {:width 8}
               (when (and (= price-period "annual")
                          renewal-date)
                 [:<>
                  (subs renewal-date 0 10) ; TODO fragile (expects particular string format of date from server)
                  [:> ui/Popup
                   {:position "bottom center"
                    :content "Check this box to have a renewal reminder sent to you 2 months before your Annual Renewal date."
                    :trigger (r/as-element
                              [:> ui/Checkbox {:style {:margin-left 10
                                                       :font-size 12}
                                               :defaultChecked renewal-reminder
                                               :on-click (fn [e]
                                                           (.stopPropagation e))
                                               :on-change (fn [_ this]
                                                            (rf/dispatch [:b/stack.set-item-renewal-reminder id (.-checked this)])
                                                            ;; return 'this' to keep it as an uncontrolled component
                                                            this)
                                               :label "Remind?"}])}]])]
              [:> ui/GridColumn {:width 5
                                 :style {:text-align "right"}}
               [:> ui/Rating {:class (when-not rating "not-rated")
                              :maxRating 5
                              :size "large"
                              :defaultRating rating
                              :clearable false
                              :on-click (fn [e]
                                          (.stopPropagation e))
                              :onRate (fn [_ this]
                                        (rf/dispatch [:b/stack.rate-item id (aget this "rating")]))}]]]]]]]]))))

(defn c-no-stack-items []
  (let [group-ids& (rf/subscribe [:group-ids])]
    (fn []
      [:> ui/Segment {:placeholder true}
       [:> ui/Header {:icon true}
        [:> ui/Icon {:name "grid layout"}]
        "You don't have any products in your stack."]
       [:> ui/SegmentInline
        "Add products to your stack to keep track of renewals, get recommendations, and share with "
        (if (not-empty @group-ids&)
          "your community"
          "others")
        "."]])))

(defn c-page []
  (let [org-idstr& (rf/subscribe [:org-idstr])

        org-id& (rf/subscribe [:org-id])
        org-name& (rf/subscribe [:org-name])
        group-ids& (rf/subscribe [:group-ids])]
    (when @org-id&
      (let [stack& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:stack-items {:buyer-id @org-idstr&
                                                   :_order_by {:created :desc}
                                                   :deleted nil}
                                     [:id :idstr :status
                                      :price-amount :price-period :rating
                                      :renewal-date :renewal-reminder
                                      [:product
                                       [:id :pname :idstr :logo
                                        [:vendor
                                         [:id :oname :idstr :short-desc]] 
                                        [:form-docs {:ftype "product-profile"
                                                     :_order_by {:created :desc}
                                                     :_limit 1
                                                     :doc-deleted nil}
                                         [:id
                                          [:response-prompts {:prompt-term ["product/description"
                                                                            "product/free-trial?"]
                                                              :ref_deleted nil}
                                           [:id :prompt-id :notes :prompt-prompt :prompt-term
                                            [:response-prompt-fields
                                             [:id :prompt-field-fname :idx :sval :nval :dval]]]]]]]]]]]}])]
        (fn []
          (if (= :loading @stack&)
            [cc/c-loader]
            (let [unfiltered-stack (:stack-items @stack&)]
              [:div.container-with-sidebar
               [:div.sidebar
                [:> ui/Segment {:class "top-categories"}
                 [:h4 "Jump To"]
                 [:div
                  [:a.blue {:on-click (fn [e]
                                        (.stopPropagation e)
                                        (rf/dispatch [:scroll-to :current-stack]))}
                   "Current Stack"]]
                 [:div
                  [:a.blue {:on-click (fn [e]
                                        (.stopPropagation e)
                                        (rf/dispatch [:scroll-to :previous-stack]))}
                   "Previous Stack"]]]]
               [:div.inner-container
                [:> ui/Segment {:class "detail-container"}
                 [:h1 {:style {:padding-bottom 7}}
                  @org-name& "'" (when (not= (last @org-name&) "s") "s") " Stack"]
                 "Add products to your stack to keep track of renewals, get recommendations, and share with "
                 (if (not-empty @group-ids&)
                   "your community"
                   "others")
                 "."]
                [:div.stack
                 [:h2 "Current"]
                 [:span.scroll-anchor {:ref (fn [this]
                                              (rf/dispatch [:reg-scroll-to-ref :current-stack this]))}]
                 [:> ui/ItemGroup {:class "results"}
                  (let [stack (filter (comp (partial = "current") :status) unfiltered-stack)]
                    (if (seq stack)
                      (for [stack-item stack]
                        ^{:key (:id stack-item)}
                        [c-stack-item stack-item])
                      [:div {:style {:margin-left 14
                                     :margin-right 14}}
                       "You don't have any products in your current stack."]))]]
                [:div.stack
                 [:h2 "Previous"]
                 [:span.scroll-anchor {:ref (fn [this]
                                              (rf/dispatch [:reg-scroll-to-ref :previous-stack this]))}]
                 [:> ui/ItemGroup {:class "results"}
                  (let [stack (filter (comp (partial = "previous") :status) unfiltered-stack)]
                    (if (seq stack)
                      (for [stack-item stack]
                        ^{:key (:id stack-item)}
                        [c-stack-item stack-item])
                      [:div {:style {:margin-left 14
                                     :margin-right 14}}
                       "You haven't listed any previously used products."]))]]]])))))))
