(ns vetd-app.buyer-fixtures
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :logout
 (fn [{:keys [db]} _]
   {:local-store {:session-token nil}
    :dispatch [:nav-login]}))

(defn header []
  (let [org-id& (rf/subscribe [:org-id])
        cart-items& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:cart_items {:buyer-id @org-id&}
                                     [:id]]]}])]
    (fn []
      [ut/flexer {:p {:style {:align-items :stretch
                              :justify-content :center
                              :f/dir :row}}}
       [[{:id :left-header
          :style {:f/grow 1}} [:div.logo]]
        [{:id :right-header
          :style {:f/grow 0
                  :padding "10px 20px 0 0"}}
         [rc/button
          :label "Logout"
          :on-click #(rf/dispatch [:logout])]]]])))
