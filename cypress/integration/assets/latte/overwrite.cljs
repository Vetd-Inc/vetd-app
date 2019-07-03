(ns latte.overwrite
  (:require [kit.core :as kit]))

(def chai
  (if (kit/module-system?)
    (js/require "chai")
    js/chai))

(def assertions (aget chai "Assertion"))

(defn method [n & args]
  (let [args      (apply hash-map args)
        guard     (:guard args (constantly false))
        assertion (:assertion args (constantly false))
        expected  (:expected args (comp clj->js first))]
    (.overwriteMethod assertions (name n)
      (fn [super]
        (fn [& expectations]
          (this-as this
            (let [obj (aget this "_obj")]
              (if (guard obj)
                (.assert this
                  (apply assertion obj expectations)
                  (:message args)
                  (:negation args)
                  (expected expectations)
                  (clj->js obj))
                (.apply super this (into-array expectations))))))))))

(defn property [n & args]
  (let [args      (apply hash-map args)
        guard     (:guard args (constantly false))
        assertion (:assertion args)]
    (.overwriteProperty assertions (name n)
      (fn [super]
        (fn []
          (this-as this
            (let [obj (aget this "_obj")]
              (if (guard obj)
                (.assert this
                  (assertion obj)
                  (:message args)
                  (:negation args)
                  (clj->js obj))
                (.apply super this)))))))))

