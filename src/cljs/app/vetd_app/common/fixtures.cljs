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
  [:div.account-actions 
   [:h5 user-name]
   [:> ui/Button {:on-click #(rf/dispatch [:nav-settings])
                  :color "lightteal"
                  :fluid true
                  :icon true
                  :labelPosition "left"}
    "Settings"
    [:> ui/Icon {:name "setting"}]]
   [:> ui/Button {:on-click #(rf/dispatch [:logout])
                  :color "white"
                  :fluid true
                  :icon true
                  :labelPosition "left"}
    "Log Out"
    [:> ui/Icon {:name "sign-out"}]]])

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
        admin-of-groups& (rf/subscribe [:admin-of-groups])]
    (fn [top-nav-pages]
      (let [;; append "Community" menu item if accessible
            pages (cond-> top-nav-pages
                    (not-empty @admin-of-groups&) (conj {:text "Community"
                                                         :pages #{:g/settings}
                                                         :event [:g/nav-settings]}))]
        (when (and @page& @user-name&)
          [:<>
           [:span.scroll-anchor {:ref (fn [this] (rf/dispatch [:reg-scroll-to-ref :top this]))}]
           [:> ui/Menu {:class "top-nav"
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
