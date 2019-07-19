(ns vetd-app.common.fixtures
  (:require [vetd-app.ui :as ui]
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
   [:> ui/Button {:on-click #(rf/dispatch [:b/nav-settings])
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

(defn c-avatar-initials
  [user-name]
  (let [parts (s/split user-name " ")]
    [:div.avatar-initials (->> (select-keys parts [0 (dec (count parts))])
                               vals
                               (map first)
                               (apply str))]))

(defn c-avatar
  [user-name org-name]
  [:> ui/Popup
   {:position "bottom right"
    :on "click"
    :content (r/as-element [c-account-actions user-name])
    :trigger (r/as-element [:div.avatar-container org-name [c-avatar-initials user-name]])}])

(defn c-top-nav [top-nav-pages]
  (let [page& (rf/subscribe [:page])
        user-name& (rf/subscribe [:user-name])
        org-name& (rf/subscribe [:org-name])]
    (fn []
      (when (and @page& @user-name&)
        [:> ui/Menu {:class "top-nav"
                     :secondary true} ; 'secondary' is a misnomer (it's just for styling)
         [:> ui/MenuItem {:class "logo"
                          :on-click #(do (rf/dispatch [:b/update-search-term ""])
                                         (rf/dispatch [:nav-home]))}
          ;; todo: use a config var for base url
          [:img {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]]
         (doall
          (for [item top-nav-pages]
            (c-top-nav-page-link (assoc item :active (boolean ((:pages item) @page&))))))
         [:> ui/MenuMenu {:position "right"}
          ;; Consider having search bar in top nav?
          ;; [:> ui/MenuItem
          ;;    [:> ui/Input {:icon "search"
          ;;                  :placeholder "Search for products & categories..."}]]
          [:> ui/MenuItem {:style {:padding-right 0}}
           [c-avatar @user-name& @org-name&]]]]))))

(defn container [body]
  [:<>
   body
   [:div {:style {:height 100}}]])
