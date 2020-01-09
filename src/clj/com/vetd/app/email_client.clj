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
            [tick.alpha.api :as tcka]
            [clojure.string :as s]))

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

(defn nine-am-pdt? [x] (= (tick/hour x) 16))

(defn calc-next-due-ts [dt]
  (->> (tick/range (-> (tick/truncate dt :hours)
                       (tick/+ (tick/new-duration 1 :hours)))
                   (tick/+ dt (tick/new-period 8 :days))
                   (tick/new-duration 1 :hours))
       (filter #(and (monday? %)
                     (nine-am-pdt? %)))
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
                [:u.uname :uname]
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
               [:= :u.deleted nil]
               ;; exclude users that were created less than 3 days ago
               [:< :u.created (->> (tick/new-period 3 :days)
                                   (tick/- (now-))
                                   tick->ts
                                   java.sql.Timestamp.)]
               [:like :u.email "%@%"] ;; primitive email address validation
               [:or ;; exclude test accounts, but let through real "Vetd" org
                [:= :m.org_id 2208512249632]
                [:not                       
                 [:or
                  [:like :u.email "%@vetd.com"]
                  [:like :u.email "temp@%"]]]]]]
       :left-join [[:email_sent_log :esl]
                   [:= :esl.user_id :m.user_id]
                   [:unsubscribes :uns]
                   [:and
                    [:= :uns.user_id :u.id]
                    [:= :uns.org_id :o.id]
                    [:= :uns.etype "weekly-buyer-email"]
                    [:= :uns.deleted nil]]]
       :where [:and
               [:= :o.deleted nil]
               [:= :o.buyer_qm true]
               [:= :uns.id nil]]
       :group-by [:m.id :m.user_id :m.org_id :o.oname :u.email :u.uname]
       :having [:or
                [:= :%max.esl.created nil]
                [:< :%max.esl.created (java.sql.Timestamp. max-ts)]]
       :limit 1}
      db/hs-query
      first))

(defn last-email-sent [etype]
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
              (let [si-with-date (if (= price-period "monthly")
                                   (assoc si :renewal-date (str "on the "
                                                                (ut/append-ordinal-suffix renewal-day-of-month)))
                                   (update-in si [:renewal-date] (comp str tick/date)))]
                (update-in si-with-date [:price-amount] #(some-> % ut/->dollars-str)))))))

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

(defn get-weekly-auto-email-data--recent-preposals [org-id]
  (->> [[:form-docs {:ftype "preposal"
                     :from-org-id org-id
                     :_where {:_or [;; when Vetd requested the estimate on behalf of user (forwarded email)
                                    {:_and [{:created {:_gt (->> (tick/new-period 7 :days)
                                                                 (tick/- (now-))
                                                                 tick->ts
                                                                 java.sql.Timestamp.
                                                                 str)}}
                                            {:title {:_like "ADMIN %"}}]}
                                    ;; when the vendor created the estimate (preposal)
                                    {:doc-created {:_gt (->> (tick/new-period 7 :days)
                                                             (tick/- (now-))
                                                             tick->ts
                                                             java.sql.Timestamp.
                                                             str)}}]}
                     :_order_by {:created :desc}}
         [:id :created :updated :doc-id :doc-created
          [:product [:pname]]
          [:from-org [:oname]]
          [:from-user [:id :uname]]
          [:to-org [:id :oname]]]]]
       ha/sync-query
       :form-docs
       (#(assoc {}
                :preposals (filter :doc-id %)
                :preposals-count (count (filter :doc-id %))
                ;; Since we create preposals on behalf of the user
                ;; number of preposal requests with title beginning with "ADMIN "
                ;; is a proxy for the number of emails they forwarded to us.
                :num-emails-forwarded (count
                                       (filter (fn [form-doc]
                                                 (> (->> form-doc
                                                         :created
                                                         .getTime)
                                                    (->> (tick/new-period 7 :days)
                                                         (tick/- (now-))
                                                         tick->ts
                                                         java.sql.Timestamp.
                                                         .getTime)))
                                               %))))))


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

(defn get-weekly-auto-email-data--communities-recent-threads
  "For group-id, in the past days-back, the new threads that were 
  created OR existing threads that gained a reply."
  [group-id days-back]
  (let [days-back-tick (->> (tick/new-period days-back :days)
                            (tick/- (now-)))
        created-after-days-back? (comp (partial tick/< days-back-tick)
                                       tick/instant
                                       :created)]
    (->> [[:threads {:group-id (str group-id)
                     :deleted nil
                     :_limit 100 ;; hopefully enough to get all the old ones w/ recent replies
                     :_order_by {:created :desc}}
           [:title :created
            [:messages {:deleted nil}
             [:created
              [:user
               [:uname]]
              [:org
               [:oname]]]]]]]
         ha/sync-query
         :threads
         (map #(identity
                {:title (-> % :title)
                 :user-name (some-> % :messages first :user :uname)
                 :org-name (some-> % :messages first :org :oname)
                 :num-recent-replies (->> %
                                          :messages
                                          (drop 1) ;; the root message of the thread
                                          (filter created-after-days-back?)
                                          count)
                 :created (-> % :created)}))
         (filter (some-fn (comp pos? :num-recent-replies) ;; has recent replies?
                          ;; or thread itself was created recently?
                          created-after-days-back?))
         (map #(assoc %
                      :num-recent-replies-string
                      (let [num-recent-replies (:num-recent-replies %)]
                        (cond
                          (= 1 num-recent-replies)
                          (str "(1 new reply)")
                          
                          (pos? num-recent-replies)
                          (str "(" num-recent-replies " new replies)")
                          
                          :else ""))))
         (take 10))))

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
     :num-new-stacks (get-weekly-auto-email-data--communities-num-new-stacks group-id 7)
     :recent-threads (get-weekly-auto-email-data--communities-recent-threads group-id 7)}))

