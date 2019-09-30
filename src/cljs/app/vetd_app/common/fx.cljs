(ns vetd-app.common.fx
  (:require [accountant.core :as acct]
            [re-frame.core :as rf]))

(defn validated-dispatch-fx
  [db event validator-fn]
  (let [[bad-input message] (validator-fn)]
    (if bad-input
      {:db (assoc-in db [:page-params :bad-input] bad-input)
       :toast {:type "error" 
               :title "Error"
               :message message}}
      {:db (assoc-in db [:page-params :bad-input] nil)
       :dispatch event})))

(rf/reg-fx
 :confetti
 (fn [_]
   (.startConfetti js/window)
   (js/setTimeout #(.stopConfetti js/window) 3000)))

;; given a React component ref, scroll to it on the page
(rf/reg-fx
 :scroll-to
 (fn [ref]
   (when ref
     (.scrollIntoView ref (clj->js {:behavior "smooth"
                                    :block "start"})))))

(rf/reg-event-fx
 :scroll-to
 (fn [{:keys [db]} [_ ref-key]]
   {:scroll-to (-> db :scroll-to-refs ref-key)}))

(rf/reg-event-fx
 :reg-scroll-to-ref
 (fn [{:keys [db]} [_ ref-key ref]]
   {:db (assoc-in db [:scroll-to-refs ref-key] ref)}))

(rf/reg-event-fx
 :read-link
 (fn [{:keys [db]} [_ link-key]]
   {:ws-send {:payload {:cmd :read-link
                        :return {:handler :read-link-result
                                 :link-key link-key}
                        :key link-key}}}))

(rf/reg-event-fx
 :read-link-result
 [(rf/inject-cofx :local-store [:join-group-link-key])]
 (fn [{:keys [db local-store]} [_ {:keys [cmd output-data] :as results} {{:keys [link-key]} :return}]]
   (case cmd ; make sure your case nav's the user somewhere (often :nav-home)
     :create-verified-account {:toast {:type "success"
                                       :title "Account Verified"
                                       :message "Thank you for verifying your email address."}
                               :local-store {:session-token (:session-token output-data)}
                               :dispatch-later [{:ms 100 :dispatch [:ws-get-session-user]}
                                                ;; first-time login, go to stack page
                                                {:ms 200 :dispatch [:nav-home true]}
                                                (when (:join-group-link-key local-store)
                                                  {:ms 300 :dispatch [:read-link (:join-group-link-key local-store)]})
                                                {:ms 500 :dispatch [:do-fx {:analytics/track
                                                                            {:event "Signup Complete"
                                                                             :props {:category "Accounts"
                                                                                     :label "Standard"}}}]}]}
     :password-reset {:toast {:type "success"
                              :title "Password Updated"
                              :message "Your password has been successfully updated."}
                      :local-store {:session-token (:session-token output-data)}
                      :dispatch-later [{:ms 100 :dispatch [:ws-get-session-user]}
                                       {:ms 200 :dispatch [:nav-home]}]}
     :invite-user-to-org (if (:user-exists? output-data)
                           {:toast {:type "success"
                                    ;; this is a vague "Joined!" because it could
                                    ;; be an org or a community
                                    ;; also see fx :join-org-signup-return
                                    :title "Joined!"
                                    :message (str "You accepted an invitation to join " (:org-name output-data))}
                            :local-store {:session-token (:session-token output-data)}
                            :dispatch-later [{:ms 100 :dispatch [:ws-get-session-user]}
                                             {:ms 200 :dispatch [:nav-home]}]}
                           {:db (assoc db :signup-by-link-org-name (:org-name output-data))
                            :dispatch [:nav-join-org-signup link-key]})
     :g/join (if (:logged-in? db)
               {:dispatch-later [(when-not (= (:page db) :b/stack)
                                   {:ms 100 :dispatch [:nav-home]})
                                 {:ms 200
                                  :dispatch
                                  (if (:join-group-link-key local-store)
                                    ;; they don't need to visually consent because they were on a custom login or signup page
                                    ;; that already informed them that they will be join to the community
                                    [:g/accept-invite link-key]
                                    ;; they need to consent
                                    [:modal
                                     {:header (str "Join the " (:group-name output-data) " community")
                                      :content (str "Do you want to join the " (:group-name output-data) " community?")
                                      :buttons [{:text "Cancel"}
                                                {:text "Join"
                                                 :event [:g/accept-invite link-key]
                                                 :color "blue"}]
                                      :size "tiny"}])}]
                :local-store {:join-group-link-key nil
                              :join-group-name nil}}
               {:local-store {:join-group-link-key link-key
                              :join-group-name (:group-name output-data)}
                :dispatch-later [{:ms 100 :dispatch [:nav-signup :buyer]}]})
     {:toast {:type "error"
              :title "That link is expired or invalid."}
      :dispatch [:nav-home]})))

(rf/reg-sub
 :bad-input
 :<- [:page-params]
 (fn [{:keys [bad-input]}] bad-input))

(rf/reg-event-fx
 :modal
 (fn [{:keys [db]} [_ {:keys [header content size] :as props}]]
   (do (-> db :modal :showing?& (reset! true)) ;; nastiness
       {:db (update db :modal merge props)})))

(rf/reg-sub
 :modal
 (fn [{:keys [modal]}] modal))

(rf/reg-fx
 :nav
 (fn nav-fx [{:keys [path query external-url]}]
   (if external-url
     (js/setTimeout #(.assign (aget js/window "location") external-url) 1000)
     (acct/navigate! path query))))
