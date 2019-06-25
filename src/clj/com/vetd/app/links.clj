(ns com.vetd.app.links
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.util :as ut]))

(def base-url "https://app.vetd.com/l/")

;; Links have a command (cmd) and input data, as well as metadata defining its validity.
;; They also have a key.
;; Some commands: TODO should be keyword or string?
;;   - create-verified-account
;;   - reset-password
;;   - accept-invitation
;; Possible input data (respective):
;;   - an account map
;;   - user id
;;   - map with org-id and role
;; Metadata:
;;   - max-uses-action (default = 1)
;;   - max-uses-read (default = 1)
;;   - expires-action (default = current time + 30 days) accepts unixtime
;;   - expires-read (default = unixtime 0, usually reset to future time upon action) accepts unixtime
;;   - uses-action (default = 0)
;;   - uses-read (default = 0)

;; TODO check if the time zone is being properly handled on its way to the DB
;; I'm using [:expires_action [:timestamp :with :time :zone]] in schema, so may have an issue
(defn create
  [{:keys [cmd input-data max-uses-action max-uses-read
           expires-action expires-read] :as link}]
  (let [[id idstr] (ut/mk-id&str)
        k (ut/mk-strong-key)]
    (db/insert! :links
                {:id id
                 :idstr idstr
                 :key k
                 :cmd (name cmd)
                 :input_data (str input-data)
                 :max_uses_action (or max-uses-action 1)
                 :max_uses_read (or max-uses-read 1)
                 :expires_action (java.sql.Timestamp.
                                  (or expires-action
                                      (+ (ut/now) (* 1000 60 60 24 30)))) ; 30 days from now
                 :expires_read (java.sql.Timestamp. (or expires-read 0))
                 :uses_action 0
                 :uses_read 0
                 :created (ut/now-ts)
                 :updated (ut/now-ts)})
    k))

(defn get-by-key
  [k]
  (-> [[:links {:key k}
        [:id :cmd :input_data :output_data
         :max_uses_action :max_uses_read
         :expires_action :expires_read
         :uses_action :uses_read]]]
      ha/sync-query
      :links
      first))

;; TODO defmulti over cmd?
