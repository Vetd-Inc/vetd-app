(ns vetd-app.public-fixtures
  (:require [vetd-app.util :as ut]
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
      [ut/flexer {:p {:id :full-header
                      :style {:justify-content :center
                              :f/dir :row}}}
       [[{:class [:logo]}]]])))


(defn container [body]
  [ut/flx {:p {:id :container
               :class [:buyer]}}
   [{:style {:width "100%"}} [header]]
   [{:style {:width "100%"}} body]])
