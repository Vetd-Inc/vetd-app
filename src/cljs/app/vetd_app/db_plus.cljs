(ns vetd-app.db-plus
  (:require [vetd-app.util :as ut]   
            [reagent.core :as r]
            [re-frame.core :as rf]))

;; TODO
;; - delete map => delete children
;; - block direct delete children

(declare extract-scalar-idxs)

(def db& (volatile! {}))

#_ (println @db&)

;; TODO move custom config
(def config& (volatile! {:uniq-idx #{:db+/id :an/id}
                         :many-idx #{:rtype :an/reload-data? :an/req-data? :dest}
                         :idx-fns #{#'extract-scalar-idxs}
                         :childware #{}}))
(def ^:dynamic *children* nil)

;; ========================================
(defn fields->children
  [m f]
  (if (= (:ntype m) :an/node)
    (let [f' (comp #(assoc %
                           :an/parent-id (:an/id m))
                   f)]
      (update-in m
                 [:nspec :an/fields]
                 (partial mapv f')))
    m))

(vswap! config& update :childware conj fields->children)

;; ========================================


(defn reset-db! [] (vreset! db& {}))

(defn kv-scalar?
  [[k v]]
  (or (keyword? k)
      (number? k)
      (string? k)))

(defn extract-scalar-idxs
  [m]
  (let [{:keys [uniq-idx many-idx]} @config&
        idxs (clojure.set/union uniq-idx many-idx)]
    (->> (select-keys m idxs)
         (filter kv-scalar?))))

(defn ensure-id
  [v]
  (if (:db+/id v)
    v
    (assoc v
           :db+/id
           (keyword (gensym "id")))))

(defn get-idx
  [idx]
  (let [db @db&]
    (if-let [ra (db idx)]
      ra
      (let [ra (r/atom {})]
        (vswap! db& assoc idx ra)
        ra))))

(defn query-idx
  [idx idx-k v]
  (let [r (idx v)]
    (if ((:uniq-idx @config&) idx-k)
      r
      (vals r))))

(defn query
  [[k v] & tail]
  (query-idx @(get-idx k) k v))

(defn extract-idx-pairs
  [m]
  (let [{:keys [uniq-idx idx-fns]} @config&
        idx-m (->> (map #(% m) idx-fns)
                   (apply merge))]
    {:uniq (filter #(-> % first uniq-idx)
                   idx-m)
     :many (remove #(-> % first uniq-idx)
                   idx-m)}))

#_(defn delete!
  [id]
  (when-let [v (query [:db+/id id] [])]
    (let [ks (->> (dissoc v :db+/id)
                  (filter kv-idxable?))]
      (doseq [[k1 k2] ks]
        (r/rswap! (get-idx k1)
                  update k2 dissoc id))
      (r/rswap! (get-idx :db+/id)
                dissoc id))))

(defn delete!
  [id]
  (when-let [m (query [:db+/id id])]
    (let [{:keys [uniq many]} (extract-idx-pairs m)]
      (doseq [[k1 k2] many]
        (r/rswap! (get-idx k1)
                  update k2 dissoc id))
      (doseq [[k1 k2] uniq]
        (r/rswap! (get-idx k1)
                  dissoc k2)))))

#_(defn upsert!**
  [v]
  (let [{:keys [:db+/id] :as v'} (ensure-id v)
        ks (->> (dissoc v :db+/id)
                (filter kv-idxable?))]
    (doseq [[k1 k2] ks]
      (r/rswap! (get-idx k1)
                assoc-in
                [k2 id]
                v'))
    (r/rswap! (get-idx :db+/id)
              assoc
              id
              v')
    v'))

(defn upsert!**
  [{:keys [:db+/id] :as m}]
  (let [{:keys [uniq many]} (extract-idx-pairs m)]
    (doseq [[k1 k2] many]
      (r/rswap! (get-idx k1)
                assoc-in
                [k2 id]
                m))
    (doseq [[k1 k2] uniq]
      (r/rswap! (get-idx k1)
                assoc k2
                m))
    m))

(defn upsert!* [{:keys [:db+/id] :as v}]
  (delete! id)
  (upsert!** v))

(declare upsert!)

(defn upsert!
  [v]
  (when *children*
    (vswap! *children* conj v))
  (binding [*children* (volatile! #{})]
    (let [v' (reduce #(%2 % upsert!)
                     (ensure-id v)
                     (:childware @config&))]
#_      (println v')
      (-> v'
          (vary-meta assoc :db+/children @*children*)
          upsert!*))))

(rf/reg-event-fx
 :db+/do-upsert
 (fn [_ [_ vs]]
   {:db+/upsert vs}))



#_ (println @(:an/parent-id @db&))

#_ (:n @(:dtype @db&))

(rf/reg-fx
 :db+/upsert
 (fn [vals]
   (doseq [v vals]
     (upsert! v))))

(rf/reg-fx
 :db+/delete
 (fn [vals]
   (doseq [{:keys [:db+/id]} vals]
     (when id
       (delete! id)))))

(rf/reg-fx
 :db+/swap
 (fn [[[idx id] f]]
   (let [r (query [idx id])]
     (if (map? r)
       (-> r f upsert!)
       (doseq [v r]
         (-> v f upsert!))))))

(rf/reg-sub
 :db+/idx
 (fn [[_ idx] _]
   (get-idx idx))
 (fn [idx _] idx))

(rf/reg-sub
 :db+/q
 (fn [[_ [idx]] _]
   (rf/subscribe [:db+/idx idx]))
 (fn [idx [_ [idx-k v] & tail]]
   (query-idx idx idx-k v)))

(rf/reg-sub
 :db+/id
 (fn [_ _]
   (rf/subscribe [:db+/idx :db+/id]))
 (fn [idx [_ id]]
   (idx id)))

(rf/reg-cofx
 :db+/q
 (fn [cofx q]
   (assoc cofx :db+/q (apply query q))))

#_(rf/dispatch-sync [:db+/do-upsert [{:a 33}]])

#_(rf/dispatch-sync [:db+/do-upsert [{:db+/id :id10 :a 44}]])

#_ @(rf/subscribe [:db+/q [:a 33]])

#_ @(rf/subscribe [:db+/q [:ds/imported "false"]])

#_ (keys @(rf/subscribe [:db+/idx :db+/id]))

#_  (println @(rf/subscribe [:db+/id :id30]))

#_ (query [:an/id :n6])

#_ (rf/clear-subscription-cache!)
