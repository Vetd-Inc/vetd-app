(ns vetd-app.common.pages.join-org-signup
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

;; Events
(rf/reg-event-fx
 :nav-join-org-signup
 (fn [_ [_ org-name]]
   {:nav {:path (str "/signup-by-invite/" org-name)}
    :analytics/track {:event "Signup Start"
                      :props {:category "Accounts"
                              :label "By Invite"}}}))

(rf/reg-event-fx
 :route-join-org-signup
 (fn [{:keys [db]} [_ org-type]]
   {:db (assoc db :page :route-join-org-signup)
    :analytics/page {:name "Signup By Invite"}}))

(rf/reg-event-fx
 :signup.submit
 (fn [{:keys [db]} [_ {:keys [uname email pwd cpwd org-name terms-agree] :as account}]]
   (let [[bad-input message]
         (cond
           (not (re-matches #".+\s.+" uname)) [:uname "Please enter your full name (first & last)."]
           (not (re-matches #"^\S+@\S+\.\S+$" email)) [:email "Please enter a valid email address."]
           (< (count pwd) 8) [:pwd "Password must be at least 8 characters."]
           (not= pwd cpwd) [:cpwd "Password and Confirm Password must match."]
           (not terms-agree) [:terms-agree "You must agree to the Terms of Use in order to sign up."]
           :else nil)]
     (if bad-input
       {:db (assoc-in db [:page-params :bad-input] bad-input)
        :toast {:type "error" 
                :title "Error"
                :message message}}
       {:dispatch [:create-acct account]}))))

(rf/reg-event-fx
 :create-acct
 (fn [{:keys [db]} [_ account]]
   {:ws-send {:ws (:ws db)
              :payload (merge {:cmd :create-acct
                               :return {:handler :create-acct-return
                                        :org-type (:org-type account)
                                        :email (:email account)}}
                              (select-keys account [:uname :org-name :org-url
                                                    :org-type :email :pwd]))}}))
(rf/reg-event-fx
 :create-acct-return
 (fn [{:keys [db]} [_ results {{:keys [org-type email]} :return}]]
   (if-not (:email-used? results)
     {:dispatch [:nav-login]
      :toast {:type "success"
              :title "Please check your email"
              :message (str "We've sent an email to " email " with a link to activate your account.")}
      :analytics/track {:event "Signup Complete"
                        :props {:category "Accounts"
                                :label org-type}}}
     {:db (assoc-in db [:page-params :bad-input] :email)
      :toast {:type "error" 
              :title "Error"
              :message "There is already an account with that email address."}})))

;; Subscriptions
(rf/reg-sub
 :signup-org-type
 :<- [:page-params] 
 (fn [{:keys [org-type]}] org-type))

;; Components
(defn c-page []
  (let [uname (r/atom "")
        email (r/atom "")
        pwd (r/atom "")
        cpwd (r/atom "")
        bad-cpwd (r/atom false)
        terms-agree (r/atom false)
        bad-input& (rf/subscribe [:bad-input])]
    (fn []
      [:div.centerpiece
       [:a {:on-click #(rf/dispatch [:nav-login])}
        [:img.logo {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]]
       [:> ui/Header {:as "h2"
                      :class "blue"}
        "Sign Up to Join ORG on Vetd"]
       [:> ui/Form {:style {:margin-top 25}}
        [:> ui/FormField {:error (= @bad-input& :uname)}
         [:label "Full Name"
          [:> ui/Input {:class "borderless"
                        :spellCheck false
                        :onChange (fn [_ this] (reset! uname (.-value this)))}]]]
        [:> ui/FormField {:error (= @bad-input& :email)}
         [:label "Work Email Address"
          [:> ui/Input {:class "borderless"
                        :type "email"
                        :disabled true
                        :spellCheck false
                        :value "PREFILL"}]]]
        [:> ui/FormField {:error (= @bad-input& :pwd)}
         [:label "Password"
          [:> ui/Input {:class "borderless"
                        :type "password"
                        :onChange (fn [_ this] (reset! pwd (.-value this)))}]]]
        [:> ui/FormField {:error (or @bad-cpwd
                                     (= @bad-input& :cpwd))}
         [:label "Confirm Password"]
         [:> ui/Input {:class "borderless"
                       :type "password"
                       :on-blur #(when-not (= @cpwd @pwd)
                                   (reset! bad-cpwd true))
                       :on-change (fn [_ this]
                                    (reset! cpwd (.-value this))
                                    (when (= @cpwd @pwd)
                                      (reset! bad-cpwd false)))}]]
        [:> ui/FormField {:error (= @bad-input& :terms-agree)
                          :style {:margin "25px 0 20px 0"}}
         [:> ui/Checkbox {:label "I agree to the Terms Of Use"
                          :onChange (fn [_ this] (reset! terms-agree (.-checked this)))}]
         [:a {:href "https://vetd.com/terms-of-use"
              :target "_blank"}
          " (read)"]]
        [:> ui/Button {:color "blue"
                       :fluid true
                       :on-click #(rf/dispatch [:signup.submit
                                                {:uname @uname
                                                 :email @email
                                                 :pwd @pwd
                                                 :cpwd @cpwd
                                                 :terms-agree @terms-agree}])}
         "Sign Up"]]])))
