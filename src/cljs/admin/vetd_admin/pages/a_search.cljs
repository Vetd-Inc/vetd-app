(ns vetd-admin.pages.a-search
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]   
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))


(def last-query-id (atom 0))

(defn get-next-query-id []
  (swap! last-query-id inc))

(rf/reg-event-fx
 :a/nav-search
 (fn [{:keys [db]} _]
   {:nav {:path "/a/search/"}}))

(rf/reg-event-db
 :a/route-search
 (fn [db [_ query-params]]
   (assoc db
          :page :a/search
          :query-params query-params)))

(def dispatch-search-DB
  (goog.functions.debounce
   #(do (rf/dispatch [:a/search %]))
   250))


(rf/reg-event-fx
 :a/search
 (fn [{:keys [db ws]} [_ q-str q-type]]
   (let [qid (get-next-query-id)]
     {:db (assoc db
                 :buyer-qid qid)
      :ws-send {:payload {:cmd :a/search
                          :return {:handler :a/ws-search-result-ids
                                   :qid qid}
                          :query q-str
                          :qid qid}}})))

(rf/reg-event-fx
 :a/login-as-support
 (fn [{:keys [db]} [_ org-id]]
   (let [qid (get-next-query-id)]
     {:db (assoc db
                 :org-id org-id)
      :dispatch [:v/nav-home]})))

(rf/reg-event-fx
 :a/create-membership
 (fn [{:keys [db]} [_ user-id org-id]]
   (let [qid (get-next-query-id)]
     {:db (assoc db
                 :buyer-qid qid)
      :ws-send {:payload {:cmd :create-membership
                          :return nil
                          :user-id user-id
                          :org-id org-id}}})))

(rf/reg-event-fx
 :a/delete-membership
 (fn [{:keys [db]} [_ memb-id]]
   (let [qid (get-next-query-id)]
     {:db (assoc db
                 :buyer-qid qid)
      :ws-send {:payload {:cmd :delete-membership
                          :return nil
                          :id memb-id}}})))

(rf/reg-sub
 :a/search-result-ids
  (fn [db _]
    (:search-result-ids db)))

(rf/reg-event-fx
 :a/ws-search-result-ids
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   (def res1 results)
   #_ (println res1)
   (def ret1 return)
   (if (= (:buyer-qid db) (:qid return))
     {:db (assoc db
                 :search-result-ids
                 results)}
     {})))

(defn c-org-search-result
  [{:keys [id oname memberships]}]
  (let [user-id& (rf/subscribe [:user-id])]
    (fn [{:keys [id oname memberships]}]
      [:div {:class :org-search-result}
       [:div oname]
       (if (empty? memberships)
         [rc/button
          :label "Join Org"
          :on-click #(rf/dispatch [:a/create-membership @user-id& id])]
         [:div
          [rc/button
           :label "Login as Support User"
           :on-click #(rf/dispatch [:a/login-as-support id])]
          [rc/button
           :label "Leave Org"
           :on-click #(rf/dispatch [:a/delete-membership (->> memberships
                                                              first
                                                              :id)])]])])))

(defn c-search-results []
  (let [user-id @(rf/subscribe [:user-id])
        {:keys [org-ids] :as ids} @(rf/subscribe [:a/search-result-ids])
        orgs (if (not-empty org-ids)
               (:orgs
                @(rf/subscribe [:gql/sub
                                {:queries
                                 [[:orgs {:id org-ids}
                                   [:id :oname :idstr
                                    [:memberships {:user-id user-id
                                                   :deleted nil}
                                     [:id]]]]]}]))
                [])]
    [:div {:class :search-results}
     [:div {:class :orgs}
      (for [o orgs]
        ^{:key (:id o)}
        [c-org-search-result o])]]))



(defn c-page []
  (let [search-query (r/atom "")]
    (fn []
      [rc/v-box
       :style {:align-items :center}
       :children [[rc/input-text
                   :model search-query
                   :attr {:auto-focus true}
                   :width "50%"
                   :on-change #(do
                                 (dispatch-search-DB %)
                                 (reset! search-query %))
                   :change-on-blur? false
                   :placeholder "Search orgs"]
                  [c-search-results]]])))
