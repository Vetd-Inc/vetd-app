(ns vetd-app.groups.pages.orgs
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [vetd-app.common.components :as cc]
            [clojure.string :as s]))

(rf/reg-event-fx
 :g/nav-orgs
 (fn [_ [_ email-address]]
   {:nav {:path "/c/orgs"}}))

(rf/reg-event-fx
 :g/route-orgs
 (fn [{:keys [db]} _]
   {:db (assoc db
               :page :g/orgs)
    :analytics/page {:name "Group Orgs"}}))


(rf/reg-event-fx
 :g/invite-user-to-org
 (fn [{:keys [db]} [_ email org-id from-user-id]]
   {:ws-send {:payload {:cmd :invite-user-to-org
                        :email email
                        :org-id org-id
                        :from-user-id from-user-id}}}))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        groups& (rf/subscribe [:gql/q
                               {:queries
                                [[:groups {:admin-org-id @org-id&
                                           :deleted nil}
                                  [[:orgs
                                    [:id :idstr :oname]]]]]}])]
    (fn []
      (def g1 @groups&)
      (if (= :loading @groups&)
        [cc/c-loader]
        (let [orgs (some-> @groups& :groups first :orgs)]
          [:div (str orgs)])))))

   
