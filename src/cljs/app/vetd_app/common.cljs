(ns vetd-app.common)


(defmulti c-plugin-hook (fn [component & _] component))

(defmethod c-plugin-hook :default [& _] nil)

(defn c-plugin [component]
  (fn [& c-args]
    (apply c-plugin-hook component c-args)))

(defmacro defn-c-plugin
  [cname & body]
  `(defmulti c-plugin-hook
     ~(keyword (namespace cname) (name cname))
     ~@body))


(defmulti fn-plugin-hook (fn [cmd & args] cmd ))

(defmethod fn-plugin-hook :default [& _] nil)

