(ns vetd-app.common.pages.signup
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

;; Events
(rf/reg-event-fx
 :nav-signup
 (fn [_ [_ org-type]]
   {:nav {:path (str "/signup/" (name org-type))}
    :analytics/track {:event "Signup Start"
                      :props {:category "Accounts"
                              :label (s/capitalize (name org-type))}}}))

(rf/reg-event-fx
 :route-signup
 (fn [{:keys [db]} [_ org-type]]
   {:db (assoc db
               :page :signup
               :page-params {:org-type (keyword org-type)})
    :analytics/page {:name (str (s/capitalize (name org-type)) " Signup")}}))

(rf/reg-event-fx
 :create-acct
 (fn [{:keys [db]} [_ [account]]]
   {:ws-send {:ws (:ws db)
              :payload (merge {:cmd :create-acct
                               :return {:handler :ws/create-acct
                                        :org-type (:org-type account)
                                        :email (:email account)}}
                              (select-keys account [:uname :org-name :org-url
                                                    :org-type :email :pwd]))}}))
(rf/reg-event-fx
 :ws/create-acct
 (fn [{:keys [db]} [_ results {{:keys [org-type email]} :return}]]
   (if-not (:email-used? results)
     {:dispatch [:nav-login]
      :toast {:type "success"
              :title "Thanks for Signing Up!"
              :message "You can now login."}
      :analytics/track {:event "Signup Complete"
                        :props {:category "Accounts"
                                :label org-type}}}
     (js/alert "Email already in use by another account."))))

;; Subscriptions
(rf/reg-sub
 :signup-org-type
 :<- [:page-params] 
 (fn [{:keys [org-type]}] org-type))

;; Components
(defn c-page []
  (let [signup-org-type& (rf/subscribe [:signup-org-type])
        uname (r/atom "")
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
         (case @signup-org-type&
           :buyer "Sign Up as a Buyer"
           :vendor "Sign Up as a Vendor")]
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
                                                               :org-type (name @signup-org-type&)
                                                               :pwd @pwd}]])}
         "Sign Up"]]])))
