(ns vetd-app.blocker
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]))


(def show-blocker? (r/atom false))

(rf/reg-event-fx
 :set-blocker-content
 (fn [{:keys [db]} [_ c]]
   {:db (assoc db :blocker-content c)}))

(rf/reg-sub
 :blocker-content
  (fn [db _]
    (:blocker-content db)))

(defn blocker
  []
  (when @show-blocker?
    [:div {:id :blocker
           :style {:position :fixed
                   :top 0
                   :left 0
                   :width "100%"
                   :height "100%"
                   :opacity "50%"
                   :background-color "#00000080"}
           :on-click #(reset! show-blocker? false)}
     [:div {:style {:width "50%"
                    :height "100%"
                    :position :fixed
                    :left 0
                    :top 0
                    :background-color "#FFF"
                    :border-right "double 5px #999"
                    :overflow-y :scroll}}
      [:div @(rf/subscribe [:blocker-content])]]]))
