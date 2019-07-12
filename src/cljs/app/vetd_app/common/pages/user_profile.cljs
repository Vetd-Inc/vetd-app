(ns vetd-app.common.pages.user-profile
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [vetd-app.common.components :as cc]
            [clojure.string :as s]))

(rf/reg-event-fx
 :nav-user-profile
 (fn [_ [_ email-address]]
   {:nav {:path (str "/profile/" email-address)}}))

(rf/reg-event-fx
 :route-user-profile
 (fn [{:keys [db]} _]
   {:db (assoc db
               :page :user-profile)
    :analytics/page {:name "User Profile"}}))

(rf/reg-event-fx
 :user-profile.save-name
 (fn [{:keys [db]} [_ user-id uname]]
   {:ws-send {:payload {:cmd :update-user
                        :user-id user-id
                        :uname uname}}}))

(rf/reg-event-fx
 :user-profile.save-password
 (fn [{:keys [db]} [_ user-id pwd new-pwd]]
   {:ws-send {:payload {:cmd :update-user-password
                        :user-id user-id
                        :pwd pwd
                        :new-pwd new-pwd}}}))

(defn c-page []
  (let [user-id& (rf/subscribe [:user-id])
        user& (rf/subscribe [:gql/q
                             {:queries
                              [[:users {:id @user-id&
                                        :deleted nil}
                                [:uname]]]}])
        new-uname& (r/atom nil)
        new-password& (r/atom nil)
        old-password& (r/atom nil)]
    (fn []
      (if (= :loading @user&)
        [cc/c-loader]
        (let [user (-> @user& :users first)]
          (when (nil? @new-uname&)
            (reset! new-uname& (:uname user)))
          [:div [:div "USER PROFILE"]
           (str user)
           [:div
            [ui/input {:value @new-uname&
                       :on-change (fn [this]
                                    (reset! new-uname& (-> this .-target .-value)))}]
            [:> ui/Button {:color "teal"
                           :on-click #(rf/dispatch [:user-profile.save-name @user-id& @new-uname&])}
             "Save Name"]]
           [:div
            [ui/input {:value @old-password&
                       :on-change (fn [this]
                                    (reset! old-password&
                                            (-> this .-target .-value)))}]
            [ui/input {:value @new-password&
                       :on-change (fn [this]
                                    (reset! new-password&
                                            (-> this .-target .-value)))}]
            [:> ui/Button {:color "teal"
                           :on-click #(rf/dispatch [:user-profile.save-password
                                                    @user-id&
                                                    @old-password&
                                                    @new-password&])}
             "Save Password"]]])))))
