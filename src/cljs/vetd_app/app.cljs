(ns vetd-app.app
  (:require [vetd-app.core :as core]
            [cljs.spec.alpha :as s]
            [expound.alpha :as expound]
            [devtools.core :as devtools]))

;;ignore println statements in prod
#_(set! *print-fn* (fn [& _]))

(set! s/*explain-out* expound/printer)

(enable-console-print!)

(devtools/install!)

(core/init!)
