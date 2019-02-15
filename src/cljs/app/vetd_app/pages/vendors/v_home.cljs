(ns vetd-app.pages.vendors.v-home
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
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

(defn new-preposal [{:keys [id title product from-org from-user docs] :as preq}]
  (let [pitch& (r/atom {})
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
       [:div "Pitch"
        [rc/input-textarea
         :model pitch&
         :on-change #(reset! pitch& %)]]
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

(defn c-req-without-preposal
  [{:keys [id title product from-org from-user docs] :as preq}]
  [flx/col #{:preposal-req}
   [:div "Preposal Request"]
   [flx/row
    [:div.info title]
    [:div.info (:pname product)]
    [:div.info (:oname from-org)]
    [:div.info (:uname from-user)]]
   [new-preposal preq]])

(defn c-req-with-preposal
  [{:keys [id title product from-org from-user docs] :as preq}]
  [flx/col #{:preposal}
   [:div "Preposal"]
   [flx/row
    [:div title]
    [:div (:pname product)]
    [:div (:oname from-org)]
    [:div (:uname from-user)]]])

(defn c-req-maybe-preposal
  [{:keys [id title product from-org from-user docs] :as preq}]
  (if (empty? docs)
    [c-req-without-preposal preq]
    [c-req-with-preposal preq]))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        prep-reqs& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:forms {:ftype "preposal"
                                             :to-org-id @org-id&}
                                     [:id :title
                                      [:product [:id :pname]]
                                      [:from-org [:id :oname]]
                                      [:from-user [:id :uname]]
                                      [:docs
                                       [:id :title]]
                                      [:prompts
                                       [:id :prompt :descr
                                        [:fields
                                         [:id :fname]]]]]]]}])]
    (fn []
      (def preq1 @prep-reqs&)
      [:div
       (for [preq (:forms @prep-reqs&)]
         ^{:key (str "form" (:id preq))}
         [c-req-maybe-preposal preq])])))

#_ (cljs.pprint/pprint preq1)
