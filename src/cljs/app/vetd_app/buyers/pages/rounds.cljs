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

;;;; Subscriptions



;;;; Components
(defn c-round
  [{:keys [id status] :as round}]
  [:p "Round " id " with status: " status])

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])]
    (when @org-id&
      (let [rounds& (rf/subscribe [:gql/sub
                                   {:queries
                                    [[:rounds {:buyer-id @org-id&
                                               :status "active"}
                                      [:id :created :status]]]}])]
        (fn []
          [:div.container-with-sidebar
           [:div.sidebar
            [:h4 "No Sidebar?"]]
           [:> ui/ItemGroup {:class "inner-container results"}
            (if (= :loading @rounds&)
              [cc/c-loader]
              (let [rounds (:rounds @rounds&)]
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
