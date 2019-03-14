(ns vetd-app.vendors.pages.signup
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]))

;; Events
(rf/reg-event-fx
 :v/nav-signup
 (constantly {:nav {:path "/v/signup"}}))

(rf/reg-event-db
 :v/route-signup
 (fn [db]
   (assoc db :page :v/signup)))

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
         "Sign Up as a Vendor"]
        [:> ui/FormField
         [:> ui/Input {:placeholder "Full Name"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! uname (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:type "email"
                       :placeholder "Email Address"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! email (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:placeholder "Organization Name"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! org-name (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:type "url"
                       :label "http://"
                       :placeholder "Organization Website"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! org-url (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:type "password"
                       :placeholder "Password"
                       :onChange (fn [_ this] (reset! pwd (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:type "password"
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
                                                               :org-type "vendor"
                                                               :pwd @pwd}]])}
         "Sign Up"]]])))
