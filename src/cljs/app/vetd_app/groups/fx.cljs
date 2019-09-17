(ns vetd-app.groups.fx
  (:require [vetd-app.util :as util]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 :g/accept-invite
 (fn [{:keys [db]} [_ link-key]]
   {:ws-send {:payload {:cmd :do-link-action
                        :return {:handler :g/accept-invite.return}
                        :link-key link-key
                        :org-id (util/db->current-org-id db)}}}))

(rf/reg-event-fx
 :g/accept-invite.return
 (fn [{:keys [db]} [_ {:keys [error]}]]   
   (if error
     {:toast {:type "error" 
              :title "Error"
              :message error}}
     {:toast {:type "success"
              :title "Community Joined!"}
      :dispatch-later [;; TODO needs to refresh page or need to refresh memberships in app-db
                       {:ms 100 :dispatch [:ws-get-session-user]}
                       (when-not (= (:page db) :b/stack)
                         {:ms 200 :dispatch [:g/nav-home]})]})))
