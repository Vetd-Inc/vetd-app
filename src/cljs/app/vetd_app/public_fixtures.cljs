(ns vetd-app.public-fixtures
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))


(defn header []
  (let [org-id& (rf/subscribe [:org-id])
        cart-items& (rf/subscribe [:gql/sub
                                   {:queries
                                    [[:cart_items {:buyer-id @org-id&}
                                      [:id]]]}])]
    (fn []
      [flx/row :full-header {:justify-content :center}
       [#{:logo}]])))


(defn container [body]
  [flx/col :container #{:buyer}
   [{:width "100%"} [header]]
   [{:width "100%"} body]
   [(constantly nil)]])
