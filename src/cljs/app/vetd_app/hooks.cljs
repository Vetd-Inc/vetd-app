(ns vetd-app.hooks
  (:require-macros vetd-app.hooks))

(defonce hook-registry (volatile! {}))

#_(vreset! hook-registry {})
#_(cljs.pprint/pprint @hook-registry)

(defn defhook* [hook-kw default]
  (with-meta 
    (fn [& handles]
      (let [reg @hook-registry]
        (or (->> handles
                 (map #(get-in reg
                               [hook-kw %]))
                 (remove nil?)
                 first)
            default)))
    {::type hook-kw}))

(defn reg-hook! [hook-fn handle f]
  (if-let [hook-type (-> hook-fn meta ::type)]
    (vswap! hook-registry assoc-in [hook-type handle] f)
    (throw (js/Error. (str "No hook-type meta found on: " hook-fn)))))

(defn reg-hooks! [hook-fn m]
  (doseq [[k v] m]
    (reg-hook! hook-fn k v)))

;;;; Hooks
(vetd-app.hooks/defhook c-page (constantly nil))

(vetd-app.hooks/defhook c-container (fn [p] [:div p]))

(vetd-app.hooks/defhook c-admin (constantly nil))

(vetd-app.hooks/defhook c-prompt (constantly nil))

(vetd-app.hooks/defhook c-prompt-field (constantly nil))

(vetd-app.hooks/defhook init! (constantly nil))
