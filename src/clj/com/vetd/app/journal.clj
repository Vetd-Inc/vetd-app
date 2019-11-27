(ns com.vetd.app.journal
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [com.vetd.app.feeds :as feeds]
            [clojure.core.async :as a]
            [circleci.analytics-clj.core :as analytics]))

(def segment-client (env/build-ignore
                     (analytics/initialize
                      (env/all-env :segment-backend-write-key))))

(defn jtype->segment-event
  [jtype props]
  (case jtype
    ;; Rounds
    :round-started {:event "Start"
                    :props {:category "Round"
                            :label (cond
                                     (seq (:product-ids props)) "Product or Duplicate"
                                     (seq (:category-names props)) "Category"
                                     :else "None")}}
    :round-init-form-completed {:event "Initiation Form Saved"
                                :props {:category "Round"
                                        :label (:round-id props)}}
    :round-winner-declared {:event "Declare Winner"
                            :props {:category "Round"
                                    :label (:round-id props)}}
    ;; Products
    :buy-request {:event "Buy"
                  :props {:category "Product"
                          :label (:product-name props)}}
    ;; Signup
    :user-created {:event "Signup Complete"
                   :props {:category "Accounts"
                           :label (case (:origin props)
                                    :signup-form "Standard"
                                    :org-invite "By Explicit Invite"
                                    "")}}
    nil))

(defn segment-track
  [{:keys [jtype] :as event-props}]
  (try
    (when-let [{:keys [event props]} (jtype->segment-event jtype event-props)]
      (analytics/track segment-client
                       (str (or com/*user-id*
                                (:user-id event-props)))
                       event
                       props))
    (catch Throwable e
      (com/log-error e))))

(defn segment-group
  [user-id group-id traits]
  (try
    (analytics/group segment-client (str user-id) (str group-id) traits)
    (catch Throwable e
      (com/log-error e))))

(defn segment-identify
  [user-id traits]
  (try
    (analytics/identify segment-client (str user-id) traits)
    (catch Throwable e
      (com/log-error e))))

(defn insert-entry
  [session-id {:keys [jtype] :as entry}]
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

;; TODO wrap in a future?
(defn push-entry
  [entry]
  (future
    (segment-track entry)
    (try
      (insert-entry com/*session-id*
                    entry)
      (a/>!! feeds/trigger-pull-feed-events true)
      (catch Throwable e
        (com/log-error e)))))

;; don't use this
(defn push-entry&sns-publish
  [topic subject message & [entry]]
  (do (push-entry (merge {:topic topic
                          :subject subject
                          :message message}
                         entry))
      (com/sns-publish topic subject message)))
