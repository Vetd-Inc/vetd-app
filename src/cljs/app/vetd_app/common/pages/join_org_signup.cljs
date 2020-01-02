(ns vetd-app.common.pages.join-org-signup
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

;; Events
(rf/reg-event-fx
 :nav-join-org-signup
 (fn [_ [_ link-key]]
   {:nav {:path (str "/signup-by-invite/" link-key)}
    :analytics/track {:event "Signup Start"
                      :props {:category "Accounts"
                              :label "By Invite"}}}))

(rf/reg-event-fx
 :route-join-org-signup
 (fn [{:keys [db]} [_ link-key]]
   {:db (assoc db
               :page :join-org-signup
               :page-params {:link-key link-key})}))

(rf/reg-event-fx
 :join-org-signup.submit
 (fn [{:keys [db]} [_ {:keys [need-email? email uname pwd cpwd terms-agree] :as account} link-key]]
   (let [[bad-input message] ; TODO use validated-dispatch-fx
         (cond
           (not (re-matches #".+\s.+" uname)) [:uname "Please enter your full name (first & last)."]
           (and need-email? (not (util/valid-email-address? email))) [:email "Please enter a valid email address."]
           (< (count pwd) 8) [:pwd "Password must be at least 8 characters."]
           (not= pwd cpwd) [:cpwd "Password and Confirm Password must match."]
           (not terms-agree) [:terms-agree "You must agree to the Terms of Use in order to sign up."]
           :else nil)]
     (if bad-input
       {:db (assoc-in db [:page-params :bad-input] bad-input)
        :toast {:type "error" 
                :title "Error"
                :message message}}
       {:dispatch [:join-org-signup account link-key]}))))

(rf/reg-event-fx
 :join-org-signup
 (fn [{:keys [db]} [_ {:keys [uname pwd] :as account} link-key]]
   {:ws-send {:ws (:ws db)
              :payload {:cmd :do-link-action
                        :return {:handler :join-org-signup-return}
                        :link-key link-key
                        :uname uname
                        :pwd pwd}}}))
(rf/reg-event-fx
 :join-org-signup-return
 (fn [{:keys [db]} [_ {:keys [cmd output-data] :as results}]]
   (if (= cmd :invite-user-to-org)
     {:toast {:type "success"
              ;; this is a vague "Joined!" because it could
              ;; be an org or a community
              :title "Joined!"
              :message (str "You accepted an invitation to join " (:org-name output-data))}
      :local-store {:session-token (:session-token output-data)}
      :dispatch-later [{:ms 100 :dispatch [:ws-get-session-user]}
                       {:ms 1000 :dispatch [:nav-home true]}
                       {:ms 1500 :dispatch [:do-fx {:analytics/track
                                                    {:event "FRONTEND Signup Complete"
                                                     :props {:category "Accounts"
                                                             :label "By Explicit Invite"}}}]}]}
     {:toast {:type "error"
              :title "Sorry, that invitation is invalid or has expired."}
      :dispatch [:nav-home]})))

;; Subscriptions
(rf/reg-sub
 :link-key
 :<- [:page-params] 
 (fn [{:keys [link-key]}] link-key))

;; this is set by the initial link read for :invite-user-to-org
(rf/reg-sub
 :signup-by-link-org-name
 (fn [{:keys [signup-by-link-org-name]}] signup-by-link-org-name))

(rf/reg-sub
 :signup-by-link-need-email?
 (fn [{:keys [signup-by-link-need-email?]}] signup-by-link-need-email?))

;; Components
(defn c-page []
  (let [email (r/atom "") ;; email is only requested if it's from a org invite reusable link (as opposed to email)
        uname (r/atom "")
        pwd (r/atom "")
        cpwd (r/atom "")
        bad-cpwd (r/atom false)
        terms-agree (r/atom false)
        bad-input& (rf/subscribe [:bad-input])
        org-name& (rf/subscribe [:signup-by-link-org-name])
        need-email?& (rf/subscribe [:signup-by-link-need-email?])
        link-key& (rf/subscribe [:link-key])]
    (fn []
      [:div.centerpiece
       [:a {:on-click #(rf/dispatch [:nav-login])}
        [:img.logo {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]]
       [:> ui/Header {:as "h2"
                      :class "blue"}
        ;; BUG @org-name& will be blank if the page is refreshed, because the link read is invalid
        
        ;; TODO
        ;; show this if simply joining an org
        "Join " @org-name& " on Vetd"
        ;; show this if invite originated from inviting a non-existent org
        ;; to a community
        ;; "Join COMMUNITY on Vetd"
        ]
       [:> ui/Form {:style {:margin-top 25}}
        (when @need-email?&
          [:> ui/FormField {:error (= @bad-input& :email)}
           [:label "Work Email Address"
            [:> ui/Input {:class "borderless"
                          :type "email"
                          :spellCheck false
                          :auto-focus true
                          :on-invalid #(.preventDefault %) ; no type=email error message (we'll show our own)
                          :on-change (fn [_ this] (reset! email (.-value this)))}]]])
        [:> ui/FormField {:error (= @bad-input& :uname)}
         [:label "Full Name"
          [:> ui/Input {:class "borderless"
                        :spellCheck false
                        :auto-focus (not @need-email?&)
                        :onChange (fn [_ this] (reset! uname (.-value this)))}]]]
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
                       :on-click #(rf/dispatch [:join-org-signup.submit
                                                {:need-email? @need-email?&
                                                 :email @email
                                                 :uname @uname
                                                 :pwd @pwd
                                                 :cpwd @cpwd
                                                 :terms-agree @terms-agree}
                                                @link-key&])}
         "Sign Up"]]])))
