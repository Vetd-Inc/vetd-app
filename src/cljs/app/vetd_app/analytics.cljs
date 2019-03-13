(ns vetd-app.analytics
  (:require [re-frame.core :as rf])
  (:require-macros vetd-app.env))

(.log js/console (str "segment key: " vetd-app.env/segment-frontend-write-key))

;; (rf/reg-fx
;;  :analytics/identify
;;  )

;; (rf/reg-fx
;;  :analytics/track
;;  )

;; (rf/reg-fx
;;  :analytics/page
;;  )

;; (rf/reg-fx
;;  :analytics/group
;;  )
