(ns vetd-app.signup
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :create-acct
 (fn [{:keys [db]} [_ [account]]]
   {:ws-send {:ws (:ws db)
              :payload (merge {:cmd :create-acct
                               :return {:handler :ws/create-acct
                                        :org-type (:org-type account)}}
                              (select-keys account [:uname :org-name :org-url
                                                    :org-type :email :pwd]))}}))
(rf/reg-event-fx
 :ws/create-acct
 (fn [{:keys [db]} [_ results {{:keys [org-type]} :return}]]
   (if-not (:email-used? results)
     {:dispatch [:nav-login]
      :analytics/track {:event "Signup Complete"
                        :props {:category "Accounts"
                                :label org-type}}}
     (js/alert "Email already in use by another account."))))
