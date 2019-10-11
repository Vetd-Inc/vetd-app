(ns com.vetd.app.email-client
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [taoensso.timbre :as log]
            [clj-http.client :as client]))

(def sendgrid-api-key "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")
(def sendgrid-api-url "https://api.sendgrid.com/v3/")
(def sendgrid-default-from "info@vetd.com")
(def sendgrid-default-from-name "Vetd Team")
(def sendgrid-default-template-id "d-0942e461a64943018bd0d1d6cf711e21") ; "Simple" template

(def common-opts
  {:as :json
   :content-type :json
   :coerce :always
   :throw-exceptions false
   :headers {"Authorization" (str "Bearer " sendgrid-api-key)}})

(defonce scheduled-email-thread& (atom nil))
(def last-email-sent-ts& (atom 0))


;; TODO This will get trickier when we have two app server instances running again
(defn select-next-email&recipient [max-ts]
  (-> {:select [[:%max.esl.created :max-created]
                [:m.id :membership-id]
                [:m.user_id :user-id]
                [:m.org_id :org-id]]
       :from [[:orgs :o]]
       :join [[:memberships :m]
              [:and
               [:= :m.deleted nil]
               [:= :m.org_id :o.id]]]
       :left-join [[:email_sent_log :esl]
                   [:= :esl.user_id :m.user_id]]
       :where [:and
               [:= :o.deleted nil]
               [:= :o.buyer_qm true]]
       :group-by [:m.id :m.user_id :m.org_id]
       :having [:or
                [:= :%max.esl.created nil]
                [:< :%max.esl.created nil (java.sql.Timestamp. max-ts)]]}
      db/hs-query
      first))

(defn do-scheduled-email-puller []
#_  (try
    (let [[_ ch] (a/alts!! [trigger-pull-scheduled-emails
                            (a/timeout 3000)])]
      (when (= ch trigger-pull-scheduled-emails)
        (while
            (when-let [{:keys [id] :as entry} (select-one-new-journal-entry)]
              (def e1 entry)
              (swap! last-journal-entry-id& (partial max id))
              (some-> entry
                      journal-entry->scheduled-email
                      insert-scheduled-email)
              true))))
    (catch Throwable e
      (def ex1 e)
      (com/log-error e)
      (Thread/sleep 5000))))



(defn start-scheduled-emailer-thread []
  (when (and (not env/building?)
             (nil? @scheduled-email-thread&))
    (reset! scheduled-email-thread&
            (future
              (log/info "Starting scheduled-emailer")
              (while (not @com/shutdown-signal)
                (#'do-scheduled-email-puller))
              (log/info "Stopped scheduled-emailer")))))

#_

(start-scheduled-email-puller-thread) ;; TODO calling this here is gross -- Bill


;; NOTE when :success is true, :resp is nil.
(defn- request
  [endpoint & [params headers]]
  (try (let [resp (-> (client/post (str sendgrid-api-url endpoint)
                                   (merge-with merge 
                                               common-opts
                                               {:form-params params}
                                               {:headers headers}))
                      :body)]
         {:success (not (seq (:errors resp)))
          :resp resp})
       (catch Exception e
         (com/log-error e)
         {:success false
          :resp {:error {:message (.getMessage e)}}})))

(defn send-template-email
  [to data & [{:keys [from from-name template-id]}]]
  (request "mail/send"
           {:personalizations [{:to [{:email to}]
                                :dynamic_template_data data}]
            :from {:email (or from sendgrid-default-from)
                   :name (or from-name sendgrid-default-from-name)}
            :template_id (or template-id sendgrid-default-template-id)}))

;;;; Example Usage
#_(send-template-email
   "chris@vetd.com"
   {:subject "Vetd Buying Platform"
    :preheader "You're going to want to see what's in this email"
    :main-content "Here is some example content."})












