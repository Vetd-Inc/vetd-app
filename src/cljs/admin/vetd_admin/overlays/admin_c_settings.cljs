(ns vetd-admin.overlays.admin-c-settings
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]   
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))


(rf/reg-event-fx
 :a/broadcast-discounts
 (fn [{:keys [db]} [_ vendor-id]]
   {:ws-send {:payload {:cmd :a/broadcast-discounts}}}))

(defn c-overlay []
  [:div "admin-c-settings"
   [:> ui/Button {:color "teal"
                  :on-click #(rf/dispatch [:a/broadcast-discounts])}
    "Propagate Discounts"]])
