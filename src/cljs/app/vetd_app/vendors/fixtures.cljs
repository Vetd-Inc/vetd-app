(ns vetd-app.vendors.fixtures
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.fixtures :as cf]))

(defn container [body]
  [:> ui/Container {:class "main-container"}
   [cf/c-top-nav [{:text "Estimates"
                   :pages #{:v/preposals}
                   :event [:v/nav-preposals]}
                  {:text "Company Profile"
                   :pages #{:v/profile}
                   :event [:v/nav-profile]}
                  {:text "Your Products"
                   :pages #{:v/products}
                   :event [:v/nav-products]}
                  {:text "VetdRounds"
                   :pages #{:v/rounds}
                   :event [:v/nav-rounds]}]]
   body
   [:div {:style {:height 100}}]])
