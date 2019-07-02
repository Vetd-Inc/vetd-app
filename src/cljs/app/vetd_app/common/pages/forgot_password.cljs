(ns vetd-app.common.pages.forgot-password
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

;; Events
(rf/reg-event-fx
 :nav-forgot-password
 (fn [_ [_ email-address]]
   {:nav {:path (str "/forgot-password/" email-address)}}))

(rf/reg-event-fx
 :route-forgot-password
 (fn [{:keys [db]} [_ email-address]]
   {:db (assoc db
               :page :forgot-password
               :page-params {:email-address email-address})
    :analytics/page {:name "Forgot Password"}}))

(rf/reg-event-fx
 :forgot-password.submit
 (fn [{:keys [db]} [_ email pwd cpwd]]
   (let [[bad-input message]
         (cond
           (not (re-matches #"^\S+@\S+\.\S+$" email)) [:email "Please enter a valid email address."]
           (< (count pwd) 8) [:pwd "Password must be at least 8 characters."]
           (not= pwd cpwd) [:cpwd "Password and Confirm Password must match."]
           :else nil)]
     (if bad-input
       {:db (assoc-in db [:page-params :bad-input] bad-input)
        :toast {:type "error" 
                :title "Error"
                :message message}}
       {:dispatch [:forgot-password.request-reset email pwd]}))))

(rf/reg-event-fx
 :forgot-password.request-reset
 (fn [{:keys [db]} [_ email pwd]]
   {:ws-send {:ws (:ws db)
              :payload {:cmd :forgot-password.request-reset
                        :return {:handler :forgot-password.request-reset-return
                                 :email email}
                        :email email
                        :pwd pwd}}}))

(rf/reg-event-fx
 :forgot-password.request-reset-return
 (fn [{:keys [db]} [_ results {{:keys [email]} :return}]]
   (if (:no-account? results)
     {:db (assoc-in db [:page-params :bad-input] :email)
      :toast {:type "error" 
              :title "Error"
              :message "That email address is not associated with an account."}}
     {:dispatch [:nav-login]
      :toast {:type "success"
              :title "Please check your email"
              :message (str "An email has been sent to \"" email "\" with a link to complete your password reset.")}
      :analytics/track {:event "Password Reset Requested"
                        :props {:category "Accounts"}}})))

;; Subscriptions
(rf/reg-sub
 :forgot-password-email-address
 :<- [:page-params] 
 (fn [{:keys [email-address]}] email-address))

(rf/reg-sub
 :bad-input
 :<- [:page-params]
 (fn [{:keys [bad-input]}] bad-input))

;; Components
(defn c-page []
  (let [email (r/atom (or @(rf/subscribe [:forgot-password-email-address]) ""))
        pwd (r/atom "")
        cpwd (r/atom "")
        bad-cpwd (r/atom false)
        bad-input& (rf/subscribe [:bad-input])]
    (fn []
      [:div.centerpiece
       [:a {:on-click #(rf/dispatch [:nav-login])}
        [:img.logo {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]]
       [:> ui/Header {:as "h2"
                      :class "teal"}
        "Forgot Password?"]
       [:> ui/Form {:style {:margin-top 25}}
        [:> ui/FormField {:error (= @bad-input& :email)}
         [:label "Work Email Address"
          [:> ui/Input {:class "borderless"
                        :defaultValue @email
                        :type "email"
                        :spellCheck false
                        :autoFocus true
                        :on-invalid #(.preventDefault %) ; no type=email error message (we'll show our own)
                        :on-change (fn [_ this]
                                     (reset! email (.-value this)))}]]]
        [:> ui/FormField {:error (= @bad-input& :pwd)}
         [:label "New Password"
          [:> ui/Input {:class "borderless"
                        :type "password"
                        :onChange (fn [_ this] (reset! pwd (.-value this)))}]]]
        [:> ui/FormField {:error (or @bad-cpwd
                                     (= @bad-input& :cpwd))}
         [:label "Confirm New Password"]
         [:> ui/Input {:class "borderless"
                       :type "password"
                       :on-blur #(when-not (= @cpwd @pwd)
                                   (reset! bad-cpwd true))
                       :on-change (fn [_ this]
                                    (reset! cpwd (.-value this))
                                    (when (= @cpwd @pwd)
                                      (reset! bad-cpwd false)))}]]
        [:> ui/Button {:color "teal"
                       :fluid true
                       :on-click #(rf/dispatch [:forgot-password.submit @email @pwd @cpwd])}
         "Send Reset Link via Email"]
        [:br] [:br]
        [:a {:on-click #(rf/dispatch [:nav-login])}
         "Back to Login"]]])))
