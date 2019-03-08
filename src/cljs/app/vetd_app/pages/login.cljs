(ns vetd-app.pages.login
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-sub
 :login-failed?
 (fn [{:keys [login-failed?]} _] login-failed?))

(rf/reg-event-db
 :pub/route-login
 (fn [db [_ query-params]]
   (assoc db
          :page :pub/login)))

(rf/reg-event-fx
 :pub/nav-login
 (fn []
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
 (fn [{:keys [db]} [_ {:keys [logged-in? user session-token memberships admin?] :as results}]]
   (if logged-in?
     {:db (assoc db
                 :login-failed? false
                 :logged-in? logged-in?
                 :user user
                 :session-token session-token
                 :memberships memberships
                 :active-memb-id (some-> memberships first :id)
                 :admin? admin?
                 ;; TODO support users with multi-orgs
                 :org-id (-> memberships first :org-id))
      :local-store {:session-token session-token}
      :cookies {:admin-token (when admin?
                               [session-token {:max-age 60
                                               :path "/"}])}
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
       [:img {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"
              :style {:width 210
                      :marginBottom 30}}]
       [:> ui/Form
        [:div (when @login-failed?
                "LOGIN FAILED")]
        [:> ui/FormField
         [:> ui/Input {:placeholder "Your Email"
                       :onChange (fn [_ this]
                                   (reset! email (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:placeholder "Password"
                       :onChange (fn [_ this]
                                   (reset! pwd (.-value this)))}]]
        [:> ui/Button {:on-click #(rf/dispatch [:login [@email @pwd]])}
         "Log In"]
        [:> ui/Divider {:horizontal true} "Or"]
        [:> ui/Button {:color "blue"
                       :on-click #(rf/dispatch [:pub/nav-signup])}
         "Sign Up"]]])))
