(ns com.vetd.app.common
  (:require [expound.alpha :as expound]
            [clojure.spec.alpha :as spec]
            [taoensso.timbre :as log]))


#_ (def handle-ws-inbound nil)
(defmulti handle-ws-inbound
  (fn [{:keys [cmd]} ws-id subscription-fn] cmd))


(defmethod handle-ws-inbound nil [_ _ _]
  :NOT-IMPLEMENTED)

(defn setup-env [prod?]
  (if prod?
    (do (log/set-config! {:level :info
                          :output-fn (partial log/default-output-fn {:stacktrace-fonts {}})}))
    (do (alter-var-root #'spec/*explain-out* (constantly expound/printer)))))

