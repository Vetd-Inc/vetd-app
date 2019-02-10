(ns vetd-app.pages.signup
  (:require [vetd-app.util :as ut]
            [vetd-app.blocker :as bl]
            [vetd-app.websockets :as ws]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [goog.functions]))


(rf/reg-event-db
 :route-signup
 (fn [db [_ query-params]]
   (assoc db
          :page :signup)))

(rf/reg-event-fx
 :nav-signup
 (fn [_ _]
   {:nav {:path "/signup"}}))

(rf/reg-event-fx
 :create-acct
 (fn [{:keys [db]} [_ [uname email org-name org-url pwd b-or-v]]]
   {:ws-send {:ws (:ws db)
              :payload {:cmd :create-acct
                        :return :ws/create-acct
                        :uname uname
                        :org-name org-name
                        :org-url org-url
                        :email email
                        :pwd pwd
                        :b-or-v? (= b-or-v :buyer)}}}))

(rf/reg-event-fx
 :ws/create-acct
 (fn [{:keys [db]} [_ results]]
   (def res1 results)
   #_ (println res1)
   (if-not (:email-used? results)
     {:dispatch [:pub/nav-login]}
     (js/alert "Email already in use by another account."))))


(defn signup-page []
  (let [uname (r/atom "")
        email (r/atom "")
        org-name (r/atom "")
        org-url (r/atom "")        
        pwd (r/atom "")
        cpwd (r/atom "")
        b-or-v (r/atom :buyer)]
    (fn []
      [:div {:id :signup-form}
       [rc/input-text
        :model uname
        :on-change #(reset! uname %)
        :placeholder "Your Name"]
       [rc/input-text
        :model email
        :on-change #(reset! email %)
        :placeholder "Your Email"]   
       [rc/input-text
        :model org-name
        :on-change #(reset! org-name %)
        :placeholder "Organization Name"]
       [rc/input-text
        :model org-url
        :on-change #(reset! org-url %)
        :placeholder "Organization Website URL"]
       [rc/input-password
        :model pwd
        :on-change #(reset! pwd %)
        :placeholder "Password"]
       [rc/input-password
        :model cpwd
        :on-change #(reset! cpwd %)
        :placeholder "Confirm Password"]
       [:div {:style {:padding "10px"
                      :font-size "20px"}}
        "I am a "
        [rc/horizontal-bar-tabs
         :model b-or-v
         :tabs [{:id :buyer
                 :label "Buyer"}
                {:id :vendor
                 :label "Vendor"}]
         :on-change #(reset! b-or-v %)]]
       [rc/button
        :on-click #(rf/dispatch [:create-acct [@uname @email @org-name @org-url @pwd @b-or-v]])
        :label "Create Account"]])))

























































