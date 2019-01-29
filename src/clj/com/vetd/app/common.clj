(ns com.vetd.app.common
  (:require [expound.alpha :as expound]
            [clojure.spec.alpha :as spec]))

(alter-var-root #'spec/*explain-out* (constantly expound/printer))

#_ (def handle-ws-inbound nil)
(defmulti handle-ws-inbound
  (fn [{:keys [cmd]} ws-id subscription-fn] cmd))


(defmethod handle-ws-inbound nil [_ _ _]
  :NOT-IMPLEMENTED)
