(ns vetd-app.orgs.fx
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :o/invite-user-to-org
 (fn [{:keys [db]} [_ email org-id from-user-id]]
   {:ws-send {:payload {:cmd :invite-user-to-org
                        :return {:handler :o/invite-user-to-org-return
                                 :email email}
                        :email email
                        :org-id org-id
                        :from-user-id from-user-id}}
    :analytics/track {:event "Invite User to Organization"
                      :props {:category "Account"
                              :label org-id}}}))

(rf/reg-event-fx
 :o/invite-user-to-org-return
 (fn [{:keys [db]} [_ results {{:keys [email]} :return}]]
   (if (:already-member? results)
     {:db (assoc-in db [:page-params :bad-input] :invite-email-address)
      :toast {:type "error" 
              :title "Error"
              :message "That email address already belongs to a current member."}}
     {:toast {:type "success"
              :title "Invitation Sent"
              :message (str "An invitation has been sent to " email ".")}
      :dispatch [:stop-edit-field "invite-email-address"]})))
