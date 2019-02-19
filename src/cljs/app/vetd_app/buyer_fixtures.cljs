(ns vetd-app.buyer-fixtures
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :logout
 (fn [{:keys [db]} _]
   {:local-store {:session-token nil}
    :cookies {:admin-token [nil {:max-age 60
                                 :path "/"}] }
    :dispatch [:pub/nav-login]}))

(defn header []
  (let [user-name& (rf/subscribe [:user-name])
        org-name& (rf/subscribe [:org-name])]
    (fn []
      [flx/row :header
       [:div#header-left [:div.logo
                          {:on-click #(rf/dispatch [:nav-home])}]]
       [:div#header-middle [:div.org-name @org-name&]]
       [:div#header-right
        [:div.user-name @user-name&]
        [rc/button
         :label "Logout"
         :on-click #(rf/dispatch [:logout])]]])))

(defn tab [current& label target disp]
  [:div.tab
   [:a {:class (into [:tab]
                     (when (= @current& target)
                       [:selected]))
        :on-click #(rf/dispatch disp)}
    label]])

(defn sidebar []
  (let [page& (rf/subscribe [:page])]
    (fn []
      [flx/col :sidebar
       (tab page& "Home" :b/home [:b/nav-home])
       (tab page& "Preposals" :b/preposals [:b/nav-preposals])
       (tab page& "Products & Categories" :b/search [:b/nav-search])])))

(defn container [body]
  [flx/col :container #{:buyer}
   [{:width "100%"} [header]]
   [flx/row {:height "100%"
             :width "100%"}
    [sidebar]
    [{:flex-grow 1 :margin "10px"}
     body]
    ]])
