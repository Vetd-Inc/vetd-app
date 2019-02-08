(ns vetd-app.hooks)

(defmacro defhook
  [hname default]
  (let [h-kw (-> hname name keyword)]
    `(def
       ~hname
        ^{::type ~h-kw}
       (fn
         [handle# & args#]
         (if-let [hk# (get-in hook-registry [~h-kw handle#])]
           (apply hk# args#)
           (apply ~default args#))))))



