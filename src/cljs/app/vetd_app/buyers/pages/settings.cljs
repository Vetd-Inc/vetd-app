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

(defn c-page []
  (let [user-name& (rf/subscribe [:user-name])
        user-email& (rf/subscribe [:user-email])
        org-name& (rf/subscribe [:org-name])
        fields-editing& (rf/subscribe [:fields-editing])]
    (fn []
      [:> ui/Grid
       [:> ui/GridRow
        [:> ui/GridColumn {:computer 5 :mobile 0}]
        [:> ui/GridColumn {:computer 6 :mobile 16}
         [bc/c-profile-segment {:title "Account Settings"}
          [:> ui/GridRow
           [:> ui/GridColumn {:width 16}
            [:> ui/Segment {:class "display-field"
                            :vertical true}
             (if (@fields-editing& "uname")
               [:> ui/Label {:on-click #(rf/dispatch [:stop-edit-field "uname"])
                             :as "a"
                             :style {:float "right"}}
                "Cancel"]
               [:> ui/Label {:on-click #(rf/dispatch [:edit-field "uname"])
                             :as "a"
                             :style {:float "right"}}
                [:> ui/Icon {:name "edit outline"}]
                "Edit Name"])
             [:h3.display-field-key "Name"]
             [:div.display-field-value
              (if (@fields-editing& "uname")
                [c-edit-user-name @user-name&]
                @user-name&)]]]]
          [c-field "Email" @user-email&]
          [c-field "Organization" @org-name&]
          [:> ui/GridRow
           [:> ui/GridColumn {:width 16}
            [:> ui/Segment {:class "display-field"
                            :vertical true}
             [:> ui/Label {:as "a"
                           :style {:float "right"}}
              [:> ui/Icon {:name "edit outline"}]
              "Change Password"]
             [:h3.display-field-key "Password"]
             [:div.display-field-value [:em "hidden"]]]]]
          
          ]]
        [:> ui/GridColumn {:computer 5 :mobile 0}]]])))
