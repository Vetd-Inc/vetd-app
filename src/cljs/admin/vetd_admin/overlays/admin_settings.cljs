(ns vetd-admin.overlays.admin-settings
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]   
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :a/create-community
 (fn [{:keys [db]} [_ community-name admin-org-id]]
   {:ws-send {:payload {:cmd :a/create-community
                        :admin-org-id admin-org-id
                        :community-name community-name}}}))

(defn c-overlay []
  (let [cname& (r/atom "")
        org-id& (rf/subscribe [:org-id])]
    (fn []
      [:div
       [ui/input {:value @cname&
                  :on-change (fn [this]
                               (reset! cname& (-> this .-target .-value)))}]
       [:> ui/Button {:color "teal"
                      :on-click #(rf/dispatch [:a/create-community @cname& @org-id&])}
        "Create Community"]])))
