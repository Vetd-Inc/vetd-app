(ns vetd-app.vendors.pages.rounds
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :v/nav-rounds
 (constantly
  {:nav {:path "/v/rounds"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Vendor Rounds"}}}))

(rf/reg-event-fx
 :v/route-rounds
 (fn [{:keys [db]}]
   {:db (assoc db :page :v/rounds)}))

(defn c-round
  [{:keys [id round-idstr product-idstr pname status created buyer]}]
  (let [nav-click (fn [e]
                    (.stopPropagation e)
                    (rf/dispatch [:v/nav-round-product-detail round-idstr product-idstr]))]
    [:> ui/Item {:onClick nav-click}
     [:> ui/ItemContent
      [:> ui/ItemHeader
       [:> ui/Button {:onClick nav-click
                      :color "blue"
                      :icon true
                      :labelPosition "right"
                      :floated "right"}
        "Manage Responses"
        [:> ui/Icon {:name "right arrow"}]]
       (:oname buyer)
       [:div {:style {:margin-top 3
                      :font-weight 400}} 
        [:small
         "Product: " pname
         [:br]
         (.toString (js/Date. created))]]]]]))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        prod-rounds& (rf/subscribe [:gql/q
                                    {:queries
                                     [[:products {:vendor-id @org-id&
                                                  :deleted nil}
                                       [:idstr :pname
                                        [:rounds {:status "in-progress"
                                                  :deleted nil
                                                  :ref-deleted nil}
                                         [:id :idstr :status :created
                                          [:buyer [:oname]]]]]]]}])]
    (fn []
      (let [product-rounds (->> (for [{:keys [rounds]
                                       p-idstr :idstr
                                       :as p} (:products @prod-rounds&)
                                      {r-idstr :idstr
                                       :as r} rounds]
                                  (-> p
                                      (assoc :product-idstr p-idstr
                                             :round-idstr r-idstr)
                                      (dissoc :rounds)
                                      (merge r)))
                                (sort-by :created)
                                reverse)]
        [:> ui/ItemGroup {:class "results"}
         (for [round product-rounds]
           ^{:key (:id round)}
           [c-round round])]))))
