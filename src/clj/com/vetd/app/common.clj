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
            clojure.pprint))

(defonce shutdown-ch (a/chan))

(defonce supress-sns? (atom false))

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

(def sns-client (aws/client {:api :sns
                             :region "us-east-1"
                             :credentials-provider aws-creds-provider}))

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
          (clojure.pprint/pprint arg)))))
