(ns com.vetd.app.feeds
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [clojure.core.async :as a]
            [taoensso.timbre :as log]))

(defonce last-journal-entry-id& (atom 0))

#_ (def feed-event-puller-thread& (atom nil))

(defonce feed-event-puller-thread& (atom nil))

(defonce trigger-pull-feed-events (a/chan (a/dropping-buffer 1)))

(def valid-journal-entry-types #{:round-started
                                 ;; no longer showing the above event.
                                 ;; instead, showing round after initiation
                                 :round-init-form-completed
                                 :round-winner-declared
                                 :stack-update-rating
                                 :stack-add-items
                                 :preposal-request
                                 :buy-request
                                 :complete-vendor-profile-request
                                 :complete-product-profile-request})

(defn select-one-new-journal-entry []
  (-> {:select [:j.id
                :j.created
                :j.jtype
                :j.entry]
       :from [[:journal_entries :j]]
       :left-join [[:feed_events :f]
                   [:= :f.journal_entry_id :j.id]]
       :where [:and
               [:> :j.id @last-journal-entry-id&]
               ;; for safety
               ;; [:> :j.created (java.sql.Timestamp.
               ;;                 (- (ut/now)
               ;;                    (* 1000 60 60 24)))]
               [:= :f.id nil]
               [:in :j.jtype (mapv name valid-journal-entry-types)]]
       :order-by {:j.id :asc}
       :limit 1}
      db/hs-query
      first))

(defn insert-feed-event
  [{:keys [journal-entry-id journal-entry-created org-id ftype data]}]
  (let [[id idstr] (ut/mk-id&str)]
    (db/insert! :feed_events
                {:id id
                 :idstr idstr
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :journal_entry_id journal-entry-id
                 :journal_entry_created journal-entry-created
                 :org_id org-id
                 :ftype ftype
                 :data data})
    id))

(defn journal-entry->feed-event [{:keys [id created jtype entry]}]
  (when-let [org-id (or (entry "org-id")
                        (entry "buyer-org-id"))]
    {:journal-entry-id id
     :journal-entry-created created
     :org-id org-id
     :ftype jtype
     :data (assoc entry
                  :journal-entry-id id
                  :journal-entry-created created
                  :org-id org-id
                  :ftype jtype)}))

(defn do-feed-event-puller []
  (try
    (let [[_ ch] (a/alts!! [trigger-pull-feed-events
                            (a/timeout 3000)])]
      (when (= ch trigger-pull-feed-events)
        (while
            (when-let [{:keys [id] :as entry} (select-one-new-journal-entry)]
              (swap! last-journal-entry-id& (partial max id))
              (some-> entry
                      journal-entry->feed-event
                      insert-feed-event)
              true))))
    (catch Throwable e
      (com/log-error e)
      (Thread/sleep 5000))))

(defn start-feed-event-puller-thread []
  (when (and (not env/building?)
             (nil? @feed-event-puller-thread&))
    (reset! feed-event-puller-thread&
            (future
              (log/info "Starting feed-event-puller")
              (while (not @com/shutdown-signal)
                (#'do-feed-event-puller))
              (log/info "Stopped feed-event-puller")))))



(start-feed-event-puller-thread) ;; TODO calling this here is gross -- Bill
