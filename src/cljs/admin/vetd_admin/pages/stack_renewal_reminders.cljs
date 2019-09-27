(ns vetd-admin.pages.stack-renewal-reminders
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(rf/reg-event-fx
 :a/nav-stack-renewal-reminders
 (fn [_ [_ idstr]]
   {:nav {:path "/a/stack-renewal-reminders"}}))

(rf/reg-event-db
 :a/route-stack-renewal-reminders
 (fn [db]
   (assoc db :page :a/stack-renewal-reminders)))

;;;; Components
(defn c-stack-item
  [{:keys [id rating price-amount price-period
           renewal-date renewal-reminder status
           product buyer] :as stack-item}]
  (let [{product-id :id
         product-idstr :idstr
         :keys [pname short-desc logo vendor]} product
        {:keys [oname]} buyer]
    [:> ui/Item {:class "selectable"}
     [bc/c-product-logo logo]
     [:> ui/ItemContent
      [:> ui/ItemHeader {:on-click #(rf/dispatch [:b/nav-product-detail product-idstr])}
       pname " " [:small " by " (:oname vendor)]]
      [:<>
       [:> ui/ItemExtra {:style {:color "rgba(0, 0, 0, 0.85)"
                                 :font-size 14
                                 :line-height "14px"}}
        [:> ui/Grid {:class "stack-item-grid"}
         [:> ui/GridRow {:class "field-row"}
          [:> ui/GridColumn {:width 4}
           [:<>
            [:div {:style {:margin-bottom 4
                           :margin-top 8
                           :font-weight "bold"}}
             "Buyer"]
            oname]]
          [:> ui/GridColumn {:width 2}
           [:<>
            [:div {:style {:margin-bottom 4
                           :margin-top 8
                           :font-weight "bold"}}
             "Renewal Date"]
            (subs renewal-date 0 10)]]
          [:> ui/GridColumn {:width 3}
           [:<>
            [:div {:style {:margin-bottom 4
                           :margin-top 8
                           :font-weight "bold"}}
             "Price"]
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
                      "monthly" "month")])]))]]
          [:> ui/GridColumn {:width 4
                             :style {:display "flex"
                                     :justify-content "end"
                                     :flex-direction "row"
                                     :align-items "flex-end"}}
           [bc/c-categories product]]
          [:> ui/GridColumn {:width 3
                             :style {:text-align "right"}}
           (when rating
             [:<>
              [:div {:style {:margin-bottom 4}}
               "Their Rating"]
              [:> ui/Rating {:rating rating
                             :maxRating 5
                             :size "huge"
                             :disabled true}]])]]]]]]]))

(defn c-page []
  (let [limit 100
        start-date (-> (js/Date.)
                       .valueOf
                       (- (* 1000 60 60 24 2))
                       js/Date.
                       .toISOString
                       (subs 0 10))
        stack& (rf/subscribe
                [:gql/sub
                 {:queries
                  [[:stack-items {:_where
                                  {:_and [{:renewal-reminder {:_eq true}}
                                          {:renewal-date {:_gte start-date}}
                                          {:price-period {:_eq "annual"}}
                                          {:deleted {:_is_null true}}
                                          {:status {:_eq "current"}}]}
                                  :_order_by {:renewal-date :asc}
                                  :_limit limit}
                    [:id :idstr :status
                     :price-amount :price-period :rating
                     :renewal-date :renewal-reminder
                     [:buyer 
                      [:id :oname]]
                     [:product
                      [:id :pname :idstr :logo
                       [:vendor
                        [:id :oname :idstr :short-desc]]
                       [:categories {:ref-deleted nil}
                        [:id :idstr :cname]]]]]]]}])]
    (fn []
      (if (= :loading @stack&)
        [cc/c-loader]
        (let [stack-items (:stack-items @stack&)]
          [:div.container.public-stack
           [:div.inner-container
            [:div.stack
             [:h2
              "Annual Renewal Reminders since "
              start-date
              " ("
              (count stack-items)
              (when (= limit (count stack-items)) "+")
              " items)"]
             [:> ui/ItemGroup {:class "results"}
              (if (seq stack-items)
                (for [stack-item stack-items]
                  ^{:key (:id stack-item)}
                  [c-stack-item stack-item])
                [:div {:style {:margin-left 14
                               :margin-right 14}}
                 "Nothing here."])]]]])))))
