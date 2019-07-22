(ns vetd-app.common.pages.settings
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]))

;;;; Events
(rf/reg-event-fx
 :nav-settings
 (constantly
  {:nav {:path "/settings"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Settings"}}}))

(rf/reg-event-fx
 :route-settings
 (fn [{:keys [db]}]
   {:db (assoc db
               :page :settings
               :page-params {:fields-editing #{}})
    :analytics/page {:name "Settings"}}))

(rf/reg-event-fx
 :edit-field
 (fn [{:keys [db]} [_ field]]
   {:db (update-in db [:page-params :fields-editing] conj field)}))

(rf/reg-event-fx
 :stop-edit-field
 (fn [{:keys [db]} [_ field]]
   {:db (update-in db [:page-params :fields-editing] disj field)}))

(defn validated-dispatch-fx
  [db event validator-fn]
  (let [[bad-input message] (validator-fn)]
    (if bad-input
      {:db (assoc-in db [:page-params :bad-input] bad-input)
       :toast {:type "error" 
               :title "Error"
               :message message}}
      {:db (assoc-in db [:page-params :bad-input] nil)
       :dispatch event})))

(rf/reg-event-fx
 :update-user-name.submit
 (fn [{:keys [db]} [_ uname]]
   (validated-dispatch-fx db
                          [:update-user uname]
                          #(cond
                             (not (re-matches #".+\s.+" uname)) [:uname "Please enter your full name (first & last)."]
                             :else nil))))

(rf/reg-event-fx
 :update-user
 (fn [{:keys [db]} [_ uname]]
   (let [user-id (-> db :user :id)]
     {:ws-send {:payload {:cmd :update-user
                          :return {:handler :update-user-return
                                   :uname uname}
                          :user-id user-id
                          :uname uname}}
      :analytics/track {:event "Update Name"
                        :props {:category "Account"
                                :label user-id}}})))

(rf/reg-event-fx
 :update-user-return
 (fn [{:keys [db]} [_ _ {{:keys [uname]} :return}]]
   {:db (assoc-in db [:user :uname] uname)
    :toast {:type "success"
            :title "Name Updated Successfully"
            :message (str "Hello " uname "!")}
    :dispatch [:stop-edit-field "uname"]}))

(rf/reg-event-fx
 :update-user-password.submit
 (fn [{:keys [db]} [_ pwd new-pwd confirm-new-pwd]]
   (validated-dispatch-fx db
                          [:update-user-password pwd new-pwd]
                          #(cond
                             (< (count new-pwd) 8) [:new-pwd "Password must be at least 8 characters."]
                             (not= new-pwd confirm-new-pwd) [:confirm-new-pwd "Password and Confirm Password must match."]
                             :else nil))))

(rf/reg-event-fx
 :update-user-password
 (fn [{:keys [db]} [_ pwd new-pwd]]
   (let [user-id (-> db :user :id)]
     {:ws-send {:payload {:cmd :update-user-password
                          :return {:handler :update-user-password-return}
                          :user-id user-id
                          :pwd pwd
                          :new-pwd new-pwd}}
      :analytics/track {:event "Update Password"
                        :props {:category "Account"
                                :label user-id}}})))

(rf/reg-event-fx
 :update-user-password-return
 (fn [{:keys [db]} [_ results]]
   (if (:success? results)
     {:toast {:type "success"
              :title "Password Updated Successfully"}
      :dispatch [:stop-edit-field "pwd"]}
     {:db (assoc-in db [:page-params :bad-input] :pwd)
      :toast {:type "error" 
              :title "Error"
              :message "Current Password is incorrect."}})))

;;;; Subscriptions
(rf/reg-sub
 :fields-editing
 :<- [:page-params]
 (fn [{:keys [fields-editing]}] fields-editing))

;;;; Components
(defn c-edit-user-name
  [user-name]
  (let [uname& (r/atom user-name)
        bad-input& (rf/subscribe [:bad-input])]
    (fn []
      [:> ui/Form
       [:> ui/FormField {:error (= @bad-input& :uname)
                         :style {:padding-top 7}}
        [:> ui/Input
         {:default-value @uname&
          :auto-focus true
          :fluid true
          :placeholder "Enter your full name..."
          :on-change #(reset! uname& (-> % .-target .-value))
          :action (r/as-element
                   [:> ui/Button {:on-click #(rf/dispatch [:update-user-name.submit @uname&])
                                  :color "blue"}
                    "Save"])}]]])))

(defn c-field-container
  [& children]
  [:> ui/GridRow
   [:> ui/GridColumn {:width 16}
    [:> ui/Segment {:class "display-field"
                    :vertical true}
     (util/augment-with-keys children)]]])

(defn c-field
  [{:keys [label value]}]
  [c-field-container
   [:h3.display-field-key label]
   [:div.display-field-value value]])

(defn c-editable-field
  [props edit-cmp]
  (let [fields-editing& (rf/subscribe [:fields-editing])]
    (fn [{:keys [label value sym edit-label]} edit-cmp]
      [c-field-container
       (if (@fields-editing& sym)
         [:> ui/Label {:on-click #(rf/dispatch [:stop-edit-field sym])
                       :as "a"
                       :style {:float "right"}}
          "Cancel"]
         [:> ui/Label {:on-click #(rf/dispatch [:edit-field sym])
                       :as "a"
                       :style {:float "right"}}
          [:> ui/Icon {:name "edit outline"}]
          edit-label])
       [:h3.display-field-key label]
       [:div.display-field-value (if (@fields-editing& sym) edit-cmp value)]])))

(defn c-user-name-field
  [user-name]
  [c-editable-field {:label "Name"
                     :value user-name
                     :sym "uname"
                     :edit-label "Edit Name"}
   [c-edit-user-name user-name]])

(defn c-edit-password [email]
  (let [pwd& (r/atom "")
        new-pwd& (r/atom "")
        confirm-new-pwd& (r/atom "")
        bad-input& (rf/subscribe [:bad-input])]
    (fn [email]
      [:> ui/Form
       [:> ui/FormField {:error (= @bad-input& :pwd)
                         :style {:padding-top 7}}
        [:> ui/Input
         {:type "password"
          :auto-focus true
          :placeholder "Current Password"
          :fluid true
          :on-change #(reset! pwd& (-> % .-target .-value))}]
        [:div {:style {:float "right"
                       :margin-top 5
                       :margin-bottom 18}}
         [:a {:on-click #(rf/dispatch [:nav-forgot-password email])
              :style {:font-size 13
                      :opacity 0.75}}
          "Forgot Password?"]]]
       [:> ui/FormField {:error (= @bad-input& :new-pwd)}
        [:> ui/Input
         {:type "password"
          :placeholder "New Password"
          :fluid true
          :on-change #(reset! new-pwd& (-> % .-target .-value))}]]
       [:> ui/FormField {:error (= @bad-input& :confirm-new-pwd)}
        [:> ui/Input
         {:type "password"
          :placeholder "Confirm New Password"
          :fluid true
          :on-change #(reset! confirm-new-pwd& (-> % .-target .-value))
          :action (r/as-element
                   [:> ui/Button {:on-click #(rf/dispatch [:update-user-password.submit @pwd& @new-pwd& @confirm-new-pwd&])
                                  :color "blue"}
                    "Save"])}]]])))

(defn c-password-field
  [email]
  [c-editable-field {:label "Password"
                     :value "**********"
                     :sym "pwd"
                     :edit-label "Change Password"}
   [c-edit-password email]])

(defn c-account-settings []
  (let [user-name& (rf/subscribe [:user-name])
        user-email& (rf/subscribe [:user-email])]
    (fn []
      [bc/c-profile-segment {:title "Account Settings"}
       [c-user-name-field @user-name&]
       [c-field {:label "Email"
                 :value @user-email&}]
       [c-password-field @user-email&]])))

(defn c-org-member
  [member]
  (let [curr-user-id& (rf/subscribe [:user-id])]
    (fn [{:keys [user status]}]
      (let [{:keys [id uname email]} user]
        [:> ui/ListItem
         [:> ui/ListContent {:floated "right"}
          [:> ui/Label { ;; :on-click #(rf/dispatch [:stop-edit-field sym])
                        :as "a"}
           "Remove"]]
         [:div {:style {:display "inline-block"
                        :float "left"
                        :margin-right 7}}
          [cc/c-avatar-initials uname]]
         [:> ui/ListContent uname (when (= id @curr-user-id&) " (you)")]
         [:> ui/ListContent email]]))))

(defn c-org-members
  [memberships]
  (let [inviting?& (r/atom false)
        email& (r/atom "")
        bad-input& (rf/subscribe [:bad-input])]
    (fn [memberships]
      [c-field-container
       (if @inviting?&
         [:> ui/Label {:on-click #(reset! inviting?& false)
                       :as "a"
                       :style {:float "right"}}
          "Cancel"]
         [:> ui/Label {:on-click #(reset! inviting?& true)
                       :as "a"
                       :style {:float "right"}}
          [:> ui/Icon {:name "add user"}]
          "Invite New Member"])
       [:h3.display-field-key "Members"]
       (when @inviting?&
         [:> ui/Form
          [:> ui/FormField {:error (= @bad-input& :email)}
           [:> ui/Input
            {:placeholder "Enter email address..."
             :fluid true
             :auto-focus true
             :on-change #(reset! email& (-> % .-target .-value))
             :action (r/as-element
                      [:> ui/Button {;; :on-click #(rf/dispatch [:update-user-password.submit @email&])
                                     :color "blue"}
                       "Invite"])}]]])
       [:> ui/List {:verticalAlign "middle"} ; the align doesn't seem to work
        (for [member memberships]
          ^{:key (-> member :user :id)}
          [c-org-member member])]])))

(defn c-orgs-settings []
  (let [org-id& (rf/subscribe [:org-id])
        org& (rf/subscribe [:gql/q
                            {:queries
                             [[:orgs {:id @org-id&
                                      :_limit 1
                                      :deleted nil}
                               [:oname :url
                                [:memberships
                                 [:status
                                  [:user
                                   [:id :idstr :uname :email]]]]]]]}])]
    (fn []                              ; TODO handle multiple orgs
      (let [{:keys [oname url memberships] :as org} (-> @org& :orgs first)]
        [bc/c-profile-segment {:title "Organization Settings"}
         [c-field {:label "Name"
                   :value oname}]
         [c-field {:label "Website"
                   :value url}]
         [c-org-members memberships]]))))

(defn c-page []
  [:> ui/Grid {:stackable true}
   [:> ui/GridRow
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [c-account-settings]]
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [c-orgs-settings]]]])
