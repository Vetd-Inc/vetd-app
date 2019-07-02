(ns vetd-app.common.fx
  (:require [re-frame.core :as rf]))

(rf/reg-fx
 :confetti
 (fn [_]
   (.startConfetti js/window)
   (js/setTimeout #(.stopConfetti js/window) 3000)))

(rf/reg-event-fx
 :read-link
 (fn [{:keys [db]} [_ k]]
   {:ws-send {:payload {:cmd :read-link
                        :return :read-link-result
                        :key k}}}))

(rf/reg-event-fx
 :read-link-result
 (fn [{:keys [db]} [_ {:keys [cmd output-data] :as results}]]
   (case cmd ; make sure your case nav's the user somewhere (often :nav-home)
     :create-verified-account {:toast {:type "success"
                                       :title "Account Verified"
                                       :message "Thank you for verifying your email address."}
                               :local-store {:session-token (:session-token output-data)}
                               :dispatch-later [{:ms 100 :dispatch [:ws-get-session-user]}
                                                {:ms 200 :dispatch [:nav-home]}]}
     :password-reset {:toast {:type "success"
                              :title "Password Updated"
                              :message "Your password has been successfully updated."}
                      :local-store {:session-token (:session-token output-data)}
                      :dispatch-later [{:ms 100 :dispatch [:ws-get-session-user]}
                                       {:ms 200 :dispatch [:nav-home]}]}
     {:toast {:type "error"
              :title "That link is expired or invalid."}
      :dispatch [:nav-home]})))
