(ns vetd-app.common.pages.signup
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

;; Events
(rf/reg-event-fx
 :nav-signup
 (fn [_ [_ type]]
   {:nav {:path (str "/signup/" type)}
    :analytics/track {:event "Signup Start"
                      :props {:category "Accounts"
                              :label (s/capitalize type)}}}))

(rf/reg-event-fx
 :route-signup
 (fn [{:keys [db]} [_ type]]
   {:db (assoc db
               :page :signup
               :page-params {:type type})
    :analytics/page {:name (str (s/capitalize type) " Signup")}}))

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
         [:> ui/Input {:class "borderless"
                       :placeholder "Full Name"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! uname (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:class "borderless"
                       :type "email"
                       :placeholder "Email Address"
                       :spellCheck false
                       :on-blur #(when (empty? @org-url)
                                   (reset! org-url (second (s/split @email #"@"))))
                       :on-change (fn [_ this]
                                    (reset! email (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:class "borderless"
                       :placeholder "Organization Name"
                       :spellCheck false
                       :onChange (fn [_ this] (reset! org-name (.-value this)))}]]
        [:> ui/FormField
         [:> ui/Input {:class "borderless"
                       :label true}
          [:> ui/Label "http://"]
          [:input {:value @org-url
                   :style {:width 0} ; idk why 0 width works, but it does
                   :placeholder "Organization Website"
                   :spellCheck false
                   :on-change #(reset! org-url (-> % .-target .-value))}]]]
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
                                                               :org-type "vendor"
                                                               :pwd @pwd}]])}
         "Sign Up"]]])))
