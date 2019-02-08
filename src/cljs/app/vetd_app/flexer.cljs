(ns vetd-app.flexer)


(defn flx-xfrm-style
  [style]
  (clojure.set/rename-keys style
   {:f/dir :flex-direction
    :f/wrap :flex-wrap
    :f/flow :flex-flow
    :f/grow :flex-grow
    :f/shrink :flex-shrink
    :f/basis :flex-basis}))

(defn flx-class [c]
  (->> c
       vec
       flatten
       distinct
       (remove nil?)
       vec))

(defn flx-props
  [dir {:keys [attrs] :as m}]
  (assoc attrs
         :style (-> m
                    flx-xfrm-style
                    (dissoc :attrs)
                    (merge (when dir
                             {:display :flex
                              :flex-direction dir})))))

(declare flx-children)
(declare interpret-flx-args)

(defn flx-child*
  [args]
  (let [{:keys [attrs children]}
        (interpret-flx-args nil args)]  
    (with-meta (into [:div attrs]
                     children)
      (meta args))))

(defn flx-child
  [ch]
  (let [head (and (sequential? ch)
                  (first ch))]
    (cond (keyword? head) [ch]
          (fn? head) [ch]          
          (set? head) [(flx-child* ch)]
          (map? head) [(flx-child* ch)]
          (sequential? head) (flx-children ch)
          (string? ch) [ch]
          :else (throw
                 (js/Error. (str "flx-child -- what is this? " ch))))))

(defn flx-children
  [chs]
  (mapcat flx-child chs))

(defn flx-args-final
  [{:keys [id class props children]}]
  ;; WEIRD!
  {:attrs (as-> props $
            (merge-with into $
                        (when class
                          {:class class}))
            (merge $ (when id
                       {:id id})))
   :children children})

(defn interpret-flx-args
  [dir args]
  (let [r (loop [state :init
                 [head & tail :as all] args
                 r {}]
            (cond (nil? head) r
                  
                  (and (= state :init)
                       (keyword? head))
                  (recur :id tail (assoc r :id head))

                  (and (#{:init :id} state)
                       (set? head))
                  (recur :class tail (assoc r :class head))

                  (and (#{:init :id :class} state)
                       (map? head))
                  (recur :props tail (assoc r :props head))

                  (= state :props)
                  (assoc r :children all)

                  (or (sequential? head)
                      (string? head))
                  (assoc r :children all)))]
    (-> r
        (update :class flx-class)
        (update :props (partial flx-props dir))
        (update :children flx-children)
        flx-args-final)))


(defn flx
  [dir args]
  (let [{:keys [attrs children]}
        (interpret-flx-args dir args)]
    (let [r (into [:div attrs]
                  children)]
      (println r)
      r)))

(defn row
  [& args]
  (flx :row args))

(defn col
  [& args]
  (flx :column args))
