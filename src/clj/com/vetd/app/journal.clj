(ns com.vetd.app.journal
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]))

(defn insert-entry [session-id {:keys [jtype] :as entry}]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :journal_entries
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :session_id session-id
                     :jtype (name jtype)
                     :entry entry})
        first)))

(defn push-entry
  [entry]
  (try
    (insert-entry com/*session-id*
                  entry)
    (catch Throwable e
      (com/log-error e))))

(defn push-entry&sns-publish
  [topic subject message & [entry]]
  (push-entry (merge {:topic topic
                      :subject subject
                      :message message}
                     entry))
  (com/sns-publish topic subject message))