(defn get-weekly-auto-email-data [user-id org-id oname uname]
  (let [group-ids (some->> [[:group-org-memberships {:org-id org-id}
                             [:group-id]]]
                           ha/sync-query
                           vals
                           first
                           (map :group-id))
        recent-preposals (get-weekly-auto-email-data--recent-preposals org-id)
        product-annual-renewals-soon (get-weekly-auto-email-data--product-renewals-soon org-id "annual" 30 15)
        product-monthly-renewals-soon (get-weekly-auto-email-data--product-renewals-soon org-id "monthly" 7 15)
        active-rounds (get-weekly-auto-email-data--active-rounds org-id)]
    {:base-url "https://app.vetd.com/"
     :unsubscribe-link (create-unsubscribe-link {:user-id user-id
                                                 :org-id org-id
                                                 :etype (name :weekly-buyer-email)})
     ;; get just the First Name
     :user-name (str ;; nil -> ""
                 (some->> (s/split (s/lower-case uname) #" ")
                          (remove #{"mr." "mrs." "ms." "dr."
                                    "mr" "mrs" "ms" "dr"})
                          first
                          s/capitalize))
     :org-name oname
     :recent-preposals recent-preposals
     :product-annual-renewals-soon product-annual-renewals-soon
     :product-annual-renewals-soon-count (count product-annual-renewals-soon)
     :product-monthly-renewals-soon product-monthly-renewals-soon
     :product-monthly-renewals-soon-count (count product-monthly-renewals-soon)
     :active-rounds active-rounds
     :active-rounds-count (count active-rounds)
     :communities (map get-weekly-auto-email-data--communities group-ids)}))

(defn do-scheduled-emailer [threshold-dt]
  (try
    (log/info (str "CALL do-scheduled-emailer " threshold-dt))
    (let [threshold-ts (tick->ts threshold-dt)]
      (while (try
               (when-let [{:keys [email oname uname user-id org-id max-created]} (select-next-email&recipient threshold-ts)]
                 (let [data (get-weekly-auto-email-data user-id org-id oname uname)]
                   (send-template-email email
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

;; this is not tested with multiple concurrent instances, but should be fine
(defn start-scheduled-emailer-thread []
  (when (and (not env/building?)              ;; not while building
             env/prod?                        ;; only in prod
             (nil? @scheduled-email-thread&)) ;; we only need one thread
    (reset! scheduled-email-thread&
            (future
              (log/info "Starting scheduled-emailer")
              (reset! next-scheduled-event&
                      ;; find the next send time based on email sent log
                      (tick/instant
                       (calc-next-due-ts
                        (or (some-> (last-email-sent "weekly-buyer-email") tick/date-time)
                            ;; if never sent, the -1 day allows it to possibly send today (if Monday)
                            (tick/- (now-) (tick/new-period 1 :days))))))
              (while (not @com/shutdown-signal)
                (let [event-time @next-scheduled-event&]
                  (if (tick/> (now-) event-time)
                    (do (reset! next-scheduled-event& (calc-next-due-ts (now-)))
                        (#'do-scheduled-emailer (tick/- event-time (tick/new-period 6 :days))))
                    (Thread/sleep (* 1000 10)))))
              (log/info "Stopped scheduled-emailer")))))

(start-scheduled-emailer-thread) ;; TODO calling this here is gross -- Bill

#_ (future-cancel @scheduled-email-thread&)

#_ (reset! override-now& (tick/+ (tick/now) (tick/new-period 6 :days)))
