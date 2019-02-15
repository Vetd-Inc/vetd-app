(ns vetd-app.hooks)

(defmacro defhook
  [hname default]
  (let [h-kw (-> hname name keyword)]
    `(def
       ~hname
       ^{::type ~h-kw}
       (fn [handle#]
         (get-in @hook-registry
                 [~h-kw handle#]
                 ~default)))))



