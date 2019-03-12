(ns vetd-app.signup
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :create-acct
 (fn [{:keys [db]} [_ [account]]]
   {:ws-send {:ws (:ws db)
              :payload (merge {:cmd :create-acct
                               :return :ws/create-acct}
                              (select-keys account [:uname :org-name :org-url
                                                    :org-type :email :pwd]))}}))
(rf/reg-event-fx
 :ws/create-acct
 (fn [{:keys [db]} [_ results]]
   (if-not (:email-used? results)
     {:dispatch [:nav-login]}
     (js/alert "Email already in use by another account."))))
