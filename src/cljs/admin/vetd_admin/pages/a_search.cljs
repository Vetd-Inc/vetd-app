(ns vetd-admin.pages.a-search
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]))


(def last-query-id (atom 0))

(defn get-next-query-id []
  (swap! last-query-id inc))

(rf/reg-event-fx
 :a/nav-search
 (fn [{:keys [db]} _]
   {:nav {:path "/a/search"}}))

(rf/reg-event-db
 :a/route-search
 (fn [db [_ query-params]]
   (assoc db
          :page :a/search
          :query-params query-params)))



(rf/reg-event-fx
 :a/update-search-term
 (fn [{:keys [db]} [_ search-term]]
   {:db (assoc db :search-term search-term)
    :dispatch-debounce [{:id :a/search
                         :dispatch [:a/search search-term]
                         :timeout 250}]}))

(rf/reg-event-fx
 :a/search
 (fn [{:keys [db ws]} [_ q-str q-type]]
   (let [qid (get-next-query-id)]
     {:db (assoc db :buyer-qid qid)
      :ws-send {:payload {:cmd :a/search
                          :return {:handler :a/ws-search-result-ids
                                   :qid qid}
                          :query q-str
                          :qid qid}}})))

(rf/reg-event-fx
 :a/ws-search-result-ids
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   (if (= (:buyer-qid db) (:qid return))
     {:db (assoc db :a/search-result-ids results)}
     {})))

(rf/reg-sub
 :a/search-result-ids
 (fn [db _]
   (:a/search-result-ids db)))

(rf/reg-sub
 :search-term
 (fn [{:keys [search-term]}] search-term))

(rf/reg-event-fx
 :a/login-as-support
 (fn [{:keys [db]} [_ org-id]]
   (let [qid (get-next-query-id)]
     {:db (assoc db
                 :org-id org-id)
      :dispatch [:v/nav-preposals]})))

(rf/reg-event-fx
 :a/create-membership
 (fn [{:keys [db]} [_ user-id org-id]]
   (let [qid (get-next-query-id)]
     {:db (assoc db
                 :buyer-qid qid)
      :ws-send {:payload {:cmd :switch-membership
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

(defn c-org-search-result
  [{:keys [id oname memberships]}]
  (let [user-id& (rf/subscribe [:user-id])]
    (fn [{:keys [id oname memberships]}]
      [:div {:style {:margin-bottom 30}}
       [:h4 {:style {:margin-bottom 8}}
        oname]
       (if (empty? memberships)
         [:> ui/Button {:onClick #(rf/dispatch [:a/create-membership @user-id& id])
                        :size "tiny"
                        :color "teal"}
          "Join Organization"]
         [:<>
          [:> ui/Button {:onClick #(rf/dispatch [:a/login-as-support id])
                         :size "tiny"
                         :color "blue"}
           "Login as Support User"]
          [:> ui/Button {:onClick #(rf/dispatch [:a/delete-membership
                                                 (->> memberships first :id)])
                         :size "tiny"
                         :color "red"}
           "Leave Organization"]])])))

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
  (let [search-query& (rf/subscribe [:search-term])]
    (fn []
      [:> ui/Grid
       [:> ui/GridRow
        [:> ui/GridColumn {:computer 4 :mobile 0}]
        [:> ui/GridColumn {:computer 8 :mobile 16}
         [:> ui/Input {:class "product-search"
                       :value @search-query&
                       :size "big"
                       :icon "search"
                       :autoFocus true
                       :spellCheck false
                       :onChange (fn [_ this]
                                   (rf/dispatch [:a/update-search-term (.-value this)]))
                       :placeholder "Search for organizations..."}]]
        [:> ui/GridColumn {:computer 4 :mobile 0}]]
       [:> ui/GridRow
        [:> ui/GridColumn {:computer 2 :mobile 0}]
        [:> ui/GridColumn {:computer 12 :mobile 16}
         [c-search-results]]
        [:> ui/GridColumn {:computer 2 :mobile 0}]]])))
