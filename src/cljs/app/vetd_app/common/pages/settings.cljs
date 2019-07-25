(ns vetd-app.common.pages.settings
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.common.fx :as cfx]
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
   {:db (-> db
            (update-in [:page-params :fields-editing] disj field)
            (assoc-in [:page-params :bad-input] nil))}))

(rf/reg-event-fx
 :update-user-name.submit
 (fn [{:keys [db]} [_ uname]]
   (cfx/validated-dispatch-fx db
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
   (cfx/validated-dispatch-fx db
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

(rf/reg-event-fx
 :invite-user-to-org.submit
 (fn [{:keys [db]} [_ email org-id user-id]]
   (cfx/validated-dispatch-fx db
                              [:o/invite-user-to-org email org-id user-id]
                              #(cond
                                 (not (re-matches #"^\S+@\S+\.\S+$" email))
                                 [:invite-email-address "Please enter a valid email address."]
                                 
                                 :else nil))))

;; use this to remove a user from an org
(rf/reg-event-fx
 :delete-membership
 (fn [{:keys [db]} [_ memb-id user-name org-id org-name]]
   {:ws-send {:payload {:cmd :delete-membership
                        :return {:handler :delete-membership-return
                                 :user-name user-name
                                 :org-name org-name}
                        :id memb-id}}
    :analytics/track {:event "Remove User From Org"
                      :props {:category "Account"
                              :label org-id}}}))

(rf/reg-event-fx
 :delete-membership-return
 (fn [{:keys [db]} [_ _ {{:keys [user-name org-name]} :return}]]
   {:toast {:type "success"
            :title "Member Removed from Organization"
            :message (str user-name " has been removed from " org-name)}}))

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
                     :value [:em "hidden"]
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
  [member org-id org-name]
  (let [curr-user-id& (rf/subscribe [:user-id])
        popup-open? (r/atom false)]
    (fn [{:keys [id status user]} org-id org-name]
      (let [{user-id :id
             user-name :uname
             email :email} user
            self? (= user-id @curr-user-id&)]
        [:> ui/ListItem
         [:> ui/ListContent
          (when-not self?
            [:> ui/Popup
             {:position "bottom right"
              :on "click"
              :open @popup-open?
              :on-close #(reset! popup-open? false)
              :content (r/as-element
                        [:div
                         [:h5 "Are you sure you want to remove " user-name " from " org-name "?"]
                         [:> ui/ButtonGroup {:fluid true}
                          [:> ui/Button {:on-click #(reset! popup-open? false)}
                           "Cancel"]
                          [:> ui/Button {:on-click (fn []
                                                     (reset! popup-open? false)
                                                     (rf/dispatch [:delete-membership id user-name org-id org-name]))
                                         :color "red"}
                           "Remove"]]])
              :trigger (r/as-element
                        [:> ui/Label {:on-click #(swap! popup-open? not)
                                      :as "a"
                                      :style {:float "right"
                                              :margin-top 5}}
                         [:> ui/Icon {:name "remove"}]
                         (if self? "Leave Organization" "Remove")])}])
          [:div {:style {:display "inline-block"
                         :float "left"
                         :margin-right 7}}
           [cc/c-avatar-initials user-name]]
          [:> ui/ListHeader user-name (when self? " (you)")]
          [:> ui/ListDescription email]]]))))

(defn c-invite-member-form [org-id]
  (let [fields-editing& (rf/subscribe [:fields-editing])
        user-id& (rf/subscribe [:user-id])
        email& (r/atom "")
        bad-input& (rf/subscribe [:bad-input])]
    (fn [org-id]
      (when (@fields-editing& "invite-email-address")
        [:> ui/Form
         [:> ui/FormField {:error (= @bad-input& :invite-email-address)
                           :style {:padding-top 7}}
          [:> ui/Input
           {:placeholder "Enter email address..."
            :fluid true
            :auto-focus true
            :on-change #(reset! email& (-> % .-target .-value))
            :action (r/as-element
                     [:> ui/Button
                      {:on-click (fn []
                                   (rf/dispatch [:invite-user-to-org.submit
                                                 @email&
                                                 org-id
                                                 @user-id&]))
                       :color "blue"}
                      "Invite"])}]]]))))

(defn c-org-members
  [memberships org-id org-name]
  (let [fields-editing& (rf/subscribe [:fields-editing])]
    (fn [memberships org-id org-name]
      [c-field-container
       (if (@fields-editing& "invite-email-address")
         [:> ui/Label {:on-click #(rf/dispatch [:stop-edit-field "invite-email-address"])
                       :as "a"
                       :style {:float "right"}}
          "Cancel"]
         [:> ui/Label {:on-click #(rf/dispatch [:edit-field "invite-email-address"])
                       :as "a"
                       :color "teal"
                       :style {:float "right"}}
          [:> ui/Icon {:name "add user"}]
          "Invite New Member"])
       [:h3.display-field-key "Members"]
       [c-invite-member-form org-id]
       ;; this TransitionGroup doesn't seem to be transitioning...
       [:> ui/TransitionGroup {:as ui/List
                               :class "members"
                               :relaxed true
                               :duration 500
                               :animation "scale"}
        (for [member memberships]
          ^{:key (-> member :user :id)}
          [c-org-member member org-id org-name])]])))

(defn c-orgs-settings []
  (let [org-id& (rf/subscribe [:org-id])
        orgs& (rf/subscribe [:gql/sub
                             {:queries
                              [[:orgs {:id @org-id&
                                       :_limit 1
                                       :deleted nil}
                                [:id :oname :url
                                 [:memberships
                                  [:id :status
                                   [:user
                                    [:id :idstr :uname :email]]]]]]]}])]
    (fn [] ; TODO handle multiple orgs
      (if (= :loading @orgs&)
        [cc/c-loader]
        (let [{:keys [id oname url memberships] :as org} (-> @orgs& :orgs first)]
          [bc/c-profile-segment {:title "Organization Settings"}
           [c-field {:label "Name"
                     :value oname}]
           [c-field {:label "Website"
                     :value url}]
           [c-org-members memberships id oname]])))))

(defn c-page []
  [:> ui/Grid {:stackable true}
   [:> ui/GridRow
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [c-account-settings]]
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [c-orgs-settings]]]])
