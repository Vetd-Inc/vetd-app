(ns app.vetd-app.core-test
  (:require [cljs.test :refer-macros [deftest is]]
            ;; [cljs.test :as t :include-macros true]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            vetd-app.core
            ))

(deftest simple
  (is (= true true))
  (is (= true false)))
