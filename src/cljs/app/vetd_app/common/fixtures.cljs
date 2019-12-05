(ns vetd-app.common.fixtures
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [clojure.string :as s]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn c-top-nav-page-link [{:keys [text event active]}]
  ^{:key text}
  [:> ui/MenuItem {:class "page-link"
                   :active active
                   :on-click #(rf/dispatch event)}
   text])

(defn c-account-actions
  [user-name]
  (let [admin-of-groups& (rf/subscribe [:admin-of-groups])
        vendor?& (rf/subscribe [:vendor?])
        dark-mode?& (rf/subscribe [:dark-mode?])]
    (fn [user-name]
      (let [is-group-admin? (not-empty @admin-of-groups&)]
        [:div.account-actions (when is-group-admin?
                                {:style {:width 195}})
         [:h5 user-name]
         [:> ui/Checkbox
          {:toggle true
           :checked @dark-mode?&
           :class "dark-mode-toggle"
           :label "Dark Mode?"
           :style {:margin-bottom 14}
           :on-click (fn [_ this]
                       (if (.-checked this)
                         (rf/dispatch [:dark-mode.on])
                         (rf/dispatch [:dark-mode.off])))}]
         [:> ui/Button {:on-click #(rf/dispatch [:nav-settings @vendor?&])
                        :color "lightteal"
                        :fluid true
                        :icon true
                        :labelPosition "left"}
          "Settings"
          [:> ui/Icon {:name "setting"}]]
         (when is-group-admin?
           [:> ui/Button {:on-click #(rf/dispatch [:g/nav-settings])
                          :color "lightblue"
                          :fluid true
                          :icon true
                          :labelPosition "left"}
            "Community Admin"
            [:> ui/Icon {:name "settings"}]])
         [:> ui/Button {:on-click #(rf/dispatch [:logout])
                        :color "white"
                        :fluid true
                        :icon true
                        :labelPosition "left"}
          "Log Out"
          [:> ui/Icon {:name "sign-out"}]]]))))

(defn c-avatar
  [user-name org-name]
  [:> ui/Popup
   {:position "bottom right"
    :on "click"
    :content (r/as-element [c-account-actions user-name])
    :trigger (r/as-element
              [:div.avatar-container
               [:span {:style {:padding-right 12}}
                org-name]
               [cc/c-avatar-initials user-name]])}])

(defn c-top-nav [top-nav-pages]
  (let [page& (rf/subscribe [:page])
        user-name& (rf/subscribe [:user-name])
        org-name& (rf/subscribe [:org-name])
        group-ids& (rf/subscribe [:group-ids])]
    (fn [top-nav-pages]
      (let [;; prepend "Community" menu item if accessible
            pages (cond->> top-nav-pages
                    (seq @group-ids&) (into [{:text "Your Communities"
                                              :pages #{:g/home}
                                              :event [:g/nav-home]}]))]
        (when (and @page& @user-name&)
          [:<>
           [:span.scroll-anchor {:ref (fn [this] (rf/dispatch [:reg-scroll-to-ref :top this]))}]
           [:> ui/Menu {:class "top-nav"
                        :stackable true
                        :secondary true} ; 'secondary' is a misnomer (it's just for styling)
            [:> ui/MenuItem {:class "logo"
                             :on-click #(do (rf/dispatch [:b/search.reset])
                                            (rf/dispatch [:nav-home]))}
             ;; todo: use a config var for base url
             [:img {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]]
            (doall
             (for [item pages]
               (c-top-nav-page-link (assoc item :active (boolean ((:pages item) @page&))))))
            [:> ui/MenuMenu {:position "right"}
             ;; Consider having search bar in top nav?
             ;; [:> ui/MenuItem
             ;;    [:> ui/Input {:icon "search"
             ;;                  :placeholder "Search for products & categories..."}]]
             [:> ui/MenuItem {:style {:padding-right 0}}
              [c-avatar @user-name& @org-name&]]]]])))))

(defn container [body]
  [:<>
   body
   [:div {:style {:height 100}}]])
