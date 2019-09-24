(ns vetd-app.common.pages.login
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.analytics :as analytics]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-sub
 :login-failed?
 (fn [{:keys [login-failed?]} _] login-failed?))

(rf/reg-sub
 :login-loading?
 (fn [{:keys [login-loading?]} _] login-loading?))

(rf/reg-event-fx
 :route-login
 [(rf/inject-cofx :local-store [:join-group-name])]
 (fn [{:keys [db local-store]}]
   {:db (assoc db
               :page :login
               :page-params {:join-group-name (:join-group-name local-store)})
    :analytics/page {:name "Login"}}))

(rf/reg-event-fx
 :nav-login
 (fn []
   {:nav {:path "/login"}}))

(rf/reg-event-fx
 :login
 (fn [{:keys [db]} [_ [email pwd]]]
   {:db (assoc db
               :login-failed? false
               :login-loading? true)
    :ws-send {:payload {:cmd :auth-by-creds
                        :return :login-result
                        :email email
                        :pwd pwd}}}))

(rf/reg-event-fx
 :login-result
 [(rf/inject-cofx :local-store [:join-group-link-key])]
 (fn [{:keys [db local-store]} [_ {:keys [logged-in? user session-token memberships
                                          admin-of-groups admin?]
                                   :as results}]]
   (if logged-in?
     (let [org-id (some-> memberships first :org-id)] ; TODO support users with multi-orgs
       {:db (assoc db
                   :login-failed? false
                   :logged-in? true
                   :user user
                   :session-token session-token
                   :memberships memberships
                   :active-memb-id (some-> memberships first :id)
                   :org-id org-id
                   :admin-of-groups admin-of-groups
                   ;; a Vetd employee with admin access?
                   :admin? admin?)
        :local-store {:session-token session-token}
        :cookies {:admin-token (when admin? [session-token {:max-age 60 :path "/"}])}
        :analytics/identify (analytics/identify-map user memberships admin-of-groups)
        :analytics/group {:group-id org-id
                          :traits {:name (some-> memberships first :org :oname)}}
        :dispatch-later [{:ms 100 :dispatch (if (:join-group-link-key local-store)
                                              [:read-link (:join-group-link-key local-store)]
                                              [:nav-home])}
                         ;; to prevent the login form from flashing briefly
                         {:ms 200 :dispatch [:hide-login-loading]}]})
     {:db (assoc db
                 :logged-in? false
                 :login-loading? false
                 :login-failed? true)})))

(rf/reg-event-db
 :hide-login-loading
 (fn [db]
   (assoc db :login-loading? false)))

(rf/reg-event-fx
 :logout
 (constantly
  {:local-store {:session-token nil}
   :cookies {:admin-token [nil {:max-age 60 :path "/"}]}
   :dispatch-n [[:init-db]
                [:nav-login]]}))

(rf/reg-event-db
 :clear-login-form
 (fn [db]
   (assoc db :login-failed? false)))

(defn c-page []
  (let [email (r/atom "")
        pwd (r/atom "")
        login-failed? (rf/subscribe [:login-failed?])
        login-loading? (rf/subscribe [:login-loading?])
        join-group-name& (rf/subscribe [:join-group-name])]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:clear-login-form])
      :reagent-render
      (fn []
        (if @login-loading?
          [cc/c-loader {:props {:style {:margin-top 175}}}]
          [:div.centerpiece
           [:img.logo {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]
           (when @join-group-name&
             [:> ui/Header {:as "h2"
                            :class "teal"}
              (str "Join the " @join-group-name& " community on Vetd")])
           [:> ui/Form {:error @login-failed?}
            (when @login-failed?
              [:> ui/Message {:error true
                              :header "Incorrect email / password"
                              :content "Contact us at help@vetd.com"}])
            [:> ui/FormField
             [ui/input {:class "borderless"
                        :value @email
                        :placeholder "Email Address"
                        :autoFocus true
                        :spellCheck false
                        :on-change #(reset! email (-> % .-target .-value))}]]
            [:> ui/FormField
             [:> ui/Input {:class "borderless"
                           :type "password"
                           :placeholder "Password"
                           :onChange (fn [_ this]
                                       (reset! pwd (.-value this)))}]
             [:div {:style {:float "right"
                            :margin-top 5
                            :margin-bottom 18}}
              [:a {:on-click #(rf/dispatch [:nav-forgot-password @email])
                   :style {:font-size 13
                           :opacity 0.75}}
               "Forgot Password?"]]]
            [:> ui/Button {:fluid true
                           :on-click #(rf/dispatch [:login [@email @pwd]])}
             "Log In"]
            [:> ui/Divider {:horizontal true
                            :style {:margin "20px 0"}}
             "Sign Up"]
            [:> ui/ButtonGroup {:fluid true}
             [:> ui/Button {:color "teal"
                            :on-click #(rf/dispatch [:nav-signup :buyer])}
              "As a Buyer"]
             [:> ui/ButtonOr]
             [:> ui/Button {:color "blue"
                            :on-click #(rf/dispatch [:nav-signup :vendor])}
              "As a Vendor"]]]]))})))
