(ns vetd-app.url
  (:require [re-frame.core :as rf]))

(defn replace-end
  [url old-str new-str]
  (if (.endsWith url (str "/" (js/encodeURI old-str)))
    (str (subs url 0 (- (count url)
                        (count (str "/" (js/encodeURI old-str)))))
         (when (not-empty new-str)
           (str "/" (js/encodeURI new-str))))
    (str url
         (when (not-empty new-str)
           (str "/" (js/encodeURI new-str))))))

(rf/reg-cofx
 :url
 (fn [cofx]
   (assoc cofx :url (.. js/window -location -href))))

(rf/reg-fx
 :url
 (fn [url]
   (.replaceState js/history {} "Vetd" url)))
