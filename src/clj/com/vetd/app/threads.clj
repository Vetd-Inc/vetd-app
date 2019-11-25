(ns com.vetd.app.threads
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.journal :as journal]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.auth :as auth]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.docs :as docs]
            [taoensso.timbre :as log]
            [clj-time.coerce :as tc]
            [clojure.string :as s]))

(defn insert-thread
  [{:keys [title group-id user-id org-id]}]
  (let [[id idstr] (ut/mk-id&str)]
    (db/insert! :threads
                {:id id
                 :idstr idstr
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :title title
                 :group_id group-id
                 :user_id user-id
                 :org_id org-id})
    id))

(defn insert-message
  [{:keys [text thread-id user-id org-id]}]
  (let [[id idstr] (ut/mk-id&str)]
    (db/insert! :messages
                {:id id
                 :idstr idstr
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :text text
                 :thread_id thread-id
                 :user_id user-id
                 :org_id org-id})
    id))

(defmethod com/handle-ws-inbound :g/threads.create
  [{:keys [title message group-id user-id org-id]} ws-id sub-fn]
  (when-let [thread-id (insert-thread {:title title
                                       :group-id group-id
                                       :user-id user-id
                                       :org-id org-id})]
    (do (insert-message {:text message
                         :thread-id thread-id
                         :user-id user-id
                         :org-id org-id})
        (com/sns-publish
         :ui-misc
         "Discussion Thread Created"
         (str "Discussion Thread Created\n\n"
              "Group ID: " group-id
              "\nTitle: " title
              "\nMessage: " message
              "\nAuthor's Org: " (-> org-id auth/select-org-by-id :oname))
         {:org-id org-id})))
  {})

;; TODO permissions
(defmethod com/handle-ws-inbound :g/threads.delete
  [{:keys [id]} ws-id sub-fn]
  (db/update-deleted :threads id))

(defmethod com/handle-ws-inbound :g/messages.delete
  [{:keys [id]} ws-id sub-fn]
  (db/update-deleted :messages id))

(defmethod com/handle-ws-inbound :g/threads.reply
  [{:keys [text thread-id user-id org-id]} ws-id sub-fn]
  (do (insert-message {:text text
                       :thread-id thread-id
                       :user-id user-id
                       :org-id org-id})
      (com/sns-publish
       :ui-misc
       "Reply Posted to Discussion Thread"
       (str "Reply Posted to Discussion Thread\n\n"
            "Thread ID: " thread-id
            "\nText: " text
            "\nAuthor's Org: " (-> org-id auth/select-org-by-id :oname))
       {:org-id org-id}))
  {})

