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

;; these analytics identify's and group's are also being done on the backend, so these should probably be removed

(def trackers
  [{:subscription [:user]
    :id :user
    :dispatch-first? false
    :event-fn
    (fn [{:keys [id uname email]}]
      [:do-fx ;; this pattern (possible [:do-fx nil]) makes dispatch-first? more predictable
       (when id
         {:analytics/identify {:user-id id
                               :traits {:name uname
                                        :displayName uname                                      
                                        :email email
                                        :fullName uname ;; only for MailChimp integration
                                        }}})])}
   {:subscription [:active-org]
    :id :active-org
    :dispatch-first? false
    :event-fn
    (fn [{:keys [id oname buyer? groups]}]
      [:do-fx
       (when id
         {:analytics/identify {:user-id @(rf/subscribe [:user-id]) ;; TODO messy
                               :traits {:userStatus (if buyer? "Buyer" "Vendor")
                                        :oname oname
                                        :gname (s/join ", " (map :gname groups))}}
          :analytics/group {:group-id id
                            :traits {:name oname}}})])}
   {:subscription [:admin-of-groups]
    :id :admin-of-groups
    :dispatch-first? false
    :event-fn
    (fn [admin-of-groups]
      [:do-fx
       (when (seq admin-of-groups)
         (let [user-id @(rf/subscribe [:user-id]) ;; TODO messy
               admin-of-groups-str (s/join ", " (map :id admin-of-groups))]
           {:analytics/identify {:user-id user-id
                                 :traits {:groupAdmin admin-of-groups-str}}}))])}

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
