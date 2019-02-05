(ns vetd-app.buyer-fixtures
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :logout
 (fn [{:keys [db]} _]
   {:local-store {:session-token nil}
    :dispatch [:nav-login]}))



(defn header []
  (let [user-name& (rf/subscribe [:user-name])
        org-name& (rf/subscribe [:org-name])]
    (fn []
      [ut/flx {:p {:id :header}}
       [{:id :header-left} [:div.logo]]
       [{:id :header-middle} [:div.org-name @org-name&]]
       [{:id :header-right}
        [:div.user-name @user-name&]
        [rc/button
         :label "Logout"
         :on-click #(rf/dispatch [:logout])]]])))

(defn tab [current& label target disp]
  [{}
   [:a {:class (into [:tab]
                     (when (= @current& target)
                       [:selected]))
        :on-click #(rf/dispatch disp)}
    label]])

(defn sidebar []
  (let [page& (rf/subscribe [:page])]
    (fn []
      [ut/flx {:p {:id :sidebar}}
       (tab page& "Home" :b-home [:nav-home])
       [{:class [:tab]} [:a "Preposals"]]
       (tab page& "Products & Categories" :b-search [:nav-b-search])])))

(defn container [body]
  [ut/flx {:p {:id :container
               :class [:buyer]}}
   [{:style {:width "100%"}}
    [header]]
   [{:style {:height "100%"
             :width "100%"             
             :display :flex
             :f/dir :row}}
    [sidebar]
    [:div {:style {:flex-grow 1
                   :margin "10px"}}
     body]]])
