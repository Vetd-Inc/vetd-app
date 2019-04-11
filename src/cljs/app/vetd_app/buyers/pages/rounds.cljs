(ns vetd-app.buyers.pages.rounds
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

;;;; Events
(rf/reg-event-fx
 :b/nav-rounds
 (constantly
  {:nav {:path "/b/rounds"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Buyers Rounds"}}}))

(rf/reg-event-fx
 :b/route-rounds
 (fn [{:keys [db]}]
   {:db (assoc db :page :b/rounds)
    :analytics/page {:name "Buyers Rounds"}}))

(rf/reg-event-fx
 :b/rounds-filter.add-selected-status
 (fn [{:keys [db]} [_ status]]
   {:db (update-in db [:rounds-filter :selected-statuses] conj status)
    :analytics/track {:event "Filter"
                      :props {:category "Rounds"
                              :label (str "Added Status: " status)}}}))

(rf/reg-event-fx
 :b/rounds-filter.remove-selected-status
 (fn [{:keys [db]} [_ status]]
   {:db (update-in db [:rounds-filter :selected-statuses] disj status)}))

;;;; Subscriptions
(rf/reg-sub
 :rounds-filter
 :rounds-filter)

;; a set of statuses to allow through filter (if empty, let all statuses through)
(rf/reg-sub
 :rounds-filter/selected-statuses
 :<- [:rounds-filter]
 :selected-statuses)

;;;; Components
(defn c-round
  [{:keys [id status] :as round}]
  [:p "Round " id " with status: " status])

(defn c-status-filter-checkboxes
  [rounds selected-statuses]
  (let [all-possible-statuses ["active" "completed"]
        statuses (->> rounds
                      (group-by :status)
                      (merge (zipmap all-possible-statuses (repeatedly vec))))]
    [:<>
     (for [[status rs] statuses]
       ^{:key status} 
       [:> ui/Checkbox
        {:label (str status " (" (count rs) ")")
         :checked (boolean (selected-statuses status))
         :onChange (fn [_ this]
                     (if (.-checked this)
                       (rf/dispatch [:b/rounds-filter.add-selected-status status])
                       (rf/dispatch [:b/rounds-filter.remove-selected-status status])))}])]))

(defn filter-rounds
  [rounds selected-statuses]
  (filter #(selected-statuses (:status %)) rounds))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])]
    (when @org-id&
      (let [selected-statuses& (rf/subscribe [:rounds-filter/selected-statuses])
            rounds& (rf/subscribe [:gql/sub
                                   {:queries
                                    [[:rounds {:buyer-id @org-id&
                                               :status "active"}
                                      [:id :created :status]]]}])]
        (fn []
          (if (= :loading @rounds&)
            [cc/c-loader]
            (let [unfiltered-rounds (:rounds @rounds&)]
              (if (seq unfiltered-rounds)
                [:div.container-with-sidebar
                 [:div.sidebar
                  [:h4 "Filter By Status"]
                  [c-status-filter-checkboxes unfiltered-rounds @selected-statuses&]]
                 [:> ui/ItemGroup {:class "inner-container results"}
                  (let [rounds (cond-> unfiltered-rounds
                                 (seq @selected-statuses&) (filter-rounds @selected-statuses&))]
                    (for [round rounds]
                      ^{:key (:id round)}
                      [c-round round]))]]
                [:> ui/Grid
                 [:> ui/GridRow
                  [:> ui/GridColumn {:computer 2 :mobile 0}]
                  [:> ui/GridColumn {:computer 12 :mobile 16}
                   [:> ui/Segment {:placeholder true}
                    [:> ui/Header {:icon true}
                     [:> ui/Icon {:name "vetd"}]
                     "You don't have any active VetdRounds."]
                    [:div {:style {:text-align "center"
                                   :margin-top 10}}
                     "To start a new round, find a "
                     [:a {:style {:cursor "pointer"}
                          :onClick #(rf/dispatch [:b/nav-search])}
                      "product or category"]
                     " and click \"Start VetdRound\"."]]]
                  [:> ui/GridColumn {:computer 2 :mobile 0}]]]))))))))
