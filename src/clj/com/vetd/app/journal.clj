(ns com.vetd.app.journal
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]))


(defn insert-entry [session-id {:keys [jtype entry]}]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :orgs
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :session_id session-id
                     :jtype jtype
                     :entry entry})
        first)))

(defn push-entry
  [entry]
  (db/insert! com/*session-id*
              entry))

(defn push-entry&sns-publish
  [topic subject message & [entry]]
  (push-entry (merge {:topic topic
                      :subject subject
                      :message message}
                     entry))
  (com/sns-publish topic subject message))
