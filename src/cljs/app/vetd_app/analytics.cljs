(ns vetd-app.analytics
  (:require [re-frame.core :as rf])
  (:require-macros [com.vetd.app.env :as env]))

(when js/analytics
  (js/analytics.load (env/segment-frontend-write-key)))

(rf/reg-fx
 :analytics/identify
 (fn [{:keys [user-id traits]}]
   (js/analytics.track user-id (clj->js traits))))

(rf/reg-fx
 :analytics/track
 (fn [{:keys [event props]}]
   (js/analytics.track event (clj->js props))))

;; (rf/reg-fx
;;  :analytics/page
;;  )

;; (rf/reg-fx
;;  :analytics/group
;;  )
