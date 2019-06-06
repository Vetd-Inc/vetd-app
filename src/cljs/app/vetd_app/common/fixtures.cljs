(ns vetd-app.common.fixtures
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn c-top-nav-page-link [{:keys [text event active]}]
  ^{:key text}
  [:> ui/MenuItem {:class "page-link"
                   :active active
                   :on-click #(rf/dispatch event)}
   text])

(defn c-top-nav [top-nav-pages]
  (let [page& (rf/subscribe [:page])
        user-name& (rf/subscribe [:user-name])
        org-name& (rf/subscribe [:org-name])]
    (fn []
      (when (and @page& @user-name&)
        [:> ui/Menu {:class "top-nav"
                     :secondary true} ; 'secondary' is a misnomer (it's just for styling)
         [:> ui/MenuItem {:class "logo"
                          :on-click #(rf/dispatch [:nav-home])}
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
          [:> ui/MenuItem (str @user-name& " @ " @org-name&)]
          [:> ui/MenuItem {:name "logout"
                           :active false
                           :on-click #(rf/dispatch [:logout])}]]]))))

(defn container [body] body)
