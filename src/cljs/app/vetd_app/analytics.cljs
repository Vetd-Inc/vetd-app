(ns vetd-app.analytics
  (:require [re-frame.core :as rf])
  (:require-macros [com.vetd.app.env :as env]))

(.log js/console (str "segment key: " (env/segment-frontend-write-key)))

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
