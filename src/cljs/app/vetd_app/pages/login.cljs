(ns vetd-app.pages.login
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-sub
 :login-failed?
 (fn [{:keys [login-failed?]} _] login-failed?))

(rf/reg-event-db
 :route-login
 (fn [db]
   (assoc db :page :login)))

(rf/reg-event-fx
 :nav-login
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
 (fn [{:keys [db]} [_ {:keys [logged-in? user session-token memberships admin?]
                       :as results}]]
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

(rf/reg-event-fx
 :logout
 (constantly
  {:local-store {:session-token nil}
   :cookies {:admin-token [nil {:max-age 60 :path "/"}]}
   :dispatch [:nav-login]}))

(rf/reg-event-db
 :clear-login-form
 (fn [db]
   (assoc db :login-failed? false)))

(defn login-page []
  (let [email (r/atom "")
        pwd (r/atom "")
        login-failed? (rf/subscribe [:login-failed?])]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:clear-login-form])
      :reagent-render
      (fn []
        [:div.centerpiece
         [:img.logo {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]
         [:> ui/Form {:error @login-failed?}
          (when @login-failed?
            [:> ui/Message {:error true
                            :header "Incorrect email/password"
                            :content "Contact us at help@vetd.com"}])
          [:> ui/FormField
           [:> ui/Input {:placeholder "Email Address"
                         :autoFocus true
                         :spellCheck false
                         :onChange (fn [_ this]
                                     (reset! email (.-value this)))}]]
          [:> ui/FormField
           [:> ui/Input {:type "password"
                         :placeholder "Password"
                         :onChange (fn [_ this]
                                     (reset! pwd (.-value this)))}]]
          [:> ui/Button {:fluid true
                         :on-click #(rf/dispatch [:login [@email @pwd]])}
           "Log In"]
          [:> ui/Divider {:horizontal true
                          :style {:margin "20px 0"}}
           "Sign Up"]
          [:> ui/ButtonGroup {:fluid true}
           [:> ui/Button {:color "teal"
                          :on-click #(rf/dispatch [:b/nav-signup])}
            "As a Buyer"]
           [:> ui/ButtonOr]
           [:> ui/Button {:color "blue"
                          :on-click #(rf/dispatch [:v/nav-signup])}
            "As a Vendor"]]]])})))
