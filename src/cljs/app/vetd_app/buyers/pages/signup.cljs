(ns vetd-app.buyers.pages.signup
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]))

;; Events
(rf/reg-event-fx
 :b/nav-signup
 (constantly
  {:nav {:path "/b/signup"}
   :analytics/track {:event "Signup Start"
                     :props {:category "Accounts"
                             :label "buyer"}}}))

(rf/reg-event-fx
 :b/route-signup
 (fn [{:keys [db]}]
   {:db (assoc db :page :b/signup)
    :analytics/page {:name "Buyers Signup"}}))

;; Components
(defn c-page []
  (let [uname (r/atom "")
        email (r/atom "")
        org-name (r/atom "")
        org-url (r/atom "")        
        pwd (r/atom "")
        cpwd (r/atom "")
        terms-agree (r/atom false)]
    (fn []
      [:div.centerpiece
       [:img.logo {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]
       [:> ui/Form
        [:> ui/Header {:as "h2"}
         "Sign Up as a Buyer"]
        [:> ui/FormField
         [:> ui/Input {:class "borderless"
                       :placeholder "Full Name"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! uname (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:class "borderless"
                       :type "email"
                       :placeholder "Email Address"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! email (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:class "borderless"
                       :placeholder "Organization Name"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! org-name (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:class "borderless"
                       :type "url"
                       :label "http://"
                       :placeholder "Organization Website"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! org-url (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:class "borderless"
                       :type "password"
                       :placeholder "Password"
                       :onChange (fn [_ this] (reset! pwd (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:class "borderless"
                       :type "password"
                       :placeholder "Confirm Password"
                       :onChange (fn [_ this] (reset! cpwd (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Checkbox {:label "I agree to the Terms Of Use"
                          :onChange (fn [_ this] (reset! terms-agree (.-checked this)))}]
         [:a {:href "https://vetd.com/terms-of-use"
              :target "_blank"}
          " (read)"]]
        [:> ui/Button {:color "teal"
                       :fluid true
                       :on-click #(rf/dispatch [:create-acct [{:uname @uname
                                                               :email @email
                                                               :org-name @org-name
                                                               :org-url @org-url
                                                               :org-type "buyer"
                                                               :pwd @pwd}]])}
         "Sign Up"]]])))
