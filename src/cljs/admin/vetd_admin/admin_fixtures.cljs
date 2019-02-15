(ns vetd-admin.admin-fixtures
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))



(defn header []
  (let [user-name& (rf/subscribe [:user-name])
        org-name& (rf/subscribe [:org-name])]
    (fn []
      [flx/row :header
       [:div#header-left
        [:div.logo {:on-click #(rf/dispatch [:nav-home])}]]
       [:div#header-middle [:div.admin-notice "ADMIN PANEL"]]
       [:div#header-right
        [:div.user-name @user-name&]
        [rc/button
         :label "Logout"
         :on-click #(rf/dispatch [:logout])]]])))

(defn tab [current& label target disp]
  [:div {:class (into [:tab]
                     (when (= @current& target)
                       [:selected]))}
   [:a {:on-click #(rf/dispatch disp)}
    label]])

(defn sidebar []
  (let [page& (rf/subscribe [:page])]
    (fn []
      [flx/col :sidebar
       (tab page& "Admin Home" :a/home [:a/nav-home])
       (tab page& "Admin Search" :a/search [:a/nav-search])])))

(defn container [body]
  [flx/col :container #{:buyer}
   [{:width "100%"} [header]]
   [flx/row {:height "100%"
             :width "100%"}
    [sidebar]
    [{:flex-grow 1 :margin "10px"}
     body]]])
