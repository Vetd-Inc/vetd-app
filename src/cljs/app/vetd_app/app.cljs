(ns vetd-app.app
  (:require [vetd-app.core :as core]
            [cljs.spec.alpha :as s]
            [expound.alpha :as expound]))

(def app-loaded? (volatile! false))

;;ignore println statements in prod
#_(set! *print-fn* (fn [& _]))

(defn load-app []
  (when-not @app-loaded?
    (vreset! app-loaded? true)
    (set! s/*explain-out* expound/printer)
    (enable-console-print!)
    (core/init!)))


(load-app)
