(ns vetd-app.buyers.fixtures
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.fixtures :as cf]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn container [body]
  [:> ui/Container {:class "main-container"}
   [cf/c-top-nav [{:text "Products & Categories"
                   :pages #{:b/search :b/product-detail}
                   :event [:b/nav-search]}
                  {:text "VetdRounds"
                   :pages #{:b/rounds :b/round-detail}
                   :event [:b/nav-rounds]}
                  {:text "Preposals"
                   :pages #{:b/preposals :b/preposal-detail}
                   :event [:b/nav-preposals]}]]
   body
   [:div {:style {:height 100}}]])
