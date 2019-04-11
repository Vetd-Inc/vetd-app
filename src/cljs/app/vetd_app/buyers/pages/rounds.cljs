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

(defn filter-rounds
  [rounds selected-statuses]
  (filter #(selected-statuses (:status %)) rounds)
  ;; (->> (for [{:keys [product] :as preposal} preposals
  ;;            category (:categories product)]
  ;;        (when (selected-categories (:id category))
  ;;          preposal))
  ;;      (remove nil?)
  ;;      distinct)
  )

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
          [:div.container-with-sidebar
           [:div.sidebar
            [:h4 "Filter By Status"]
            (let [statuses (->> @rounds&
                                :rounds
                                (group-by :status)
                                (merge {"active" []
                                        "completed" []}))]
              (doall
               (for [[status rs] statuses]
                 ^{:key status} 
                 [:> ui/Checkbox
                  {:label (str status " (" (count rs) ")")
                   :checked (boolean (@selected-statuses& status))
                   :onChange (fn [_ this]
                               (if (.-checked this)
                                 (rf/dispatch [:b/rounds-filter.add-selected-status status])
                                 (rf/dispatch [:b/rounds-filter.remove-selected-status status])))}])))]
           [:> ui/ItemGroup {:class "inner-container results"}
            (if (= :loading @rounds&)
              [cc/c-loader]
              (let [rounds (cond-> (:rounds @rounds&)
                             (seq @selected-statuses&) (filter-rounds @selected-statuses&))]
                (if (seq rounds)
                  (for [round rounds]
                    ^{:key (:id round)}
                    [c-round round])
                  [:div {:style {:width 500
                                 :margin "70px auto"}}
                   [:h3 {:style {:margin-bottom 5}} "You currently don't have any Rounds."]
                   "To get started, request a blah blah blah from the "
                   [:a {:style {:cursor "pointer"}
                        :onClick #(rf/dispatch [:b/nav-search])}
                    "Products & Categories"]
                   " page."
                   [:br]
                   "Or, simply forward any sales emails you receive to forward@vetd.com."])))]])))))
