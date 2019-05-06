(ns vetd-app.common.fx
  (:require [re-frame.core :as rf]))

(rf/reg-fx
 :confetti
 (fn [_]
   (.startConfetti js/window)
   (js/setTimeout #(.stopConfetti js/window) 3000)))
