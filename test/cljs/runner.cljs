(ns runner
  (:require [jx.reporter.karma :as karma :include-macros true]
            [app.vetd-app.core-test]))

(enable-console-print!)

(defn ^:export run-karma [karma]
  (karma/run-tests
   karma
   'app.vetd-app.core-test))
