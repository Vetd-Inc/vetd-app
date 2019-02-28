(ns vetd-app.cookies
  (:require [re-frame.core :as rf]
            [vetd-app.util :refer [kw->str]]
            goog.net.cookies))

(defn set-cookie!
  "sets a cookie, the max-age for session cookie
   following optional parameters may be passed in as a map:
   :max-age - defaults to -1
   :path - path of the cookie, defaults to the full request path
   :domain - domain of the cookie, when null the browser will use the full request host name
   :secure? - boolean specifying whether the cookie should only be sent over a secure channel"
  [k content & [{:keys [max-age path domain secure?]} :as opts]]
  (let [k' (kw->str k)
        content' (clj->js content)]
    (if-not opts
      (.set goog.net.cookies k' content')
      (.set goog.net.cookies k' content' (or max-age -1) path domain (boolean secure?)))))

(rf/reg-cofx 
 :cookies
 (fn [cofx local-store-keys]
   (->> (for [k local-store-keys]
          [k (js->clj (.get goog.net.cookies (kw->str k)))])
        (into {})
        (assoc cofx :local-store))))

(rf/reg-fx
 :cookies
 (fn [m]
   (doseq [[k v] m]
     (if (vector? v)
       (apply set-cookie! k v)
       (set-cookie! k v)))))

