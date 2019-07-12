(ns vetd-app.orgs.pages.users
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [vetd-app.common.components :as cc]
            [clojure.string :as s]))

(rf/reg-event-fx
 :o/nav-users
 (fn [_ [_ email-address]]
   {:nav {:path "/o/users/"}}))

(rf/reg-event-fx
 :o/route-users
 (fn [{:keys [db]} _]
   {:db (assoc db
               :page :o/users)
    :analytics/page {:name "Org Users"}}))

(rf/reg-event-fx
 :o/invite-user-to-org
 (fn [{:keys [db]} [_ email org-id from-user-id]]
   {:ws-send {:payload {:cmd :invite-user-to-org
                        :email email
                        :org-id org-id
                        :from-user-id from-user-id }}}))

(defn c-page []
  (let [user-id& (rf/subscribe [:user-id])
        org-id& (rf/subscribe [:org-id])
        org& (rf/subscribe [:gql/q
                            {:queries
                             [[:orgs {:id @org-id&
                                      :deleted nil}
                               [:oname
                                [:memberships
                                 [[:user
                                   [:id :idstr :uname :email]]]]]]]}])
        email& (r/atom "")]
    (fn []
      (if (= :loading @org&)
        [cc/c-loader]
        (let [org (-> @org& :orgs first)]
          [:div (str org)
           [:div
            [ui/input {:value @email&
                       :on-change (fn [this]
                                    (reset! email& (-> this .-target .-value)))}]
            [:> ui/Button {:color "teal"
                           :on-click #(rf/dispatch [:o/invite-user-to-org
                                                    @email&
                                                    @org-id&
                                                    @user-id&])}
             "Save Name"]]])))))
