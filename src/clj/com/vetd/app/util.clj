(ns com.vetd.app.util
  (:require [clojure.walk :as walk]
            clojure.set))

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

;; --------------------


;; max is 100k (not 10k) to avoid conflicts during bursts (which shouldn't really happen)
(def last-id (atom (rand-int 10000)))

(def ts2019-01-01 1546300800000)

(defn ms-since-vetd-epoch []
  (- (System/currentTimeMillis) ts2019-01-01))

(defn long-floor-div
  [a b]
  (-> a
      (/ b)
      long))

(def base36
  (into {}
        (map-indexed vector
                     (concat
                      (range 97 123)
                      (range 48 58)))))

(def base36-inv (clojure.set/map-invert base36))

(def base31
  (into {}
        (map-indexed vector
                     (concat
                      (remove #{101 105 111 117} ;; vowels
                              (range 98 123))
                      (range 48 58)))))

(def base31-inv (clojure.set/map-invert base31))

(defn base36->str
  [v]
  (let [x (loop [v' v
                 r []]
            (if (zero? v')
              r
              (let [idx (mod v' 36)
                    v'' (long-floor-div v' 36)]
                (recur v''
                       (conj r (mod v' 36))))))]
    (->> x
         reverse
         (map base36)
         (map char)
         clojure.string/join)))

(defn base36->num
  [s]
  (loop [[head & tail] (reverse s)
         idx 0
         r 0]
    (if (nil? head)
      (long r)
      (let [d (* (base36-inv (long head)) (Math/pow 36 idx))]
        (recur tail
               (inc idx)
               (+ r d))))))

(defn base31->str
  [v]
  (let [x (loop [v' v
                 r []]
            (if (zero? v')
              r
              (let [idx (mod v' 31)
                    v'' (long-floor-div v' 31)]
                (recur v''
                       (conj r (mod v' 31))))))]
    (->> x
         reverse
         (map base31)
         (map char)
         clojure.string/join)))

(defn base31->num
  [s]
  (loop [[head & tail] (reverse s)
         idx 0
         r 0]
    (if (nil? head)
      (long r)
      (let [d (* (base31-inv (long head)) (Math/pow 31 idx))]
        (recur tail
               (inc idx)
               (+ r d))))))

(defn mk-id []
  ;; max is 100k (not 10k) to avoid conflicts during bursts (which shouldn't really happen)
  (let [sub-id (swap! last-id #(-> % inc (mod 100000)))] 
    (-> (ms-since-vetd-epoch)
        (long-floor-div 100)
        (* 10000)
        (+ sub-id))))

(defn mk-id&str []
  (let [id (mk-id)]
    [id (base31->str id)]))

;; --------------------
