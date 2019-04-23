(ns com.vetd.app.common
  (:require [expound.alpha :as expound]
            [clojure.spec.alpha :as spec]
            [clojure.core.async :as a]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as aws-creds]
            [taoensso.timbre :as log]))

(defonce shutdown-ch (a/chan))

#_ (def handle-ws-inbound nil)
(defmulti handle-ws-inbound
  (fn [{:keys [cmd]} ws-id subscription-fn] cmd))


(defmethod handle-ws-inbound nil [_ _ _]
  :NOT-IMPLEMENTED)

(defn setup-env [prod?]
  (if prod?
    (do (log/merge-config! {:level :info
                            :output-fn (partial log/default-output-fn {:stacktrace-fonts {}})}))
    (do (alter-var-root #'spec/*explain-out* (constantly expound/printer)))))


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
  [topic subject message]
  (aws/invoke sns-client
              {:op :Publish
               :request {:TopicArn (topic->arn topic)
                         :Subject subject
                         :Message message}}))
