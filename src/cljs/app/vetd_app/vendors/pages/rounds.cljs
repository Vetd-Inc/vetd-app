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


(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        prod-rounds& (rf/subscribe [:gql/q
                                    {:queries
                                     [[:products {:vendor-id @org-id&
                                                  :deleted nil}
                                       [:idstr :pname
                                        [:rounds {:deleted nil
                                                  :ref-deleted nil}
                                         [:id :idstr :status :created
                                          [:buyer [:oname]]]]]]]}])]
    (fn []
      (let [prod-rounds (->> (for [{:keys [rounds] p-idstr :idstr :as p} (:products @prod-rounds&)
                                   {r-idstr :idstr :as r} rounds]
                               (-> p
                                   (assoc :product-idstr p-idstr
                                          :round-idstr r-idstr)
                                   (dissoc :rounds)
                                   (merge r)))
                             (sort-by :created)
                             reverse
                             (group-by :status))]
        [:div
         (for [[k-status rounds] prod-rounds]
           [:div
            [:div {:style {:background-color "#333"
                           :color "#FFF"
                           :margin "10px"
                           :padding "10px"
                           :font-size "x-large"}}
             k-status]
            (for [{:keys [round-idstr product-idstr pname status created buyer]} rounds]
              [:div {:on-click #(rf/dispatch [:v/nav-round-product-detail round-idstr product-idstr])
                     :style {:background-color "#DDD"
                             :cursor "pointer"
                             :margin-left "30px"
                             :padding "10px"
                             :font-size "large"}}
               [:span {:style {:padding "30px"}} (:oname buyer)]
               [:span {:style {:padding "30px"}} pname]
               (.toString (js/Date. created))])])]))))
