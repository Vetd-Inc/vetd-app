(ns com.vetd.app.proc-tree)

(defmacro tree-assoc-fn
  [key-bindings & body]
  `(fn [x#]
     (let [{:keys ~key-bindings :as x#} (assoc x#
                                               :parent
                                               (-> x# :parents last))
           r# (do ~@body)]
       (apply assoc x# r#))))

(declare proc-tree)

(defn proc-tree-child-reducer
  [m v' agg [k vs]]
  (if-let [mk (and m (m k))]
    (assoc agg
           k
           (mapv #(dissoc (proc-tree mk
                                      (merge v' %))
                          :parents)
                 vs))
    agg))

(defn proc-tree-map
  [m {:keys [item children parents] :as v}]
  (let [parents' (conj (or parents [])
                       item)
        v' (-> v
               (dissoc :item
                       :children)
               (assoc :parents parents'))]
    (assoc v
           :children
           (reduce (partial proc-tree-child-reducer
                            m
                            v')
                   {}
                   (if (map? children)
                     children
                     {(ffirst m) children})))))

(defn proc-tree
  [[& ops] v]
  (loop [[head & tail] (flatten ops)
         v' v]
    (if head
      (recur (flatten tail)
             (if (map? head)
               (proc-tree-map head v')
               (head v')))
      v')))
