(ns vetd-app.common.pages.signup
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
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
 [(rf/inject-cofx :local-store [:join-group-name])]
 (fn [{:keys [db local-store]} [_ org-type]]
   {:db (assoc db
               :page :signup
               :page-params {:org-type (keyword org-type)
                             :join-group-name (:join-group-name local-store)})
    :analytics/page {:name (str (s/capitalize (name org-type)) " Signup")}}))

(rf/reg-event-fx
 :signup.submit
 (fn [{:keys [db]} [_ {:keys [uname email pwd cpwd org-name terms-agree] :as account}]]
   (let [[bad-input message]
         (cond
           (not (re-matches #".+\s.+" uname)) [:uname "Please enter your full name (first & last)."]
           (not (util/valid-email-address? email)) [:email "Please enter a valid email address."]
           (< (count pwd) 8) [:pwd "Password must be at least 8 characters."]
           (not= pwd cpwd) [:cpwd "Password and Confirm Password must match."]
           (s/blank? org-name) [:org-name "Please enter the name of your organization."]
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
              :message (str "We've sent an email to " email " with a link to activate your account.")}}
     {:db (assoc-in db [:page-params :bad-input] :email)
      :toast {:type "error" 
              :title "Error"
              :message "There is already an account with that email address."}})))

;; Subscriptions
(rf/reg-sub
 :signup-org-type
 :<- [:page-params] 
 (fn [{:keys [org-type]}] org-type))

(rf/reg-sub
 :join-group-name
 :<- [:page-params] 
 (fn [{:keys [join-group-name]}] join-group-name))

;; Components
(defn c-page []
  (let [signup-org-type& (rf/subscribe [:signup-org-type])
        join-group-name& (rf/subscribe [:join-group-name])
        uname (r/atom "")
        email (r/atom "")
        org-name (r/atom "")

        ;; for orgs
        options& (r/atom []) ; options from search results + current values
        search-query& (r/atom "")
        
        org-url (r/atom "")
        org-url-input-ref (atom nil)
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
                      :class (case @signup-org-type& :buyer "teal" :vendor "blue")}
        (if @join-group-name&
          (str "Join the " @join-group-name& " community on Vetd")
          (str "Sign Up as a " (case @signup-org-type& :buyer "Buyer" :vendor "Vendor")))]
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
                        :spellCheck false
                        :on-invalid #(.preventDefault %) ; no type=email error message (we'll show our own)
                        ;; for convenience, try to autofill Organization Website field
                        :on-blur #(let [email-domain (second (s/split @email #"@"))]
                                    (when (and (empty? @org-url)
                                               (not (#{"gmail.com" "yahoo.com" "hotmail.com" "msn.com" "outlook.com"} email-domain)))
                                      (reset! org-url email-domain)))
                        :on-change (fn [_ this]
                                     (reset! email (.-value this)))}]]]
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
        [:> ui/FormField {:error (= @bad-input& :org-name)}
         [:label "Organization Name"
          (case @signup-org-type&
            :buyer [:> ui/Input {:class "borderless"
                                 :spellCheck false
                                 :onChange (fn [_ this] (reset! org-name (.-value this)))}]
            :vendor (let [orgs->options (fn [orgs]
                                          (for [{:keys [id oname]} orgs]
                                            {:key id
                                             :text oname
                                             :value oname}))
                          orgs& (rf/subscribe
                                 [:gql/q
                                  {:queries
                                   [[:orgs {:_where
                                            {:_and ;; while this trims search-query, the Dropdown's search filter doesn't...
                                             [{:oname {:_ilike (str "%" (s/trim @search-query&) "%")}}
                                              {:deleted {:_is_null true}}]}
                                            :_limit 100
                                            :_order_by {:oname :asc}}
                                     [:id :oname]]]}])
                          _ (when-not (= :loading @orgs&)
                              (let [options (->> @orgs&
                                                 :orgs
                                                 orgs->options ; now we have options from gql sub
                                                 ;; (this dumbly actually keeps everything, but that seems fine)
                                                 (concat @options&) ; keep options for the current values
                                                 distinct)]
                                (when-not (= @options& options)
                                  (reset! options& options))))]
                      [:> ui/Dropdown {:class "borderless"
                                       :value @org-name
                                       :fluid true
                                       :loading (= :loading @orgs&)
                                       :options @options&
                                       :placeholder "Search or add organization..."
                                       :search true
                                       :selection true
                                       :multiple false
                                       :selectOnBlur false
                                       :selectOnNavigation true
                                       :closeOnChange true
                                       :allowAdditions true
                                       :additionLabel "Hit 'Enter' to Add "
                                       :onAddItem (fn [_ this]
                                                    (swap! options& conj {:key (gensym)
                                                                          :value (.-value this)
                                                                          :text (.-value this)})
                                                    (reset! org-name (.-value this)))
                                       :onSearchChange (fn [_ this]
                                                         (reset! search-query& (aget this "searchQuery")))
                                       :onChange (fn [_ this]
                                                   (reset! org-name (.-value this)))}]))]]
        [:> ui/FormField {:error (= @bad-input& :org-url)}
         [:label "Organization Website"
          [:> ui/Input {:class "borderless"
                        :label true}
           [:> ui/Label "http://"]
           [:input {:value @org-url
                    :style {:width 0} ; idk why 0 width works, but it does
                    :spellCheck false
                    :ref (fn [this] (reset! org-url-input-ref this))
                    :on-change #(reset! org-url (-> % .-target .-value))}]]]]
        [:> ui/FormField {:error (= @bad-input& :terms-agree)
                          :style {:margin "25px 0 20px 0"}}
         [:> ui/Checkbox {:label "I agree to the Terms Of Use"
                          :onChange (fn [_ this] (reset! terms-agree (.-checked this)))}]
         [:a {:href "https://vetd.com/terms-of-use"
              :target "_blank"}
          " (read)"]]
        [:> ui/Button {:color (case @signup-org-type& :buyer "teal" :vendor "blue")
                       :fluid true
                       :on-click #(rf/dispatch [:signup.submit
                                                {:uname @uname
                                                 :email @email
                                                 :org-name @org-name
                                                 :org-url @org-url
                                                 :org-type (name @signup-org-type&)
                                                 :pwd @pwd
                                                 :cpwd @cpwd
                                                 :terms-agree @terms-agree}])}
         "Sign Up"]
        [:br] [:br]
        [:a {:on-click #(rf/dispatch [:nav-login])}
         "Already have an account? Log in."]]])))
