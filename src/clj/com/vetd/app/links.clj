(ns com.vetd.app.links
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [clj-time.core :as t]
            [clojure.edn :as edn]))

(def base-url "https://app.vetd.com/l/")

;; Links have a command (cmd) and input data, as well as metadata defining its validity.
;; They also have a key (a secret string used to trigger action and/or read output).
;; Some commands:
;;   - :create-verified-account
;;   - :reset-password
;;   - :accept-invitation
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
;; Output data will be determined by method implementation for that cmd.
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
                                      (+ (ut/now) (* 1000 60 60 24)))) ; 1 days from now
                 :expires_read (java.sql.Timestamp. (or expires-read 0)) ; can't read by default
                 :uses_action 0
                 :uses_read 0
                 :created (ut/now-ts)
                 :updated (ut/now-ts)})
    k))

(defn- parse-stored-link
  [link]
  (-> link
      (update :cmd keyword)
      (update :input-data edn/read-string)
      (update :output-data edn/read-string)))

(defn get-by-key
  [k]
  (some-> [[:links {:key k}
            [:id :cmd :input-data :output-data
             :max-uses-action :max-uses-read
             :expires-action :expires-read
             :uses-action :uses-read :deleted]]]
          ha/sync-query
          :links
          first
          parse-stored-link))

(defn- valid?
  [uses max-uses expires]
  (and (< uses max-uses)
       (< (ut/now) (ut/sql-ts->unix-ms expires))))

(def actionable?
  (comp (partial apply valid?)
        (juxt :uses-action :max-uses-action :expires-action)))

(def readable?
  (comp (partial apply valid?)
        (juxt :uses-read :max-uses-read :expires-read)))

;; If invoking, you probably want to use "do-action" instead.
;; If adding a new link cmd, defmethod onto this multi,
;; using the cmd keyword as dispatch-val. Your method will receive
;; the parsed link map (the most useful part being :input-data),
;; and should return whatever you want to be stored as output data.
;; Your return value can be anything that is EDN-encodeable.
;; Returning nil has the unique behavior of not setting output data.
(defmulti action (fn [link args]
                   (if (actionable? link)
                     (:cmd link)
                     :invalid)))

;; called when link does not exist, is expired, or already did maximum action's
(defmethod action :invalid [link args] nil)

(defn update-output
  "Store the output of a link action."
  [{:keys [id]} output]
  (db/update-any! {:id id
                   :output_data (str output)}
                  :links))

(defn- inc-uses
  "Increment the number of uses of a link in the DB.
  system - 'action' / 'read'"
  [link system]
  (let [field (keyword (str "uses_" system))
        curr-uses (->> (str "uses-" system)
                       keyword
                       (get link))]
    (db/update-any! {:id (:id link)
                     field (inc curr-uses)}
                    :links)))

(defn update-expires
  "Update the expires time for a given system (action / read)."
  [link system expires-unix-ms]
  (let [field (keyword (str "expires_" system))]
    (db/update-any! {:id (:id link)
                     field (java.sql.Timestamp. expires-unix-ms)}
                    :links)))

(defn do-action
  [link & [args]]
  (let [result (action link args)]
    (when-not (nil? result)
      (update-output link result)
      (inc-uses link "action"))
    result))

(defn do-action-by-key
  "Given a link key, try to do its action."
  [k & [args]]
  (do-action (get-by-key k)
             args))

(defn read-output
  [{:keys [output-data] :as link}]
  (when (readable? link)
    (inc-uses link "read")
    output-data))

(defn read-output-by-key
  "Given a link key, try to read its output."
  [k]
  (read-output (get-by-key k)))

(defmethod com/handle-ws-inbound :read-link
  [req ws-id sub-fn]
  (if-let [link (get-by-key (:key req))]
    (if-let [output-data (read-output link)]
      {:cmd (:cmd link)
       :output-data output-data}
      {:cmd :invalid})
    {:cmd :invalid}))

(defmethod com/handle-ws-inbound :do-link-action
  [{:keys [link-key] :as req} ws-id sub-fn]
  (if-let [link (get-by-key link-key)]
    (if-let [output-data (do-action link req)]
      {:cmd (:cmd link)
       :output-data output-data}
      {:cmd :invalid})
    {:cmd :invalid}))

