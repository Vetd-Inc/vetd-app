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

(defn avatar
  [user-name]
  (let [parts (s/split user-name " ")]
    [:> ui/Popup
     {:position "bottom right"
      :on "click"
      :content (r/as-element
                [:div 
                 [:h5 {:style {:text-align "right"}}
                  user-name]
                 [:> ui/Button {:color "white"
                                :fluid true
                                :on-click #(rf/dispatch [:logout])}
                  "Log Out"]])
      :trigger (r/as-element
                [:div.avatar-initials (->> (select-keys parts [0 (dec (count parts))])
                                           vals
                                           (map first)
                                           (apply str))])}]))

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
           @org-name& (avatar @user-name&)]]]))))

(defn container [body]
  [:<>
   body
   [:div {:style {:height 100}}]])
