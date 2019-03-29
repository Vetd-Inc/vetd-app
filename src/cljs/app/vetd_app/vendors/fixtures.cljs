(ns vetd-app.vendors.fixtures
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.fixtures :as cf]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn container [body]
  [:> ui/Container {:class "main-container"}
   [cf/c-top-nav [{:text "Preposals"
                   :pages #{:v/preposals}
                   :event [:v/nav-preposals]}
                  {:text "Products"
                   :pages #{:v/products}
                   :event [:v/nav-products]}
                  {:text "Profile"
                   :pages #{:v/profile}
                   :event [:v/nav-profile]}]]
   body
   [:div {:style {:height 100}}]])
