(ns vetd-app.vendor-fixtures
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(defn header []
  [:div [rc/button
         :label "Logout"
         :on-click #(rf/dispatch [:logout])]])
