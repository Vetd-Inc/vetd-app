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
  [:div [rc/button
         :label "Logout"
         :on-click #(rf/dispatch [:logout])]])























































