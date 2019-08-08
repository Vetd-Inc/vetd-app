(ns vetd-app.buyers.pages.stack
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(def init-db
  {:filter {:status #{}}
   :loading? true})

;;;; Subscriptions
(rf/reg-sub
 :b/stack
 (fn [{:keys [stack]}] stack))

(rf/reg-sub
 :b/stack.filter
 :<- [:b/stack]
 (fn [{:keys [filter]}]
   filter))

;;;; Events
(rf/reg-event-fx
 :b/nav-stack
 (constantly
  {:nav {:path "/b/stack"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Buyers Stack"}}}))

(rf/reg-event-fx
 :b/route-stack
 (fn [{:keys [db]}]
   {:db (assoc db :page :b/stack)
    :analytics/page {:name "Buyers Stack"}}))

(rf/reg-event-fx
 :b/stack.filter.add
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:stack :filter group] conj value)}))

(rf/reg-event-fx
 :b/stack.filter.remove
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:stack :filter group] disj value)}))

;;;; Components
(defn c-stack-item
  [{:keys [product] :as stack-item}]
  (let [{:keys [id idstr pname short-desc logo 
                form-docs vendor ]} product
        product-v-fn (->> form-docs
                          first
                          :response-prompts
                          (partial docs/get-value-by-term))]
    [:> ui/Item {:on-click #(rf/dispatch [:b/nav-product-detail idstr])}
     [bc/c-product-logo logo]
     [:> ui/ItemContent
      [:> ui/ItemHeader
       pname " " [:small " by " (:oname vendor)]]
      [:> ui/ItemMeta
       "Something in meta"]
      [:> ui/ItemDescription (bc/product-description product-v-fn)]
      [:> ui/ItemExtra "some tags here?"]]]))

(defn c-status-filter-checkboxes
  [stack selected-statuses]
  (let [all-possible-statuses ["current" "past"]
        statuses (->> stack
                      (group-by :status)
                      (merge (zipmap all-possible-statuses (repeatedly vec))))]
    [:<>
     (for [[status rs] statuses]
       ^{:key status} 
       [:> ui/Checkbox
        {:label (str (-> status
                         (s/replace  #"-" " ")
                         util/capitalize-words) 
                     " (" (count rs) ")")
         :checked (boolean (selected-statuses status))
         :on-change (fn [_ this]
                      (if (.-checked this)
                        (rf/dispatch [:b/stack.filter.add "status" status])
                        (rf/dispatch [:b/stack.filter.remove "status" status])))}])]))

(defn filter-stack
  [stack selected-statuses]
  (filter #(selected-statuses (:status %)) stack))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])]
    (when @org-id&
      (let [filter& (rf/subscribe [:b/stack.filter])
            stack& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:stack-items {:buyer-id @org-id&
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
            (let [unfiltered-stack (:stack-items @stack&)
                  selected-statuses (:status @filter&)]
              (if (seq unfiltered-stack)
                [:div.container-with-sidebar
                 [:div.sidebar
                  [:> ui/Segment
                   [bc/c-start-round-button {:etype :none
                                             :props {:fluid true}}]]
                  [:> ui/Segment
                   [:h2 "Filter"]
                   [:h4 "Status"]
                   [c-status-filter-checkboxes unfiltered-stack selected-statuses]]]
                 [:> ui/ItemGroup {:class "inner-container results"}
                  (let [stack unfiltered-stack #_(cond-> unfiltered-stack
                                                   (seq selected-statuses) (filter-stack selected-statuses))]
                    (for [stack-item stack]
                      ^{:key (:id stack-item)}
                      [c-stack-item stack-item]))]
                 ]
                [:> ui/Grid
                 [:> ui/GridRow
                  [:> ui/GridColumn {:computer 2 :mobile 0}]
                  [:> ui/GridColumn {:computer 12 :mobile 16}
                   [:> ui/Segment {:placeholder true}
                    [:> ui/Header {:icon true}
                     [:> ui/Icon {:name "grid layout"}]
                     "Your stack is empty."]
                    [:> ui/Button {;; :on-click (fn [e] )
                                   :color "teal" ; yellow ?
                                   :icon true
                                   :labelPosition "left"
                                   :fluid true
                                   :style {:margin-top 15}}
                     "Add a Product"
                     [:> ui/Icon {:name "add"}]]]]
                  [:> ui/GridColumn {:computer 2 :mobile 0}]]]))))))))
