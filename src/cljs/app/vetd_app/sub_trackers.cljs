(ns vetd-app.sub-trackers
  (:require [re-frame.core :as rf]
            [vimsical.re-frame.fx.track :as track]
            [clojure.string :as s]))

;; put the pages that you want to call analytics/page on in here
;; some pages have custom analytics in their route event so they may not be here...
(def page->title
  {:login "Login"
   :join-org-signup "Signup By Invite"
   :settings "Settings"
   :forgot-password "Forgot Password"
   :b/search "Buyers Products & Categories"
   :b/rounds "Buyers Rounds"
   :b/stack "Buyers Stack"
   :v/products "Vendors Products"
   :v/profile "Vendor Edit Profile"
   :v/preposals "Vendor Preposals"
   :v/rounds "Vendor Rounds"
   :g/home "Groups Home"
   :g/settings "Groups Settings"})

(def trackers
  [{:subscription [:user]
    :id :user
    :dispatch-first? false
    :event-fn
    (fn [{:keys [id uname email]}]
      ;; This pattern... the sometimes: [:do-fx nil]
      ;; makes dispatch-first? more predictable.
      [:do-fx 
       (when id
         {:analytics/identify {:user-id id}})])}
   {:subscription [:active-org]
    :id :active-org
    :dispatch-first? false
    :event-fn
    (fn [{:keys [id]}]
      [:do-fx
       (when id
         {:analytics/group {:group-id id}})])}
   {:subscription [:page]
    :id :page-change
    :dispatch-first? false
    :event-fn
    (fn [page]
      [:do-fx
       (when-let [title (page->title page)]
         {:analytics/page {:name title}})])}])

(rf/reg-event-fx
 :reg-sub-trackers
 (fn []
   {::track/register trackers}))

(rf/reg-event-fx
 :dispose-sub-trackers
 (fn []
   {::track/dispose (map #(select-keys % [:id]) trackers)}))
