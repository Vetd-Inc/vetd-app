(ns vetd-app.util
  (:require [clojure.string :as s]
            [clojure.set]
            [re-frame.core :as rf]
            [re-frame.interop :refer [deref? reagent-id]]
            [reagent.ratom :as rr]
            [reagent.format :as format]
            [re-frame.registrar :as rf-reg]
            [markdown-to-hiccup.core :as md])
  (:import [goog.functions]))

(defonce dispatch-debounce-store& (atom {}))
(defonce debounce-by-id-caller-store& (atom {}))
(defonce debounce-by-id-callee-store& (atom {}))

(defn mk-dispatch-debounce
  [dispatch-vec ms]
  (goog.functions.debounce
   (fn []
     (swap! dispatch-debounce-store&
            dissoc dispatch-vec)
     (rf/dispatch dispatch-vec))
   ms))

(defn dispatch-debounce [dispatch-vec ms]
  (if-let [f (@dispatch-debounce-store& dispatch-vec)]
    (f)
    (let [f (mk-dispatch-debounce dispatch-vec ms)]
      (swap! dispatch-debounce-store&
             assoc dispatch-vec f)
      (f))))

(defn mk-call-debounce-by-id-fn [id ms]
  (goog.functions.debounce
   (fn []
     (let [callee-fn (@debounce-by-id-callee-store& id)]
       (swap! debounce-by-id-caller-store&
              dissoc id)
       (swap! debounce-by-id-callee-store&
              dissoc id)
       (callee-fn)))
   ms))

(defn call-debounce-by-id
  [id ms f]
  (swap! debounce-by-id-callee-store& assoc id f)
  (if-let [caller-f (@debounce-by-id-caller-store& id)]
    (caller-f)
    (let [caller-f (mk-call-debounce-by-id-fn id ms)]
      (swap! debounce-by-id-caller-store&
             assoc id caller-f)
      (caller-f))))

(defn now [] (.getTime (js/Date.)))

;; Number formatters
(def currency-format format/currency-format)
(defn decimal-format [n]
  (.format (goog.i18n.NumberFormat. (.-DECIMAL goog.i18n.NumberFormat.Format)) n))

(defn kw->str
  [kw]
  (str (when-let [n (and (keyword? kw)
                         (namespace kw))]
         (str n "/"))
       (name kw)))

(defn ->vec [v]
  (cond (vector? v) v
        (sequential? v) (vec v)
        (map? v) [v]
        (coll? v) (vec v)
        :else [v]))

(defn fmap [f m]
  (->> (for [[k v] m]
         [k (f v)])
       (into {})))

(defn keep-kv [f m]
  (->> (for [[k v] m]
         (f k v))
       (remove nil?)
       (into {})))

(defn inline-colls
  [tag & tail]
  (into [tag]
        (if (sequential? tail)
          (->> tail
               (map #(if (and (sequential? %)
                              (-> %
                                  first
                                  sequential?))
                       % [%]))
               (apply concat)
               vec)
          tail)))

;; TODO this is not necessary??!!?!?!?!?
(def ic inline-colls)

(defn flexer-xfrm-attrs
  [attrs]
  (update attrs :style
          clojure.set/rename-keys
          {:f/dir :flex-direction
           :f/wrap :flex-wrap
           :f/flow :flex-flow
           :f/grow :flex-grow
           :f/shrink :flex-shrink
           :f/basis :flex-basis}))

(defn flexer-merge-attrs
  [a1 a2]
  (merge a1 a2
         {:class (-> (into (:class a1)
                           (:class a2))
                     distinct
                     vec)
          :style (merge {}
                        (:style a1)
                        (:style a2))}))

