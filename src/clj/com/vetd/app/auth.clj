(ns com.vetd.app.auth
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.email-client :as ec]
            [com.vetd.app.links :as l]
            [clojure.string :as st]
            [buddy.hashers :as bhsh]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]))

;; Sendgrid template id's
(def verify-account-template-id "d-d1f3509a0c664b4d84a54777714d5272")
(def password-reset-template-id "d-a782e6648d054f34b8453cbf8e14c007")
(def invite-user-to-org-template-id "d-5c8e34b8db4145d6b443472c5bf03d9c")

(defn select-org-by-name [org-name]
  (-> [[:orgs {:oname org-name}
        [:id :created :idstr :oname :buyer? :vendor? :short-desc :long-desc]]]
      ha/sync-query
      vals
      ffirst))

(defn select-org-by-id [org-id]
  (-> [[:orgs {:id org-id}
        [:id :created :idstr :oname :buyer? :vendor? :short-desc :long-desc]]]
      ha/sync-query
      vals
      ffirst))

(defn insert-org [org-name org-url buyer? vendor?]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :orgs
                    {:id id
                     :idstr idstr
                     :oname org-name
                     :url org-url
                     :buyer_qm buyer?
                     :vendor_qm vendor?
                     :created (ut/now-ts)
                     :updated (ut/now-ts)})
        first)))

(defn create-or-find-org
  [org-name org-url buyer? vendor?]
  (if-let [org (select-org-by-name org-name)]
    [false org]
    [true (insert-org org-name org-url buyer? vendor?)]))

(defn select-user-by-email
  [email & fields]
  (-> [[:users {:email (st/lower-case email)}
        (or (not-empty fields)
            [:id :uname :email])]]
      ha/sync-query
      vals
      ffirst))

(defn select-user-by-id
  [id & fields]
  (-> [[:users {:id id}
        (or (not-empty fields)
            [:id :uname :email])]]
      ha/sync-query
      vals
      ffirst))

(defn insert-user
  [uname email pwd-hash]
  (let [[id idstr] (ut/mk-id&str)]  
    (-> (db/insert! :users
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :uname uname
                     :email email
                     :pwd pwd-hash})
        first)))

(defn valid-creds?
  "If valid email / password combination, return that user map."
  [email pwd]
  (when-let [{pwd-hsh :pwd :as user} (select-user-by-email email :id :uname :email :pwd)]
    (when (bhsh/check pwd pwd-hsh)
      user)))

(defn valid-creds-by-id?
  "If valid email / password combination, return that user map."
  [user-id pwd]
  (when-let [{pwd-hsh :pwd :as user} (select-user-by-id user-id :id :uname :email :pwd)]
    (when (bhsh/check pwd pwd-hsh)
      user)))

(defn select-memb-by-ids
  [user-id org-id]
  (-> [[:memberships {:user-id user-id
                      :org-id org-id}
        [:id :user-id :org-id :created]]]
      ha/sync-query
      vals
      ffirst))

(defn select-memb-org-by-user-id
  [user-id]
  (-> [[:memberships {:user-id user-id}
        [:id :user-id :org-id
         [:org [:id :oname :id :created :buyer? :vendor?]]]]]
      ha/sync-query
      :memberships))

(defn select-memb-by-id
  [id]
  (-> [[:memberships
        {:id id}
        [:id :user-id :org-id :created]]]
      ha/sync-query
      :memberships))

(defn insert-memb
  [user-id org-id]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :memberships
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :user_id user-id
                     :org_id org-id})
        first)))

(defn delete-memb
  [memb-id]
  (db/hs-exe! {:delete-from :memberships
               :where [:= :id memb-id]}))

(defn create-or-find-memb
  [user-id org-id]
  (if (select-memb-by-ids user-id org-id)
    [false nil]
    [true (insert-memb user-id org-id)]))

(defn prepare-account-map
  "Normalizes and otherwise prepares an account map for insertion in DB."
  [account]
  (-> account
      (select-keys [:uname :email :pwd :org-name :org-url :org-type])
      (update :email st/lower-case)
      (update :pwd bhsh/derive)))

(defn send-verify-account-email
  [{:keys [email] :as account}]
  (let [link-key (l/create {:cmd :create-verified-account
                            :input-data (prepare-account-map account)
                            ;; 30 days from now
                            :expires-action (+ (ut/now) (* 1000 60 60 24 30))})]
    (ec/send-template-email
     email
     {:verify-link (str l/base-url link-key)}
     {:template-id verify-account-template-id})))

