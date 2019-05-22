(ns com.vetd.app.auth
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [clojure.string :as st]
            [buddy.hashers :as bhsh]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]))

;; this seems secure
(defn mk-session-token []
  (let [base 1000000
        f #(ut/base36->str
            (+ base (rand-int (- Integer/MAX_VALUE base))))]
    (-> (concat (f) (f) (f) (f))
        shuffle
        clojure.string/join)))

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
  [uname email pwd]
  (let [[id idstr] (ut/mk-id&str)]  
    (-> (db/insert! :users
                    {:id id
                     :idstr idstr
                     :uname uname
                     :email (st/lower-case email)
                     :pwd (bhsh/derive pwd)})
        first)))

(defn valid-creds?
  "If valid email / password combination, return that user map."
  [email pwd]
  (when-let [{pwd-hsh :pwd :as user} (select-user-by-email email :id :uname :email :pwd)]
    (when (bhsh/check pwd pwd-hsh)
      user)))

(defn select-memb-by-ids
  [user-id org-id]
  (-> [[:memberships
        {:user-id user-id
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

(defn create-account
  "Create a user account.
  NOTE: org-type is a string: either 'buyer' or 'vendor'"
  [{:keys [uname org-name org-url org-type email pwd]}]
  (try
    (if (select-user-by-email email)
      {:email-used? true}
      (let [user (insert-user uname email pwd)
            [org-created? org] (create-or-find-org org-name org-url (= org-type "buyer") (= org-type "vendor"))
            [memb-created? memb] (create-or-find-memb (:id user) (:id org))]
        {:user-created? true
         :user user
         :org-created? org-created?
         :org org
         :memb-created? memb-created?
         :memb memb}))
    (catch Throwable e
      (log/error e))))

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
                     :token (mk-session-token)
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

(defmethod com/handle-ws-inbound :create-membership
  [{:keys [user-id org-id]} ws-id sub-fn]
  (create-or-find-memb user-id org-id))

(defmethod com/handle-ws-inbound :delete-membership
  [{:keys [id]} ws-id sub-fn]
  (delete-memb id))

(defmethod com/handle-ws-inbound :switch-membership
  [{:keys [user-id org-id]} ws-id sub-fn]
  (doseq [{:keys [id]} (select-memb-org-by-user-id user-id)]
    (delete-memb id))
  (create-or-find-memb user-id org-id))

;; TODO logout!!!!!!!!!!!!!!!
