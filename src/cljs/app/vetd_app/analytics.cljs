(ns vetd-app.analytics
  (:require [re-frame.core :as rf])
  (:require-macros [com.vetd.app.env :as env]))

(when js/analytics
  (if-not js/window.suppressAnalytics
    (js/analytics.load (env/segment-frontend-write-key))
    (println "analytics are suppressed for admins")))

(rf/reg-fx
 :analytics/identify
 (fn [{:keys [user-id traits]}]
   (js/analytics.identify user-id (clj->js traits))))

(rf/reg-fx
 :analytics/track
 (fn [{:keys [event props]}]
   (js/analytics.track event (clj->js props))))

(rf/reg-fx
 :analytics/page
 (fn [{:keys [name props]}]
   (js/analytics.page name (clj->js props))))

(rf/reg-fx
 :analytics/group
 (fn [{:keys [group-id traits]}]
   (js/analytics.group group-id (clj->js traits))))
