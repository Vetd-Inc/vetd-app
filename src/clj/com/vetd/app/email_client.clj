(ns com.vetd.app.email-client
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.links :as l]            
            [taoensso.timbre :as log]
            [clj-http.client :as client]
            [clj-time.periodic :as t-per]
            [tick.core :as tick]
            [tick.alpha.api :as tcka]))


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

(defn- insert-unsubscribe
  [{:keys [user-id org-id etype]}]
  (let [[id idstr] (ut/mk-id&str)]
    (db/insert! :unsubscribes
                {:id id
                 :idstr idstr
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :user_id user-id
                 :org_id org-id
                 :etype etype})
    id))

(defn unsubscribe
  "Unsubscribe a user from an email type."
  [{:keys [user-id org-id etype] :as args}]
  (insert-unsubscribe args))

(defn create-unsubscribe-link
  "Create a new unsubscribe link. Return the link url."
  [{:keys [user-id org-id etype] :as input}]
  (str l/base-url
       (l/create {:cmd :email-unsubscribe
                  :input-data (select-keys input [:user-id :org-id :etype])
                  ;; a year from now
                  :expires-action (+ (ut/now) (* 1000 60 60 24 365))})))

(defmethod l/action :email-unsubscribe
  [{:keys [input-data] :as link} _]
  (do (l/update-expires link "read" (+ (ut/now) (* 1000 60 5))) ; allow read for next 5 mins)
      {:unsubscribed? (boolean (unsubscribe input-data))}))

;;;; Auto Email

(defonce scheduled-email-thread& (atom nil))
(defonce next-scheduled-event& (atom nil))

(def override-now& (atom nil))

(defn now- []
  (or @override-now&
      (tick/now)))

(defn monday? [x] (= (tick/day-of-week x) #time/day-of-week "MONDAY"))

(defn nine-am-pst? [x] (= (tick/hour x) 16))

(defn calc-next-due-ts [dt]
  (->> (tick/range (-> (tick/truncate dt :hours)
                       (tick/+ (tick/new-duration 1 :hours)))
                   (tick/+ dt (tick/new-period 8 :days))
                   (tick/new-duration 1 :hours))
       (filter #(and (monday? %)
                     (nine-am-pst? %)))
       first))

(defn tick->ts [t]
  (tick/millis (tick/between (tick/epoch) t)))

(defn ts->tick [msecs]
  (tcka/+ (tick/epoch) (tick/new-duration msecs :millis)))

(defn insert-email-sent-log-entry
  [{:keys [etype org-id user-id data]}]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :email_sent_log
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :etype (name etype)
                     :org_id org-id
                     :user_id user-id
                     :data data})
        first)))

;; TODO This will get trickier when we have multiple app server instances running again
(defn select-next-email&recipient [max-ts]
  (-> {:select [[:%max.esl.created :max-created]
                [:m.id :membership-id]
                [:m.user_id :user-id]
                [:u.email :email]                
                [:m.org_id :org-id]
                [:o.oname :oname]]
       :from [[:orgs :o]]
       :join [[:memberships :m]
              [:and
               [:= :m.deleted nil]
               [:= :m.org_id :o.id]]
              [:users :u]
              [:and
               [:= :u.id :user_id]
               [:or
                [:= :m.org_id 2208512249632]
                [:not
                 [:like :u.email "%@vetd.com"]]]]]
       :left-join [[:email_sent_log :esl]
                   [:= :esl.user_id :m.user_id]]
       :where [:and
               [:= :o.deleted nil]
               [:= :o.buyer_qm true]]
       :group-by [:m.id :m.user_id :m.org_id :o.oname :u.email]
       :having [:or
                [:= :%max.esl.created nil]
                [:< :%max.esl.created nil (java.sql.Timestamp. max-ts)]]
       :limit 1}
      db/hs-query
      first))


(defn select-max-email-log-created-by-etype [etype]
  (-> {:select [[:%max.esl.created :max-created]]
       :from [[:email_sent_log :esl]]
       :where [:and
               [:= :esl.deleted nil]
               [:= :esl.etype etype]]
       :limit 1}
      db/hs-query
      first
      :max-created))

