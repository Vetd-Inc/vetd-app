(ns vetd-app.buyer-fixtures
  (:require [vetd-app.flexer :as flx]
            [vetd-app.ui :as ui]
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

(defn menu-item [{:keys [text event active]}]
  ^{:key text}
  [:> ui/MenuItem {:active active
                   :onClick #(rf/dispatch event)}
   text])

(defn top-nav []
  (let [page& (rf/subscribe [:page])
        user-name& (rf/subscribe [:user-name])
        org-name& (rf/subscribe [:org-name])]
    (fn []
      [:> ui/Menu {:class "top-nav"
                   :secondary true} ; todo: misnomer just to cause styling
       [:> ui/MenuItem {:onClick #(rf/dispatch [:b/nav-home])
                        :class "logo"}
        ;; todo: use a config var for base url
        [:img {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]]
       (for [item [{:text "Preposals"
                    :page :b/preposals
                    :event [:b/nav-preposals]}
                   {:text "Products & Categories"
                    :page :b/search
                    :event [:b/nav-search]}]]
         (menu-item (assoc item :active (= @page& (:page item)))))
       [:> ui/MenuMenu {:position "right"}
        #_[:> ui/MenuItem
           [:> ui/Input {:icon "search"
                         :placeholder "Search..."}]]
        [:> ui/MenuItem (str @user-name& " @ " @org-name&)]
        [:> ui/MenuItem {:name "logout"
                         :active false
                         :onClick #(rf/dispatch [:logout])}]]])))

(defn container [body]
  [:> ui/Container {:class "main-container"}
   [top-nav]
   body])
