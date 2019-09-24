(ns vetd-app.analytics
  (:require [re-frame.core :as rf])
  (:require-macros [com.vetd.app.env :as env]))

(when js/analytics
  (js/analytics.load (env/segment-frontend-write-key)))

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

(defn identify-map
  [{:keys [id uname email] :as user} memberships admin-of-groups]
  (let [{:keys [buyer? oname]} (some-> memberships first :org)]
    {:user-id id
     :traits {:name uname
              :displayName uname                                      
              :email email
              :userStatus (if buyer? "Buyer" "Vendor")
              :oName oname
              ;; :gName
              ;; :admin
              ;; only for MailChimp integration
              :fullName uname}}))
