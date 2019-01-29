(ns com.vetd.app.path
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]))

(defn compile-paths
  [ps]
  (->> (for [[[k1 k2] {:keys [path]}] ps]
         (let [head-kw (-> path first namespace keyword)
               tail-kw (-> path last namespace keyword)
               forward {:table tail-kw
                        :path path}
               reversed {:table head-kw
                         :path (-> path reverse vec)}
               k1-alias (-> k1 last name)
               k2-alias (some-> k2 last name)]
           (if k2
             {k1 (assoc forward
                        :alias k1-alias)
              k2 (assoc reversed
                        :alias k2-alias)}
             {k1 (assoc forward
                        :alias k1-alias)})))
       (apply merge)))

(defn ->from
  [select tables views]
  (-> select
      keys
      first
      name
      (clojure.string/split #"\.")
      first
      keyword))

(declare fff*)

(defn fff** [k v]
  (if (coll? v)
    (mapcat (partial fff* k) v)
    [[k v]]))

(defn fff* [c [k v]]
  (mapcat (partial fff** (conj c k))
       v))

(defn fff
  [m]
  (mapcat (partial fff* [])
       m))

(defn fff->field-kw
  [[a b]]
  [(keyword (str
             (clojure.string/join ">"
                                  (map name a))
             "."
             (name b)))
   (keyword (str
             (clojure.string/join ">"
                                  (map name a))
             "/"
             (name b)))])

(defn ->select
  [select from tables cpaths views]
  (map fff->field-kw (fff {from select})))

(defn www [c [k v]]
  (let [c' (conj c k)]
    (if (map? v)
      (mapcat (partial www c') v)
      [[c' v]])))

(defn ->where* [m]
  (mapcat (partial www [])
          m))

(defn ->where
  [where from tables paths views]
  (let [ws (->> {from where}
                ->where*
                (map (fn [[a b]]
                       [(if (coll? b)
                          :in :=)
                        (keyword
                         (str (->> a
                                   drop-last
                                   (map name)
                                   (clojure.string/join ">"))
                              "."
                              (-> a
                                  last
                                  name)))
                        b])))]
    (if (= (count ws) 1)
      (first ws)
      (into [:and] ws))))

(defn ->path-keys
  [p]
  (->> (map vector
            (into [nil] p)
            (into p [nil]))
       rest
       drop-last))

(defn select-path->join-spec
  [cpaths path]
  (let [[from & path'] path]
    (loop [[head & tail] path'
           table from
           alias-left (name from)
           r []]
      (if head
        (let [pair [table head]
              {t :table p :path a :alias :as x} (cpaths pair)
              a' (str alias-left ">" a)]
          (recur tail
                 t
                 a'
                 (conj r (assoc x
                                :alias-left alias-left
                                :alias-right a'))))
        r))))

(defn spec->joins
  [{:keys [table path alias-left alias-right]}]
  (if (= (count path) 2)
    (let [left-field (->> path
                          first
                          name
                          (str alias-left ".")
                          keyword)
          right-field (->> path
                           last
                           name
                           (str alias-right ".")
                           keyword)]
      [[[table (keyword alias-right)] [:= left-field right-field]]])
    (let [tables (->> path
                      (partition 2)
                      (map last)
                      (map namespace))
          aliases (map #(-> % gensym name) (drop-last tables))
          ts (concat [alias-left]
                     (mapcat vector aliases aliases)
                     [alias-right])
          ts2 (concat aliases [alias-right])]
      (mapv vector
            (map #(vector (keyword %)
                          (keyword %2))
                 tables ts2)
            (->> path
                 (map name)
                 (map #(keyword (str % "." %2)) ts)
                 (partition 2)
                 (mapv (partial into [:=])))))))

(defn ->join
  [select from tables cpaths views]
  (->> (fff {from select})
       (map first)
       (filter #(-> % count (> 1)))
       (mapcat (partial select-path->join-spec cpaths))
       distinct
       (mapcat spec->joins)
       (apply concat)))

(defn ->hsql
  [[from select where] {:keys [tables cpaths views]}]
  (merge
   {:select (->select select from tables cpaths views)
    :from [from]}
   (when where
     {:where (->where where from tables cpaths views)})
   (when-let [join (not-empty (->join select from tables cpaths views))]
     {:join (->join select from tables cpaths views)})))

(defn process-results*
  [r]
  (->> (for [[k v] r]
         [(map keyword
               (conj
                (-> k
                    namespace
                    (clojure.string/split #">"))
                (name k)))
          v])
       (reduce (fn [agg [ks v]]
                 (assoc-in agg ks v))
               {})
       first
       second))

(defn process-results
  [rs]
  (mapv process-results*
        rs))