(defn create-account
  "Create a user account. (Really just start the process; send verify email)
  NOTE: org-type is a string: either 'buyer' or 'vendor'"
  ;; expecting these keys in account: [uname org-name org-url org-type email pwd]
  [{:keys [email] :as account}]
  (try
    (if (select-user-by-email email)
      {:email-used? true}
      (do (future (send-verify-account-email account))
          {}))
    (catch Throwable e
      (com/log-error e))))

(defn change-password
  [user-id pwd-hash]
  (db/update-any! {:id user-id
                   :pwd pwd-hash}
                  :users))

(defn send-password-reset-email
  [{:keys [email] :as creds}]
  (let [link-key (l/create {:cmd :password-reset
                            :input-data (-> creds
                                            (select-keys [:email :pwd])
                                            prepare-account-map)})]
    (ec/send-template-email
     email
     {:reset-link (str l/base-url link-key)}
     {:template-id password-reset-template-id})))

(defn password-reset-request
  [{:keys [email] :as creds}]
  (if (select-user-by-email email)
    (do (future (send-password-reset-email creds))
        {})
    {:no-account? true}))

(defn send-invite-user-to-org-email
  [{:keys [email org-id from-user-id] :as invite}]
  (let [link-key (l/create {:cmd :invite-user-to-org
                            :input-data (select-keys invite
                                                     [:email
                                                      :org-id
                                                      :from-user-id])
                            ;; 30 days from now
                            :expires-action (+ (ut/now) (* 1000 60 60 24 30))})
        from-user-name (:uname (select-user-by-id from-user-id :uname))
        from-org-name (:oname (select-org-by-id org-id))]
    (ec/send-template-email
     email
     {:invite-link (str l/base-url link-key)
      :from-user-name from-user-name
      :from-org-name from-org-name}
     {:template-id invite-user-to-org-template-id})))

(defn select-session-by-id
  [session-token]
  (-> [[:sessions
        {:token session-token}
        [:user-id]]]
      ha/sync-query
      :sessions
      first))

(defn select-active-session-by-token
  [session-token]
  (-> [[:sessions
        {:token session-token
         :deleted nil}
        [:user-id]]]
      ha/sync-query
      :sessions
      first))

(defn is-admin? [user-id]
  (-> (db/hs-query {:select [:id]
                    :from [:admins]
                    :where [:and
                            [:= :user_id user-id]
                            [:= :deleted nil]]})
      empty?
      not))

(defn admin-session? [session-token]
  (-> (db/hs-query {:select [:a.id]
                    :from [[:sessions :s]]
                    :join [[:admins :a] [:and
                                         [:= :a.user_id :s.user_id]
                                         [:= :a.deleted nil]]]
                    :where [:and
                            [:= :s.token session-token]
                            [:= :s.deleted nil]]})
      empty?
      not))

(defn insert-session
  [user-id]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :sessions
                    {:id id
                     :token (ut/mk-strong-key)
                     :user_id user-id
                     :created (ut/now-ts)})
        first)))

(defn login
  [{:keys [email pwd]}]
  (or (when-let [{:keys [id] :as user} (valid-creds? email pwd)]
        (let [memberships (-> id select-memb-org-by-user-id not-empty)
              admin? (is-admin? id)]
          (when (or (->> memberships (keep :org) not-empty)
                    admin?)
            {:logged-in? true
             :session-token (-> id insert-session :token)
             :user (dissoc user :pwd)
             :admin? admin?
             :memberships memberships})))
      {:logged-in? false
       :login-failed? true}))

;; Websocket handlers
(defmethod com/handle-ws-inbound :create-acct
  [m ws-id sub-fn]
  (create-account m))

(defmethod com/handle-ws-inbound :auth-by-creds
  [{:keys [email pwd] :as req} ws-id sub-fn]
  (login req))

(defmethod com/handle-ws-inbound :auth-by-session
  [{:keys [session-token] :as req} ws-id sub-fn]
  (if-let [{:keys [user-id]} (select-active-session-by-token session-token)]
    {:logged-in? true
     :session-token session-token
     :user (select-user-by-id user-id)
     :admin? (is-admin? user-id)     
     :memberships (select-memb-org-by-user-id user-id)}
    {:logged-in? false}))

