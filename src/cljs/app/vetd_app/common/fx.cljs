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
   { ;; :db (assoc db :page :login) ; add some kind of loading control
    :ws-send {:payload {:cmd :read-link
                        :return :read-link-result
                        :key k}}}))

(rf/reg-event-fx
 :read-link-result
 (fn [{:keys [db]} [_ {:keys [cmd output-data] :as results}]]
   (case cmd ; make sure your case nav's the user somewhere (often :nav-home)
     :create-verified-account {:toast {:type "success"
                                       :title "Account Verified!"
                                       :message "Thank you for verifying your email address."}
                               :local-store {:session-token (:session-token output-data)}
                               :dispatch-later [{:ms 100 :dispatch [:ws-get-session-user]}
                                                {:ms 200 :dispatch [:nav-home]}]}
     {:toast {:type "error"
              :title "That link is expired or invalid."}
      :dispatch [:nav-home]})))


;; (if logged-in?
;;   (let [org-id (-> memberships first :org-id)] ; TODO support users with multi-orgs
;;     {:db (assoc db
;;                 :login-failed? false
;;                 :logged-in? true
;;                 :user user
;;                 :session-token session-token
;;                 :memberships memberships
;;                 :active-memb-id (some-> memberships first :id)
;;                 :admin? admin?
;;                 :org-id org-id)
;;      :local-store {:session-token session-token}
;;      :cookies {:admin-token (when admin? [session-token {:max-age 60 :path "/"}])}
;;      :analytics/identify {:user-id (:id user)
;;                           :traits {:name (:uname user)
;;                                    :displayName (:uname user)
;;                                    :email (:email user)}}
;;      :analytics/group {:group-id org-id
;;                        :traits {:name (-> memberships first :org :oname)}}
;;      :dispatch-later [{:ms 100 :dispatch [:nav-home]}
;;                       ;; to prevent the login form from flashing briefly
;;                       {:ms 200 :dispatch [:hide-login-loading]}]})
;;   {:db (assoc db
;;               :logged-in? false
;;               :login-loading? false
;;               :login-failed? true)})
