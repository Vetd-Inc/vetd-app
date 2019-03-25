(ns vetd-app.common.components
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn c-loader [{:keys [props]}]
  [:div.spinner props [:i] [:i]])
