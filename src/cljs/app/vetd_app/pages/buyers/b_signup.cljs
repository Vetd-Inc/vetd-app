(ns vetd-app.pages.buyers.b-signup
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]))

;; Events
(rf/reg-event-fx
 :b/nav-signup
 (constantly {:nav {:path "/b/signup"}}))

(rf/reg-event-db
 :b/route-signup
 (fn [db]
   (assoc db :page :b/signup)))

;; Components
(defn c-page []
  (let [uname (r/atom "")
        email (r/atom "")
        org-name (r/atom "")
        org-url (r/atom "")        
        pwd (r/atom "")
        cpwd (r/atom "")]
    (fn []
      [:div.centerpiece
       [:img {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"
              :style {:width 210
                      :marginBottom 30}}]
       [:> ui/Form
        [:> ui/Header {:as "h2"}
         "Sign Up as a Buyer"]
        [:> ui/FormField
         [:> ui/Input {:placeholder "Full Name"
                       :onChange (fn [_ this] (reset! uname (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:placeholder "Email Address"
                       :onChange (fn [_ this] (reset! email (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:placeholder "Organization Name"
                       :onChange (fn [_ this] (reset! org-name (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:placeholder "Organization Website"
                       :onChange (fn [_ this] (reset! org-url (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:type "password"
                       :placeholder "Password"
                       :onChange (fn [_ this] (reset! pwd (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:type "password"
                       :placeholder "Confirm Password"
                       :onChange (fn [_ this] (reset! cpwd (.-value this)))}]]
        [:> ui/Button {:color "teal"
                       :fluid true
                       :on-click #(rf/dispatch [:create-acct [{:uname @uname
                                                               :email @email
                                                               :org-name @org-name
                                                               :org-url @org-url
                                                               :org-type "buyer"
                                                               :pwd @pwd}]])}
         "Sign Up"]]])))
