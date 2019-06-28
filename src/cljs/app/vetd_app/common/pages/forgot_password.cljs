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

;; (rf/reg-event-fx
;;  :signup.submit
;;  (fn [{:keys [db]} [_ {:keys [uname email pwd cpwd org-name terms-agree] :as account}]]
;;    (let [[bad-input message]
;;          (cond
;;            (not (re-matches #".*\s.*" uname)) [:uname "Please enter your full name (first & last)."]
;;            (not (re-matches #"^\S+@\S+\.\S+$" email)) [:email "Please enter a valid email address."]
;;            (< (count pwd) 8) [:pwd "Password must be at least 8 characters."]
;;            (not= pwd cpwd) [:cpwd "Password and Confirm Password must match."]
;;            (not terms-agree) [:terms-agree "You must agree to the Terms of Use in order to sign up."]
;;            :else nil)]
;;      (if bad-input
;;        {:db (assoc-in db [:page-params :bad-input] bad-input)
;;         :toast {:type "error" 
;;                 :title "Error"
;;                 :message message}}
;;        {:dispatch [:create-acct account]}))))

;; (rf/reg-event-fx
;;  :create-acct
;;  (fn [{:keys [db]} [_ account]]
;;    {:ws-send {:ws (:ws db)
;;               :payload (merge {:cmd :create-acct
;;                                :return {:handler :create-acct-return
;;                                         :org-type (:org-type account)
;;                                         :email (:email account)}}
;;                               (select-keys account [:uname :org-name :org-url
;;                                                     :org-type :email :pwd]))}}))
;; (rf/reg-event-fx
;;  :create-acct-return
;;  (fn [{:keys [db]} [_ results {{:keys [org-type email]} :return}]]
;;    (if-not (:email-used? results)
;;      {:dispatch [:nav-login]
;;       :toast {:type "success"
;;               :title "Please check your email"
;;               :message (str "We've sent an email to " email " with a link to activate your account.")}
;;       :analytics/track {:event "Signup Complete"
;;                         :props {:category "Accounts"
;;                                 :label org-type}}}
;;      {:db (assoc-in db [:page-params :bad-input] :email)
;;       :toast {:type "error" 
;;               :title "Error"
;;               :message "There is already an account with that email address."}})))

;; Subscriptions
(rf/reg-sub
 :forgot-password-email-address
 :<- [:page-params] 
 (fn [{:keys [email-address]}] email-address))

;; (rf/reg-sub
;;  :bad-input
;;  :<- [:page-params]
;;  (fn [{:keys [bad-input]}] bad-input))

;; Components
(defn c-page []
  (let [email (r/atom @(rf/subscribe [:forgot-password-email-address]))
        bad-input& (rf/subscribe [:bad-input])]
    (fn []
      [:div.centerpiece
       [:a {:on-click #(rf/dispatch [:nav-login])}
        [:img.logo {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]]
       [:> ui/Header {:as "h2"
                      ;; :class "blue"
                      }
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
        [:> ui/Button {:color "teal"
                       :fluid true
                       ;; :on-click #(rf/dispatch [:signup.submit
                       ;;                          {:uname @uname
                       ;;                           :email @email
                       ;;                           :org-name @org-name
                       ;;                           :org-url @org-url
                       ;;                           :org-type (name @signup-org-type&)
                       ;;                           :pwd @pwd
                       ;;                           :cpwd @cpwd
                       ;;                           :terms-agree @terms-agree}])
                       }
         "Request Password Reset Link"]
        [:br] [:br]
        [:a {:on-click #(rf/dispatch [:nav-login])}
         "Return to Login"]]])))
