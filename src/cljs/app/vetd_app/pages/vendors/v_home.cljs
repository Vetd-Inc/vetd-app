(ns vetd-app.pages.vendors.v-home
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :v/nav-home
 (fn [{:keys [db]} _]
   {:nav {:path "/v/home/"}}))

(rf/reg-event-db
 :v/route-home
 (fn [db [_ query-params]]
   (assoc db
          :page :v/home
          :query-params query-params)))

(rf/reg-event-fx
 :v/create-preposal
 (fn [{:keys [db]} prep-def]
   {:ws-send {:payload (merge {:cmd :v/create-preposal
                               :return nil}
                              prep-def)}}))

(defn search-results
  [orgs q]
  (->> (for [org (:orgs orgs)
             {:keys [user]} (:memberships org)]
         {:label (str (:oname org) " / " (:uname user))
          :org-id (:id org)
          :user-id (:id user)})
       (filter (fn [{:keys [label]}]
                 (re-find (re-pattern (str "(?i)" q))
                          label))))
  #_[{:label "hello" :value 44}])

(defn new-preposal [orgs]
  (let [model& (r/atom {})
        org-user& (r/atom nil)
        pitch& (r/atom "")
        price-val& (r/atom "")
        price-unit& (r/atom :year)
        orgs& (rf/subscribe [:gql/q
                             {:queries
                              [[:orgs {:buyer? true}
                                [:id :oname :idstr :short-desc
                                 [:memberships
                                  [:id
                                   [:user
                                    [:id :idstr :uname]]]]]]]}])]
    (fn []
      [:div "NEW PREPOSAL"
       [rc/typeahead
        :data-source (partial search-results @orgs&)
        :model model&
        :render-suggestion (fn [{:keys [label]}]
                             [:span label])
        :suggestion-to-string #(:label %)
        :rigid? true
        :on-change #(reset! org-user& %)]
       [:div "Pitch"
        [rc/input-textarea
         :model pitch&
         :on-change #(reset! model& %)]]
       "Price Estimate "
       [rc/input-text
        :model price-val&
        :on-change #(reset! price-val& %)
        :validation-regex #"^\d*$"]
       " per "
       [rc/single-dropdown
        :model price-unit&
        :on-change #(reset! price-unit& %)
        :choices [{:id :year :label "year"}
                  {:id :month :label "month"}]]
       [rc/button
        :label "Save"
        :on-click (fn []
                     (let [{:keys [org-id user-id]} @org-user&]
                       (rf/dispatch [:v/create-preposal {:buyer-org-id org-id
                                                       :buyer-user-id user-id
                                                       :pitch @pitch&
                                                       :price-val @price-val&
                                                       :price-unit @price-unit&}])))]])))

(defn c-page []
  (let [orgs& (rf/subscribe [:gql/q
                             {:queries
                              [[:orgs {:buyer? true}
                                [:id :oname :idstr :short-desc
                                 [:memberships
                                  [:id
                                   [:user
                                    [:id :idstr :uname]]]]]]]}])]
    (fn []
      [:div "VENDORS' HOME"
       [new-preposal @orgs&]])))
