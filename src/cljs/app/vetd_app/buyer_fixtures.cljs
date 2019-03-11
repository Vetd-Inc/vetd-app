(ns vetd-app.buyer-fixtures
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]))

;; Components
(def top-nav-items
  [{:text "Preposals"
    :pages #{:b/preposals :b/preposal-detail}
    :event [:b/nav-preposals]}
   {:text "Products & Categories"
    :pages #{:b/search :b/product-detail}
    :event [:b/nav-search]}])

(defn c-top-nav-item [{:keys [text event active]}]
  ^{:key text}
  [:> ui/MenuItem {:active active
                   :onClick #(rf/dispatch event)}
   text])

(defn c-top-nav []
  (let [page& (rf/subscribe [:page])
        user-name& (rf/subscribe [:user-name])
        org-name& (rf/subscribe [:org-name])]
    (fn []
      [:> ui/Menu {:class "top-nav"
                   :secondary true} ; 'secondary' is a misnomer (it's just for styling)
       [:> ui/MenuItem {:class "logo"
                        :onClick #(rf/dispatch [:nav-home])}
        ;; todo: use a config var for base url
        [:img {:src "https://s3.amazonaws.com/vetd-logos/vetd.svg"}]]
       (doall
        (for [item top-nav-items]
          (c-top-nav-item (assoc item :active (boolean ((:pages item) @page&))))))
       [:> ui/MenuMenu {:position "right"}
        ;; Consider having search bar in top nav?
        ;; [:> ui/MenuItem
        ;;    [:> ui/Input {:icon "search"
        ;;                  :placeholder "Search for products & categories..."}]]
        [:> ui/MenuItem (str @user-name& " @ " @org-name&)]
        [:> ui/MenuItem {:name "logout"
                         :active false
                         :onClick #(rf/dispatch [:logout])}]]])))

(defn container [body]
  [:> ui/Container {:class "main-container"}
   [c-top-nav]
   body
   [:div {:style {:height 100}}]])
