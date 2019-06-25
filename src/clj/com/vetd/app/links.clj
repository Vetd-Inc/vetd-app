(ns com.vetd.app.links
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.util :as ut]
            [clojure.edn :as edn]))

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

(defn parse-stored-link
  [link]
  (-> link
      (update :input-data edn/read-string)
      (update :output-data edn/read-string)))

(defn get-by-key
  [k]
  (some-> [[:links {:key k}
            [:id :cmd :input-data :output-data
             :max-uses-action :max-uses-read
             :expires-action :expires-read
             :uses-action :uses-read :deleted
             ]]]
          ha/sync-query
          :links
          first
          parse-stored-link))

;; TODO add expires constraint
(defn actionable?
  [{:keys [max-uses-action uses-action]}]
  (> max-uses-action uses-action))

;; if invoking, you probably want to use "do-action" instead
(defmulti action (fn [link]
                   (if (actionable? link)
                     (:cmd link)
                     :invalid)))

;; called when link does not exist, is expired, or already did maximum action's
(defmethod action :invalid [link] false)

(defn update-output
  "Store the output of a link action."
  [id output]
  (db/update-any! {:id id
                   :output_data (str output)}
                  :links))

(defn do-action
  [{:keys [id] :as link}]
  (update-output id (action link)))

(defn do-action-by-key
  "Given a link key, try to do its action."
  [k]
  (do-action (get-by-key k)))

;; TODO add expires constraint
(defn readable?
  [{:keys [max-uses-read uses-read]}]
  (> max-uses-read uses-read))

(defn read-output
  [{:keys [output-data :as link]}]
  (when (readable? link)
    output-data))

(defn read-output-by-key
  "Given a link key, try to read its output."
  [k]
  (read-output (get-by-key k)))




;; (do-action-by-key "0p7sb6vb24jfkkgdmnsaz37i")

