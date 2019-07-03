(ns kit.core
  (:require-macros [kit.core :as core]))

(defn module-system?
  "Checks if a CommonJS style module system is present"
  []
  (and (core/exists? js/module) (.-exports js/module)))
