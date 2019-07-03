(ns latte.chai
  (:require [clojure.set     :as set]
            [clojure.string  :as str]
            [kit.core        :as kit]
            [latte.add       :as add]
            [latte.overwrite :as overwrite]))

(def chai
  (if (kit/module-system?)
    (js/require "chai")
    js/chai))

(def expect* (aget chai "expect"))

(defn- assertion? [x]
  (= x (.-Assertion chai)))

(defn- function? [x]
  (js* "typeof ~{x} === \"function\""))

(defn- find-meth [obj ks]
  (loop [obj  (expect* obj)
         ks   ks]
    (if (not-empty ks)
      (if-let [meth (aget obj (-> ks first name))]
        (if (and (function? meth)
                 (empty? (rest ks)))
          [meth obj]
          (recur meth (rest ks)))
        (-> (str "No property: " (-> ks first name) " found")
            (js/Error.)
            (throw)))
      obj)))

(defn expect [obj expr & args]
  (if-let [result (find-meth obj (str/split (name expr) #"\."))]
    (if (vector? result)
      (let [[meth this] result]
        (.apply meth this (into-array args)))
      (when (not-empty args)
        (-> (str "Arguments: "
                 (clj->js args)
                 " supplied to property expression "
                 expr
                 " where none where expected")
            (js/Error.)
            (throw))))
    (throw (js/Error. "Could not find test method"))))

(defn plugin [s]
  (when-let [module (js/require s)]
    (.use chai module)
    module))

;; this additional method is needed as the include and contain
;; methods can not be overwritten in chai.

(add/method :value
  :message   "expected #{act} to contain the value #{exp}"
  :negation  "expected #{act} not to contain the value #{exp}"
  :assertion (comp boolean #(some #{%2} (seq %1))))

;; overwrite properties and methods that need special handling
;; for clojure data types

(overwrite/method :equal
  :message    "expected #{exp} to be equal to #{act}"
  :negation   "expected #{exp} not to be equal to #{act}"
  :guard      (some-fn list? map? vector? seq? set?)
  :assertion  =)

(overwrite/property :empty
  :message    "expected #{act} to be empty"
  :negation   "expected #{act} not to be empty"
  :guard      (some-fn list? map? vector? seq? set?)
  :assertion  empty?)


(overwrite/method :keys
  :message    "expected #{act} to contain keys #{exp}"
  :negation   "expected #{act} not to contain keys #{exp}"
  :guard      map?
  :assertion  (fn [m & ks]
                (->> (flatten ks)
                     (every? #(contains? m %)))))

(overwrite/method :members
  :message    "expected #{act} to be a superset of #{exp}"
  :negation   "expected #{act} not to be a superset of #{act}"
  :guard      (some-fn list? vector? seq? set?)
  :assertion  (fn [x y]
                (empty? (set/difference (set y) (set x)))))
