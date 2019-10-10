(ns vetd-app.buyers.fixtures
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.fixtures :as cf]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn container [body {:keys [no-spacer?]}]
  [:> ui/Container {:class "main-container"}
   [cf/c-top-nav [{:text "Browse Products"
                   :pages #{:b/search :b/product-detail}
                   :event [:b/nav-search]}
                  {:text "VetdRounds"
                   :pages #{:b/rounds :b/round-detail}
                   :event [:b/nav-rounds]}
                  {:text "Stack"
                   :pages #{:b/stack}
                   :event [:b/nav-stack]}]]
   body
   (when-not no-spacer?
     [:div {:style {:height 100}}])])

(defn appendable-container [body]
  [:<>
   [container nil {:no-spacer? true}]
   body
   [:div {:style {:height 100}}]])