(defn flexer
  [{:keys [p c]} & children]
  (let [children' (apply concat children)]
    [ic :div (flexer-xfrm-attrs
              (assoc-in p [:style :display] :flex))
     (for [ch children']
       (let [[attrs & body] ch]
         (into [:div (flexer-xfrm-attrs
                      (flexer-merge-attrs c attrs))]
               body)))]))

(defn find-map-heads [v]
  (cond (-> v sequential? not) v
        (-> v first map?) [v]
        (-> v first sequential?) (->> v
                                      (mapcat find-map-heads)
                                      vec)
        :else (throw
               (js/Error. (str "find-map-heads -- what is this? " v)))))

(defn flx
  [{:keys [p c]} & children]
  (def c1 children)
  (let [chs (find-map-heads children)]
    ;; `into` avoids unique key warnings
    (into [:div (flexer-xfrm-attrs
                 (assoc-in p [:style :display] :flex))]
          (for [ch chs]
            (let [[attrs & body] ch]
              (into [:div (flexer-xfrm-attrs
                           (flexer-merge-attrs c attrs))]
                    body))))))

(defn- map-signals
  "Runs f over signals. Signals may take several
  forms, this function handles all of them."
  [f signals]
  (cond
    (sequential? signals) (map f signals)
    (map? signals) (fmap f signals)
    (deref? signals) (f signals)
    :else '()))

(defn- deref-input-signals
  [signals query-id]
  (let [dereffed-signals (map-signals deref signals)]
    (cond
      (sequential? signals) (map deref signals)
      (map? signals) (fmap deref signals)
      (deref? signals) (deref signals)
      :else (println :error "in the reg-sub for" query-id ", the input-signals function returns:" signals))
    dereffed-signals))

(defn reg-sub-special
  [query-id inputs-fn init-fn]
  (let [err-header (str "reg-sub-special for " query-id ", ")]
    (rf-reg/register-handler
     :sub
     query-id
     (fn subs-handler-fn
       ([db query-vec]
        (let [subscriptions (inputs-fn query-vec)
              reaction-id   (atom nil)
              {:keys [computation auto-set on-dispose on-set]} (init-fn query-vec)
              reaction      (rr/make-reaction
                             (fn []
                               (computation (deref-input-signals subscriptions query-id)
                                            query-vec))
                             :auto-set auto-set
                             :on-dispose on-dispose
                             :on-set on-set)]

          (reset! reaction-id (reagent-id reaction))
          reaction))
       #_       ([db query-vec dyn-vec]
                 (let [subscriptions (inputs-fn query-vec dyn-vec)
                       reaction-id   (atom nil)
                       {:keys [computation auto-set on-dispose on-set]} (init-fn query-vec)              
                       reaction      (apply make-reaction
                                            (fn []
                                              (computation (deref-input-signals subscriptions query-id)
                                                           query-vec
                                                           dyn-vec))
                                            :auto-set auto-set
                                            :on-dispose on-dispose
                                            :on-set on-set)]

                   (reset! reaction-id (reagent-id reaction))
                   reaction))))))

(defn db->current-org-id
  "Given the app-db state, return the current org id of the user."
  [db]
  (->> db
       :memberships
       (filter #(= (:id %) (:active-memb-id db)))
       first
       :org-id))

(defn capitalize-words
  [string]
  (->> (s/split string #"\b")
       (map s/capitalize)
       s/join))

;; TODO truncate leaving words intact (i.e., don't split a word)
(defn truncate-text
  "Truncates text, adding ellipsis."
  [string length]
  (str (subs string 0 length)
       (when (> (count string) length)
         "...")))

(defn parse-md
  "Takes string and parses any Markdown into hiccup components."
  [string]
  (some-> string
          md/md->hiccup
          md/component))

(defn augment-with-keys
  "Add the index as a key to the metadata of each element.
  Note: This is useful for dumping children components into 
  a parent component and avoiding React warnings. However, React
  recommends setting the 'key' to a unique consistent value, such
  as an ID, rather than using simply an iterative index."
  [xs]
  (map-indexed
   (fn [i x] (with-meta x {:key i}))
   xs))

;;;; Base 31/36 idstr calculation
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
      (let [d (* (base31-inv (.charCodeAt head 0)) (Math/pow 31 idx))]
        (recur tail
               (inc idx)
               (+ r d))))))

;;;; DOM
(defn nodes-by-class
  [class]
  (-> js/document
      (.getElementsByClassName class)
      array-seq))

(defn first-node-by-class
  [class]
  (first (nodes-by-class class)))

(defn add-class
  [node class]
  (.add (.-classList node) class))

(defn remove-class
  [node class]
  (.remove (.-classList node) class))

(defn contains-class?
  [node class]
  (.contains (.-classList node) class))
