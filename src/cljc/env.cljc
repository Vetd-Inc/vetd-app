(ns vetd-app.env
  (:require [environ.core :as env]))

(defmacro segment-frontend-write-key []
  (env/env :segment-frontend-write-key))
