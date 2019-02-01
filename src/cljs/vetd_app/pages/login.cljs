(ns vetd-app.pages.login
  (:require [vetd-app.util :as ut]
            [vetd-app.blocker :as bl]
            [vetd-app.websockets :as ws]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))


(rf/reg-sub
 :login-failed?
 (fn [{:keys [login-failed?]} _] login-failed?))

(rf/reg-event-db
 :route-login
 (fn [db [_ query-params]]
   (assoc db
          :page :login)))

(rf/reg-event-fx
 :nav-login
 (fn [_ _]
   {:nav {:path "/login"}}))


(rf/reg-event-fx
 :login
 (fn [{:keys [db]} [_ [email pwd]]]
   {:ws-send {:payload {:cmd :auth-by-creds
                        :return :ws/login
                        :email email
                        :pwd pwd}}}))

(rf/reg-event-fx
 :ws/login
 (fn [{:keys [db]} [_ {:keys [logged-in? user session-token memberships] :as results}]]
   (def res1 results)
   (if logged-in?
     {:db (assoc db
                 :login-failed? false
                 :logged-in? logged-in?
                 :user user
                 :session-token session-token
                 :memberships memberships
                 ;; TODO support users with multi-orgs
                 :org-id (-> memberships first :org-id))
      :local-store {:session-token session-token}
      :dispatch-later [{:ms 100 :dispatch [:nav-home]}]}
     {:db (assoc db
                 :logged-in? logged-in?
                 :login-failed? true)})))

(defn login-page []
  (let [email (r/atom "")
        pwd (r/atom "")
        login-failed? (rf/subscribe [:login-failed?])]
    (fn []
      [:div {:id :login-form}
       [:div (when @login-failed?
               "LOGIN FAILED")]
       [rc/input-text
        :model email
        :on-change #(reset! email %)
        :placeholder "Your Email"]   
       [rc/input-password
        :model pwd
        :on-change #(reset! pwd %)
        :placeholder "Password"]
       [rc/button
        :on-click #(rf/dispatch [:login [@email @pwd]])
        :label "Login"]
       [rc/button
        :on-click #(rf/dispatch [:nav-signup])
        :label "Sign Up"]])))

