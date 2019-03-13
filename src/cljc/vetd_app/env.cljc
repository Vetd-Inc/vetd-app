(ns vetd-app.env
  (:require [environ.core :as environ]))

(defmacro segment-frontend-write-key []
  (environ/env :segment-frontend-write-key))
