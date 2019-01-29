(ns vetd-app.pages.home
  (:require [vetd-app.util :as ut]
            [vetd-app.blocker :as bl]
            [vetd-app.websockets :as ws]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [goog.functions]))

(defn home-page []
  [:div "VETD!"])
