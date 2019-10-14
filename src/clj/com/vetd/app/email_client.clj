(ns com.vetd.app.email-client
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [taoensso.timbre :as log]
            [clj-http.client :as client]
            [clj-time.periodic :as t-per]
            [tick.core :as tick]))


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
(defonce next-scheduled-event& (atom nil))


(defn monday? [x] (= (tick/day-of-week x) #time/day-of-week "MONDAY"))

(defn nine-am-pst? [x] (= (tick/hour x) 16))

(defn calc-next-due-ts [dt]
  (->> (tick/range (tick/truncate dt :hours)
                   (tick/+ dt (tick/new-period 8 :days))
                   (tick/new-duration 1 :hours))
       (filter #(and (monday? %)
                     (nine-am-pst? %)))
       first))

(defn tick->ts [t]
  (tick/millis (tick/between (tick/epoch) t)))


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




;; TODO This will get trickier when we have multiple app server instances running again
(defn select-next-email&recipient [max-ts]
  (-> {:select [[:%max.esl.created :max-created]
                [:m.id :membership-id]
                [:m.user_id :user-id]
                [:m.uname :uname]                
                [:m.email :email]                
                [:m.org_id :org-id]
                [:m.oname :oname]]
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
                [:< :%max.esl.created nil (java.sql.Timestamp. max-ts)]]
       :limit 1}
      db/hs-query
      first))

(defn get-weekly-auto-email-data--product-renewals-soon [org-id days-forward limit]
  (-> [[:stack-items {:_where
                                 {:_and [{:renewal-reminder {:_eq true}}
                                         {:renewal-date {:_gte (str (ut/now-ts))}}
                                         {:renewal-date {:_lte (->> (tick/new-period days-forward :days)
                                                                    (tick/+ (tick/now))
                                                                    tick->ts
                                                                    java.sql.Timestamp.
                                                                    str)}}
                                         {:price-period {:_eq "annual"}}
                                         {:deleted {:_is_null true}}
                                         {:status {:_eq "current"}}
                                         {:buyer_id {:_eq (str org-id)}}]}
                                 :_order_by {:renewal-date :asc}
                                 :_limit limit}
                   [:id :idstr :status
                    :price-amount :price-period :rating
                    :renewal-date :renewal-reminder
                    [:buyer 
                     [:id :oname]]
                    [:product
                     [:id :pname :idstr :logo
                      [:vendor
                       [:id :oname :idstr :short-desc]]
                      [:categories {:ref-deleted nil}
                       [:id :idstr :cname]]]]]]]
      ha/sync-query
      :stack-items))

(defn get-weekly-auto-email-data--active-rounds [org-id]
  (-> [[:rounds {:deleted nil
                 :buyer-id org-id}
        [:id :title]]]
      ha/sync-query
      :rounds))

(defn get-weekly-auto-email-data--communities-num-new-discounts [org-id days-back]
  (->> {:select [:%count.gd.id]
        :from [[:group_discounts :gd]]
        :join [[:group_org_memberships :gom]
               [:and
                [:= :gom.org_id org-id]
                [:= :gom.deleted nil]]]
        :where [:and
                [:= :gd.deleted nil]
                [:> :gd.created
                      (->> (tick/new-period days-back :days)
                           (tick/- (tick/now))
                           tick->ts
                           java.sql.Timestamp.)]]}
       db/hs-query
       first
       :count))

(defn get-weekly-auto-email-data--communities-num-new-orgs [org-id days-back]
  (->> {:select [:%count.gom2.org_id]
        :modifiers [:distinct]
        :from [[:group_org_memberships :gom1]]
        :join [[:group_org_memberships :gom2]
               [:and
                [:= :gom1.group_id :gom2.group_id]
                [:= :gom2.deleted nil]]]
        :where [:and [:= :gom1.org_id org-id]
                [:> :gom2.created
                 (->> (tick/new-period days-back :days)
                      (tick/- (tick/now))
                      tick->ts
                      java.sql.Timestamp.)]]}
       db/hs-query
       first
       :count))

(defn get-weekly-auto-email-data--communities-num-new-stacks [org-id days-back]
  (-> {:select [[:%count-distinct.gom2.org_id :count-ids]]
       :from [[:group_org_memberships :gom1]]
       :join [[:group_org_memberships :gom2]
              [:and
               [:= :gom1.group_id :gom2.group_id]
               [:= :gom2.deleted nil]]
              [:stack_items :si]
              [:and
               [:= :si.buyer_id :gom2.org_id]
               [:= :si.deleted nil]]]
       :where [:and
               [:= :gom1.deleted nil]
               [:= :gom1.org_id org-id]]
       :having [:> :%max.si.created
                (->> (tick/new-period days-back :days)
                     (tick/- (tick/now))
                     tick->ts
                     java.sql.Timestamp.)]}
      db/hs-query
      first
      :count-ids))

(defn get-weekly-auto-email-data [org-id oname]
  (let [product-renewals-soon (get-weekly-auto-email-data--product-renewals-soon org-id)]
    {:product-renewals-soon product-renewals-soon
     :num-renewals-soon (count product-renewals-soon)
     :active-rounds (get-weekly-auto-email-data--active-rounds org-id)
     :communities-num-new-discounts (get-weekly-auto-email-data--communities-num-new-discounts org-id)
     :communities-num-new-orgs (get-weekly-auto-email-data--communities-num-new-orgs org-id)
     :communities-num-new-stacks (get-weekly-auto-email-data--communities-num-new-stacks org-id)}))

(defn do-scheduled-emailer [dt]
  (try
    (let [threshold-ts (-> dt
                           (tick/- (tick/new-period 6 :days))
                           tick->ts)]
      (while
          (when-let [{:keys [email uname oname org-id max-created]} (select-next-email&recipient threshold-ts)]
            (send-template-email email
                                 (get-weekly-auto-email-data org-id oname))
            true)))
    (catch Throwable e
      (def ex1 e)
      (com/log-error e)
      (Thread/sleep (* 1000 60 60)))))


(defn start-scheduled-emailer-thread []
  (when (and (not env/building?)
             (nil? @scheduled-email-thread&))
    (reset! scheduled-email-thread&
            (future
              (log/info "Starting scheduled-emailer")
              (while (not @com/shutdown-signal)
                (let [event-time @next-scheduled-event&]
                  (if (tick/> (tick/now) next-scheduled-event)
                    (do (reset! next-scheduled-event& (calc-next-due-ts (tick/now) ))
                        (#'do-scheduled-emailer event-time))
                    (Thread/sleep (* 1000 10)))))
              (log/info "Stopped scheduled-emailer")))))


#_

(start-scheduled-email-puller-thread) ;; TODO calling this here is gross -- Bill

