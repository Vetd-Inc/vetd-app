(ns vetd-app.buyers.pages.rounds
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [clojure.string :as s]))

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

(rf/reg-event-fx
 :b/round.share
 (fn [{:keys [db]} [_ round-id round-title email-addresses]]
   {:ws-send {:payload {:cmd :b/round.share
                        :return {:handler :b/round.share-return}
                        :round-id round-id
                        :round-title round-title
                        :email-addresses email-addresses
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Share"
                      :props {:category "Round"
                              :label round-id}}}))

(rf/reg-event-fx
 :b/round.share-return
 (constantly
  {:toast {:type "success"
           :title "VetdRound Shared!"}}))

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
  [{:keys [id idstr status title products] :as round} share-modal-fn]
  (let [nav-click (fn [e]
                    (.stopPropagation e)
                    (rf/dispatch [:b/nav-round-detail idstr]))]
    [:> ui/Item {:onClick nav-click}
     [:> ui/ItemContent
      [:> ui/ItemHeader
       [:> ui/Button {:onClick nav-click
                      :color "blue"
                      :icon true
                      :labelPosition "right"
                      :floated "right"}
        "View / Manage"
        [:> ui/Icon {:name "right arrow"}]]
       [:> ui/Button {:onClick (fn [e]
                                 (.stopPropagation e)
                                 (share-modal-fn id title))
                      :color "lightblue"
                      :icon true
                      :labelPosition "right"
                      :floated "right"
                      :style {:margin-right 5}}
        "Share"
        [:> ui/Icon {:name "share"}]]
       title
       [:div {:style {:margin-top 3
                      :font-weight 400}} 
        [:small (apply str (interpose ", " (map :pname products)))]]]
      [bc/c-round-status status]]]))

(defn c-status-filter-checkboxes
  [rounds selected-statuses]
  (let [all-possible-statuses ["initiation" "in-progress" "complete"]
        statuses (->> rounds
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
                                               :_order_by {:created :desc}
                                               :deleted nil}
                                      [:id :idstr :created :status :title
                                       [:products {:ref-deleted nil}
                                        [:pname]]]]]}])
            share-modal-round-id& (r/atom nil)
            share-modal-round-title& (r/atom nil)
            share-modal-showing?& (r/atom false)
            share-modal-fn (fn [round-id round-title]
                             (reset! share-modal-round-id& round-id)
                             (reset! share-modal-round-title& round-title)
                             (reset! share-modal-showing?& true))]
        (fn []
          (if (= :loading @rounds&)
            [cc/c-loader]
            (let [unfiltered-rounds (:rounds @rounds&)]
              (if (seq unfiltered-rounds)
                [:div.container-with-sidebar
                 [:div.sidebar
                  [:> ui/Segment
                   [bc/c-start-round-button {:etype :none
                                             :props {:fluid true}}]]
                  [:> ui/Segment
                   [:h4 "Status"]
                   [c-status-filter-checkboxes unfiltered-rounds @selected-statuses&]]]
                 [:> ui/ItemGroup {:class "inner-container results"}
                  (let [rounds (cond-> unfiltered-rounds
                                 (seq @selected-statuses&) (filter-rounds @selected-statuses&))]
                    (for [round rounds]
                      ^{:key (:id round)}
                      [c-round round share-modal-fn]))]
                 [bc/c-share-modal @share-modal-round-id& @share-modal-round-title& share-modal-showing?&]]
                [:> ui/Grid
                 [:> ui/GridRow
                  [:> ui/GridColumn {:computer 2 :mobile 0}]
                  [:> ui/GridColumn {:computer 12 :mobile 16}
                   [:> ui/Segment {:placeholder true}
                    [:> ui/Header {:icon true}
                     [:> ui/Icon {:name "vetd"}]
                     "You don't have any active VetdRounds."]
                    [bc/c-start-round-button {:etype :none
                                              :props {:fluid true
                                                      :style {:margin-top 15}}}]]]
                  [:> ui/GridColumn {:computer 2 :mobile 0}]]]))))))))