(defmethod com/handle-ws-inbound :forgot-password.request-reset
  [{:keys [email pwd] :as req} ws-id sub-fn]
  (password-reset-request req))

(defmethod com/handle-ws-inbound :invite-user-to-org
  [{:keys [email org-id from-user-id] :as req} ws-id sub-fn]
  (let [user (select-user-by-email email)]
    (if (and user
             (select-memb-by-ids (:id user) org-id))
      {:already-member? true}
      (do (future (send-invite-user-to-org-email req))
          {}))))

(defmethod com/handle-ws-inbound :create-membership
  [{:keys [user-id org-id]} ws-id sub-fn]
  (create-or-find-memb user-id org-id)
  {})

(defmethod com/handle-ws-inbound :delete-membership
  [{:keys [id]} ws-id sub-fn]
  (delete-memb id)
  {})

(defmethod com/handle-ws-inbound :switch-membership
  [{:keys [user-id org-id]} ws-id sub-fn]
  (doseq [{:keys [id]} (select-memb-org-by-user-id user-id)]
    (delete-memb id))
  (create-or-find-memb user-id org-id))

(defmethod com/handle-ws-inbound :update-user
  [{:keys [user-id uname]} ws-id sub-fn]
  (db/update-any! {:id user-id
                   :uname uname}
                  :users))

(defmethod com/handle-ws-inbound :update-user-password
  [{:keys [user-id pwd new-pwd]} ws-id sub-fn]
  (if (valid-creds-by-id? user-id pwd)
    (do (change-password user-id
                         (bhsh/derive new-pwd))
        {:success? true})
    {:success? false}))

;; TODO logout!!!!!!!!!!!!!!!

;;;; Link action handlers
(defmethod l/action :create-verified-account
  [{:keys [input-data] :as link} _]
  (let [{:keys [uname email pwd org-name org-url org-type]} input-data]
    (when-not (select-user-by-email email) ; rare, but someone created an account with this email sometime after the link was made
      (let [user (insert-user uname email pwd)
            [_ org] (create-or-find-org org-name org-url (= org-type "buyer") (= org-type "vendor"))
            _ (create-or-find-memb (:id user) (:id org))]
        (l/update-expires link "read" (+ (ut/now) (* 1000 60 5))) ; allow read for next 5 mins
        {:session-token (-> user :id insert-session :token)}))))

(defmethod l/action :password-reset
  [{:keys [input-data] :as link} _]
  (let [{:keys [email pwd]} input-data]
    (when-let [{:keys [id]} (select-user-by-email email)]
      (do (change-password id pwd)
          (l/update-expires link "read" (+ (ut/now) (* 1000 60 5))) ; allow read for next 5 mins
          {:session-token (:token (insert-session id))}))))

(defmethod l/action :invite-user-to-org
  [{:keys [input-data] :as link} account]
  (let [{:keys [email org-id]} input-data
        org-name (:oname (select-org-by-id org-id))
        signup-flow? (every? (partial contains? account) [:uname :pwd])]
    ;; this link action is 'overloaded'
    (if-not signup-flow?
      ;; standard usage of the link (i.e., the initial click from email)
      (if-let [{:keys [id]} (select-user-by-email email)]
        ;; the account already exists, just add them to org, and give a session token
        (do (create-or-find-memb id org-id)
            (l/update-expires link "read" (+ (ut/now) (* 1000 60 5))) ; allow read for next 5 mins
            {:user-exists? true
             :org-name org-name
             :session-token (-> id insert-session :token)})
        ;; they will need to "signup by invite"
        (do (l/update-max-uses link "action" 2) ; allow another use (will be via ws :do-link-action)
            (l/update-expires link "read" (+ (ut/now) (* 1000 60 5))) ; allow read for next 5 mins
            {:user-exists? false
             :org-name org-name}))
      ;; reusing link action to create account + add to org
      ;; used from ws, :do-link-action cmd
      (let [{:keys [uname pwd]} account
            {:keys [id]} (insert-user uname email (bhsh/derive pwd))]
        (when id ; user was successfully created
          (create-or-find-memb id org-id)
          ;; this 'output' will be read immediately from the ws results
          ;; i.e., it won't be read from a link read
          {:org-name org-name
           :session-token (-> id insert-session :token)})))))
