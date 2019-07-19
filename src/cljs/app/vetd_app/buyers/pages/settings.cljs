(ns vetd-app.buyers.pages.settings
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [clojure.string :as s]))

;;;; Events
(rf/reg-event-fx
 :b/nav-settings
 (constantly
  {:nav {:path "/b/settings"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Buyers Settings"}}}))

(rf/reg-event-fx
 :b/route-settings
 (fn [{:keys [db]}]
   {:db (assoc db
               :page :b/settings
               :page-params {:fields-editing #{}})
    :analytics/page {:name "Buyers Settings"}}))

(rf/reg-event-fx
 :edit-field
 (fn [{:keys [db]} [_ field]]
   {:db (update-in db [:page-params :fields-editing] conj field)}))

(rf/reg-event-fx
 :stop-edit-field
 (fn [{:keys [db]} [_ field]]
   {:db (update-in db [:page-params :fields-editing] disj field)}))

(rf/reg-event-fx
 :update-user-name.submit
 (fn [{:keys [db]} [_ uname]]
   (let [[bad-input message]
         (cond
           (not (re-matches #".+\s.+" uname)) [:uname "Please enter your full name (first & last)."]
           :else nil)]
     (if bad-input
       {:db (assoc-in db [:page-params :bad-input] bad-input)
        :toast {:type "error" 
                :title "Error"
                :message message}}
       {:db (assoc-in db [:page-params :bad-input] nil)
        :dispatch [:update-user uname]}))))

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
   (let [[bad-input message]
         (cond
           (< (count new-pwd) 8) [:new-pwd "Password must be at least 8 characters."]
           (not= new-pwd confirm-new-pwd) [:confirm-new-pwd "Password and Confirm Password must match."]
           :else nil)]
     (if bad-input
       {:db (assoc-in db [:page-params :bad-input] bad-input)
        :toast {:type "error" 
                :title "Error"
                :message message}}
       {:db (assoc-in db [:page-params :bad-input] nil)
        :dispatch [:update-user-password pwd new-pwd]}))))

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
(defn c-field
  [k v ]
  [:> ui/GridRow
   [:> ui/GridColumn {:width 16}
    [:> ui/Segment {:class "display-field"
                    :vertical true}
     [:h3.display-field-key k]
     [:div.display-field-value v]]]])

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

(defn c-editable-field [field-text field-name value edit-text editor]
  (let [fields-editing& (rf/subscribe [:fields-editing])]
    (fn [field-text field-name value edit-text editor]
      [:> ui/GridRow
       [:> ui/GridColumn {:width 16}
        [:> ui/Segment {:class "display-field"
                        :vertical true}
         (if (@fields-editing& field-name)
           [:> ui/Label {:on-click #(rf/dispatch [:stop-edit-field field-name])
                         :as "a"
                         :style {:float "right"}}
            "Cancel"]
           [:> ui/Label {:on-click #(rf/dispatch [:edit-field field-name])
                         :as "a"
                         :style {:float "right"}}
            [:> ui/Icon {:name "edit outline"}]
            edit-text])
         [:h3.display-field-key field-text]
         [:div.display-field-value
          (if (@fields-editing& field-name) editor value)]]]])))

(defn c-user-name-field
  [user-name]
  [c-editable-field
   "Name"
   "uname"
   user-name
   "Edit Name"
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
  [c-editable-field
   "Password"
   "pwd"
   "**********"
   "Change Password"
   [c-edit-password email]])

(defn c-page []
  (let [user-name& (rf/subscribe [:user-name])
        user-email& (rf/subscribe [:user-email])
        org-name& (rf/subscribe [:org-name])]
    (fn []
      [:> ui/Grid
       [:> ui/GridRow
        [:> ui/GridColumn {:computer 5 :mobile 0}]
        [:> ui/GridColumn {:computer 6 :mobile 16}
         [bc/c-profile-segment {:title "Account Settings"}
          [c-user-name-field @user-name&]
          [c-field "Email" @user-email&]
          [c-field "Organization" @org-name&]
          [c-password-field @user-email&]]]
        [:> ui/GridColumn {:computer 5 :mobile 0}]]])))