;; days-forward should be less than 30/31 if using price-period of monthly
(defn get-weekly-auto-email-data--product-renewals-soon [org-id price-period days-forward limit]
  (->> [[:stack-items {:_where
                       {:_and (concat [{:status {:_eq "current"}}
                                       {:buyer_id {:_eq (str org-id)}}
                                       ;; {:renewal-reminder {:_eq true}} ;; include regardless of reminder checked
                                       {:price-period {:_eq price-period}}
                                       {:deleted {:_is_null true}}]
                                      (if (= price-period "monthly")
                                        (let [start-day (tick/day-of-month (now-))
                                              end-day (->> (tick/new-period days-forward :days)
                                                           (tick/+ (now-))
                                                           tick/day-of-month)]
                                          (if (> start-day end-day) ;; it wrapped around to next month
                                            [{:_or
                                              [{:renewal-day-of-month {:_gte start-day}}
                                               {:renewal-day-of-month {:_lte end-day}}]}]
                                            [{:renewal-day-of-month {:_gte start-day}}
                                             {:renewal-day-of-month {:_lte end-day}}]))
                                        [{:renewal-date {:_gte (str (tick/date (ut/now-ts)))}}
                                         {:renewal-date {:_lte (->> (tick/new-period days-forward :days)
                                                                    (tick/+ (now-))
                                                                    tick/date
                                                                    str)}}]))}
                       :_order_by {:renewal-date :asc}
                       :_limit limit}
         [:price-amount :renewal-date :renewal-day-of-month
          [:product
           [:pname]]]]]
       ha/sync-query
       :stack-items
       (map (fn [{:keys [renewal-day-of-month] :as si}]
              (if (= price-period "monthly")
                (assoc si :renewal-date (str "on the "
                                             renewal-day-of-month
                                             (case renewal-day-of-month
                                               1 "st"
                                               2 "nd"
                                               3 "rd"
                                               21 "st"
                                               22 "nd"
                                               23 "rd"
                                               31 "st"
                                               "th")))
                (update-in si [:renewal-date] (comp str tick/date)))))))

;; active rounds are rounds that are "in-progress"
(defn get-weekly-auto-email-data--active-rounds [org-id]
  (->> [[:rounds {:deleted nil
                  :status "in-progress"
                  :buyer-id org-id
                  :_order_by {:created :desc}}
         [:id :idstr :title
          [:products
           [:id]]]]]
       ha/sync-query
       :rounds
       (map (fn [{:keys [products] :as round}]
              (assoc round :num-products (count products))))))

(defn get-weekly-auto-email-data--communities-num-new-discounts [group-id days-back]
  (->> {:select [[:%count.gd.id :count]]
        :from [[:group_discounts :gd]]
        :where [:and
                [:= :gd.deleted nil]
                [:= :gd.group_id group-id]
                [:> :gd.created
                 (->> (tick/new-period days-back :days)
                      (tick/- (now-))
                      tick->ts
                      java.sql.Timestamp.)]]}
       db/hs-query
       first
       :count))

(defn get-weekly-auto-email-data--communities-num-new-orgs [group-id days-back]
  (->> {:select [[:%count.gom.org_id :count]]
        :from [[:group_org_memberships :gom]]
        :where [:and
                [:= :gom.deleted nil]
                [:= :gom.group_id group-id]
                [:> :gom.created
                 (->> (tick/new-period days-back :days)
                      (tick/- (now-))
                      tick->ts
                      java.sql.Timestamp.)]]}
       db/hs-query
       first
       :count))

(defn get-weekly-auto-email-data--communities-num-new-stacks [group-id days-back]
  ;; celwell: I can't say I fully understand this, but it seems to work
  (-> {:select [[:%count.si.buyer_id :si-buyer-count]]
       :from [[:group_org_memberships :gom]]
       :join [[:stack_items :si]
              [:and
               [:= :si.buyer_id :gom.org_id]
               [:= :si.deleted nil]]]
       :where [:and
               [:= :gom.deleted nil]
               [:= :gom.group_id group-id]]
       :group-by [:si.buyer_id]
       :having [:> :%min.si.created
                (->> (tick/new-period days-back :days)
                     (tick/- (now-))
                     tick->ts
                     java.sql.Timestamp.)]}
      db/hs-query
      count))

