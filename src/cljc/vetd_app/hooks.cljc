(ns vetd-app.hooks)

(defmacro defhook
  [hname default]
  (let [h-kw (-> hname name keyword)]
    `(def
       ~hname
       (defhook* ~h-kw ~default))))


