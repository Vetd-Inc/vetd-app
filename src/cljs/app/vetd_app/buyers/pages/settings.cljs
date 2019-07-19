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
   {:ws-send {:payload {:cmd :update-user
                        :return {:handler :update-user-return
                                 :uname uname}
                        :user-id (-> db :user :id)
                        :uname uname}}
    :analytics/track {:event "Update Name"
                      :props {:category "Account"}}}))

(rf/reg-event-fx
 :update-user-return
 (fn [{:keys [db]} [_ _ {{:keys [uname]} :return}]]
   {:db (assoc-in db [:user :uname] uname)
    :toast {:type "success"
            :title "Name Updated Successfully"
            :message (str "Hello " uname "!")}
    :dispatch [:stop-edit-field "uname"]}))

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
       [:> ui/Input
        {:default-value @uname&
         :auto-focus true
         :fluid true
         :style {:padding-top 7}
         :error (= @bad-input& :uname)
         :placeholder "Enter your full name..."
         :on-change #(reset! uname& (-> % .-target .-value))
         :action (r/as-element
                  [:> ui/Button {:on-click #(rf/dispatch [:update-user-name.submit @uname&])
                                 :color "blue"}
                   "Save"])}]])))

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

(defn c-edit-password []
  (let [pwd& (r/atom "")
        new-pwd& (r/atom "")
        confirm-new-pwd& (r/atom "")
        bad-input& (rf/subscribe [:bad-input])]
    (fn []
      [:> ui/Form
       [:> ui/Input
        {:type "password"
         :auto-focus true
         :placeholder "Current Password"
         :fluid true
         :style {:padding-top 7}
         :error (= @bad-input& :pwd)
         :on-change #(reset! pwd& (-> % .-target .-value))}]
       [:> ui/Input
        {:type "password"
         :placeholder "New Password"
         :fluid true
         :style {:padding-top 7}
         :error (= @bad-input& :new-pwd)
         :on-change #(reset! new-pwd& (-> % .-target .-value))}]
       [:> ui/Input
        {:type "password"
         :placeholder "Confirm New Password"
         :fluid true
         :style {:padding-top 7}
         :error (= @bad-input& :confirm-new-pwd)
         :on-change #(reset! confirm-new-pwd& (-> % .-target .-value))
         :action (r/as-element
                  [:> ui/Button {:on-click #(rf/dispatch [:update-user-password.submit @new-pwd&])
                                 :color "blue"}
                   "Save"])}]])))

(defn c-password-field
  []
  [c-editable-field
   "Password"
   "pwd"
   "**********"
   "Change Password"
   [c-edit-password]])

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
          [c-password-field]]]
        [:> ui/GridColumn {:computer 5 :mobile 0}]]])))