(defn get-weekly-auto-email-data--communities
  [group-id]
  (let [{:keys [gname]} (->> [[:groups {:id group-id}
                               [:gname]]]
                             ha/sync-query
                             vals
                             ffirst)]
    {:group-name gname
     :num-new-discounts (get-weekly-auto-email-data--communities-num-new-discounts group-id 7)
     :num-new-orgs (get-weekly-auto-email-data--communities-num-new-orgs group-id 7)
     :num-new-stacks (get-weekly-auto-email-data--communities-num-new-stacks group-id 7)}))

(defn get-weekly-auto-email-data [user-id org-id oname]
  (let [group-ids (some->> [[:group-org-memberships {:org-id org-id}
                             [:group-id]]]
                           ha/sync-query
                           vals
                           first
                           (map :group-id))
        product-annual-renewals-soon (get-weekly-auto-email-data--product-renewals-soon org-id "annual" 30 15)
        product-monthly-renewals-soon (get-weekly-auto-email-data--product-renewals-soon org-id "monthly" 7 15)
        active-rounds (get-weekly-auto-email-data--active-rounds org-id)]
    {:base-url "https://app.vetd.com/"
     :unsubscribe-link (create-unsubscribe-link {:user-id user-id
                                                 :org-id org-id
                                                 :etype (name :weekly-buyer-email)})
     :org-name oname
     :product-annual-renewals-soon product-annual-renewals-soon
     :product-annual-renewals-soon-count (count product-annual-renewals-soon)
     :product-monthly-renewals-soon product-monthly-renewals-soon
     :product-monthly-renewals-soon-count (count product-monthly-renewals-soon)
     :active-rounds active-rounds
     :active-rounds-count (count active-rounds)
     :communities (map get-weekly-auto-email-data--communities group-ids)}))


(defn do-scheduled-emailer [dt]
  (try
    (log/info (str "CALL do-scheduled-emailer " dt))
    (let [threshold-ts (-> dt
                           (tick/- (tick/new-period 6 :days))
                           tick->ts)]
      (while (try
               (when-let [{:keys [email oname user-id org-id max-created]} (select-next-email&recipient threshold-ts)]
                 (let [data (get-weekly-auto-email-data user-id org-id oname)]
                   (send-template-email
;;                    "zach@vetd.com" 
                    email
                    data
                    {:template-id "d-76e51dc96f2d4d7e8438bd6b407504f9"})
                   (insert-email-sent-log-entry
                    {:etype :weekly-buyer-email
                     :user-id user-id
                     :org-id org-id
                     :data data}))
                 (Thread/sleep 1000)
                 true)
               (catch Throwable e
                 (com/log-error e)
                 false))))
    (catch Throwable e
      (com/log-error e)
      (Thread/sleep (* 1000 60 60)))))

(defn start-scheduled-emailer-thread []
  (when (and (not env/building?)
             (nil? @scheduled-email-thread&))
    (reset! scheduled-email-thread&
            (future
              (log/info "Starting scheduled-emailer")
              (reset! next-scheduled-event& (or (some-> "weekly-buyer-email"
                                                        select-max-email-log-created-by-etype
                                                        tick/date-time
                                                        calc-next-due-ts)
                                                (calc-next-due-ts (now-))))
              (while (not @com/shutdown-signal)
                (let [event-time @next-scheduled-event&]
                  (if (tick/> (now-) event-time)
                    (do (reset! next-scheduled-event& (calc-next-due-ts (now-)))
                        (#'do-scheduled-emailer event-time))
                    (Thread/sleep (* 1000 10)))))
              (log/info "Stopped scheduled-emailer")))))

(start-scheduled-emailer-thread) ;; TODO calling this here is gross -- Bill
