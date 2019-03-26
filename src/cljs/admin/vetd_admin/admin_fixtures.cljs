(ns vetd-admin.admin-fixtures
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]))

;; Components
(def top-nav-items
  [{:text "Admin Search"
    :pages #{:a/search}
    :event [:a/nav-search]}
   {:text "Form Templates"
    :pages #{:a/form-templates}
    :event [:a/nav-form-templates]}])

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
        [:> ui/MenuItem (str @user-name& " @ " @org-name&)]
        [:> ui/MenuItem {:name "logout"
                         :active false
                         :onClick #(rf/dispatch [:logout])}]]])))

(defn container [body]
  [:> ui/Container {:class "main-container"}
   [c-top-nav]
   body
   [:div {:style {:height 100}}]])
