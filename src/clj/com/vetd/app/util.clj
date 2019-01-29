(ns com.vetd.app.util
  (:require [clojure.walk :as walk]))

(defn now [] (System/currentTimeMillis))

(defn now-ts []
  (java.sql.Timestamp.
   (System/currentTimeMillis)))

(defn kw->str
  [kw]
  (str (when-let [n (and (keyword? kw)
                         (namespace kw))]
         (str n "/"))
       (name kw)))

(defn ->int
  [v]
  (try (cond (integer? v) v
             (string? v) (Integer/parseInt v)
             (float? v) (int v))
       (catch Exception e
         nil)))

(defn ->vec
  [v]
  (cond (vector? v) v
        (sequential? v) (vec v)
        (map? v) [v]
        (coll? v) (vec v)
        :else [v]))

(defn ->number
  [v]
  (try (cond (number? v) v
             (string? v) (Double/parseDouble v))
       (catch Exception e
         nil)))

(defn replace$
  [form]
  (let [$sym `$#
        form' (walk/prewalk-replace {'$ $sym}
                                    form)]
    (if (= form form')
      form
      `((fn [~$sym] ~form')))))

(defmacro $-
  [m & body]
  `(~m ~@(map replace$ body)))

(defmacro for->map
  [bindings & body]
  `(into {}
         (for ~bindings
           ~@body)))


(defn ->long [x]
  (if (integer? x) ;; optimize fast case
    x
    (try
      (condp #(% %2) x
        string? (Long/parseLong x)
        number? (long x)
        keyword? (-> x name ->long)
        nil)
      (catch Exception e nil))))

(defn ->double [x]
  (try
    (condp #(% %2) x
      integer? (double x)
      string? (Double/parseDouble x)
      float? x
      keyword? (-> x name ->double)
      nil)
    (catch Exception e nil)))

(defn keep-kv [f m]
  (->> (for [[k v] m]
         (f k v))
       (remove nil?)
       (into {})))

(defmulti fmap
  "Applies function f to each item in the data structure s and returns
   a structure of the same kind."
   {:arglists '([f s])}
   (fn [f s] (type s)))

(defmethod fmap clojure.lang.IPersistentList
  [f v]
  (map f v))

(defmethod fmap clojure.lang.IPersistentVector
  [f v]
  (into (empty v) (map f v)))

(defmethod fmap clojure.lang.IPersistentMap
  [f m]
  (into (empty m) (for [[k v] m] [k (f v)])))

(defmethod fmap clojure.lang.IPersistentSet
  [f s]
  (into (empty s) (map f s)))

(defmethod fmap clojure.lang.IFn
  [f fn]
  (comp f fn))

(prefer-method fmap clojure.lang.IPersistentMap clojure.lang.IFn)
(prefer-method fmap clojure.lang.IPersistentVector clojure.lang.IFn)

(defn md5 [s]
  (->> s
       .getBytes
       (.digest (java.security.MessageDigest/getInstance "MD5"))
       (BigInteger. 1)
       (format "%032x")))

(defn uuid [] (java.util.UUID/randomUUID))

(defn uuid-str [] (.toString (uuid)))

(defn traverse-values
  [f v]
  (if (coll? v)
    (fmap (partial traverse-values f) v)
    (f v)))
