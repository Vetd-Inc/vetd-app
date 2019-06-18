(ns com.vetd.app.common
  "This is a grab bag until we come up with a reason that it shouldn't be.
  Before adding a function here, consider whether it would be more
  appropriate in util. "
  (:require [com.vetd.app.util :as ut]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as spec]
            [clojure.core.async :as a]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as aws-creds]
            [taoensso.timbre :as log]
            [buddy.hashers :as bhsh]
            [clj-honeycomb.core :as hnyc]
            clojure.pprint))

;; TODO this should be a channel (so timeout is available), but I don't feel like it right now -- Bill
(defonce shutdown-signal (atom false))

(defonce supress-sns? (atom false))

(def ws-on-close-fns& (atom {}))

(defn md5-hex [s]
  (-> s
      buddy.core.hash/md5
      buddy.core.codecs/bytes->hex))

#_ (def handle-ws-inbound nil)
(defmulti handle-ws-inbound
  (fn [{:keys [cmd]} ws-id subscription-fn] cmd))


(defmethod handle-ws-inbound nil [_ _ _]
  :NOT-IMPLEMENTED)

(defn setup-env [prod?]
  (if prod?
    (do (log/merge-config! {:level :info
                            :output-fn (partial log/default-output-fn {:stacktrace-fonts {}})}))
    (do (alter-var-root #'spec/*explain-out* (constantly expound/printer))
        (reset! supress-sns? true))))


;;;; AWS API
(def aws-creds-provider
  (aws-creds/basic-credentials-provider
   ;; TODO cycle creds and keep in env
   {:access-key-id "AKIAIJN3D74NBHJIAARQ"
    :secret-access-key "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"}))

(def aws-creds-provider-s3-writer
  (aws-creds/basic-credentials-provider
   ;; TODO cycle creds and keep in env
   {:access-key-id "AKIAJPZBKHS4C6DB2YOQ"
    :secret-access-key "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"}))


(def sns-client (aws/client {:api :sns
                             :region "us-east-1"
                             :credentials-provider aws-creds-provider}))

(def s3-client (aws/client {:api :s3
                             :region "us-east-1"
                             :credentials-provider aws-creds-provider-s3-writer}))

(def topic->arn
  {:ui-misc "arn:aws:sns:us-east-1:744151627940:ui-misc"
   :ui-req-new-prod-cat "arn:aws:sns:us-east-1:744151627940:ui-req-new-prod-cat"
   :ui-start-round "arn:aws:sns:us-east-1:744151627940:ui-start-round"})

(defn sns-publish
  "Publishes new notification to AWS SNS.
  topic - keyword abbrev. for topic's ARN
  subject - notification Subject
  message - notification message body"
  [topic subject message & [override-suppress?]]
  (let [arg {:op :Publish
                 :request {:TopicArn (topic->arn topic)
                           :Subject subject
                           :Message message}}]
    (if (or (not @supress-sns?) override-suppress?)
      (aws/invoke sns-client arg)
      ;; TODO make separate sns for dev
      (do (println "SNS PUBLISH SUPPRESSED")
          (clojure.pprint/pprint arg)
          {} ; to ensure ws return gets triggered
          ))))


(defn s3-put [bucket-name file-name data]
  (aws/invoke s3-client
              {:op :PutObject :request {:Bucket bucket-name :Key file-name
                                        :Body data}}))

(defn walk-dissoc [frm ks]
  (clojure.walk/prewalk
   (fn [v]
     (if (map? v)
       (apply dissoc v ks)
       v))
   frm))

(defn hc-send [v]
  ;; TODO don't use supress-sns
  (when-not @supress-sns?
    (try
      (when-not (hnyc/initialized?)
        (hnyc/init {:data-set "app-prod"
                    :write-key "fadb42b152f679a1575055e9678ac49a"}))
      (-> v
          (walk-dissoc [:pwd :session-token])
          hnyc/send)
      (catch Throwable e
        (log/error e)))))


(defn log-error [e & [arg]]
  (hc-send {:type "error"
            :ex e
            :arg arg})
  (if arg
    (log/error e arg)    
    (log/error e)))



(defn reg-ws-on-close-fn'
  [ws-on-close-fns ws-id k f]
  (assoc-in ws-on-close-fns
            [ws-id k] f))

(defn reg-ws-on-close-fn [ws-id k f]
  (swap! ws-on-close-fns& reg-ws-on-close-fn' ws-id k f))

(defn unreg-ws-on-close-fn'
  [ws-on-close-fns ws-id k]
  (or (try
        (update ws-on-close-fns
                ws-id
                dissoc k)
        (catch Exception e
          (log-error e)))
      ws-on-close-fns))

(defn unreg-ws-on-close-fn [ws-id k]
  (swap! ws-on-close-fns& unreg-ws-on-close-fn' ws-id k))

(defn force-all-ws-on-close-fns []
  (try
    (let [ws-on-close-fns @ws-on-close-fns&]
      (log/info (format "Forcing close on %d ws-on-close-fns"
                        (count @ws-on-close-fns&)))
      (->> ws-on-close-fns
           vals
           (pmap #(%))
           doall)
      (log/info "DONE Forcing close on ws-on-close-fns"))
    (catch Throwable e
      (log-error e))))
