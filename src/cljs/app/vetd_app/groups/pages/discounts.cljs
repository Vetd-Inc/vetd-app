(ns vetd-app.groups.pages.discounts
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [vetd-app.common.components :as cc]
            [clojure.string :as s]))


(rf/reg-event-fx
 :g/nav-discounts
 (fn [_ [_ email-address]]
   {:nav {:path "/g/discounts/"}}))

(rf/reg-event-fx
 :g/route-discounts
 (fn [{:keys [db]} _]
   {:db (assoc db
               :page :g/discounts)
    :analytics/page {:name "Group Discounts"}}))

(rf/reg-event-fx
 :g/invite-user-to-org
 (fn [{:keys [db]} [_ email org-id from-user-id]]
   {:ws-send {:payload {:cmd :invite-user-to-org
                        :email email
                        :org-id org-id
                        :from-user-id from-user-id }}}))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        groups& (rf/subscribe [:gql/q
                               {:queries
                                [[:groups {:admin-org-id @org-id&
                                           :deleted nil}
                                  [[:discounts
                                    [:id :idstr :pname
                                     :group-discount-descr]]]]]}])]
    (fn []
      (def g1 @groups&)
      (if (= :loading @groups&)
        [cc/c-loader]
        (let [discounts (some-> @groups& :groups first :discounts)]
          [:div (str discounts)])))))

