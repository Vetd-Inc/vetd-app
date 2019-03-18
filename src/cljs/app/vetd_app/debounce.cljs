(ns vetd-app.debounce
  (:require [re-frame.core :as rf]
            [re-frame.router :as rf-router]))

;; reference: https://github.com/Day8/re-frame/pull/249

(def debounced-events (atom {}))

(defn cancel-timeout [id]
  (js/clearTimeout (:timeout (@debounced-events id)))
  (swap! debounced-events dissoc id))

(rf/reg-fx
 :dispatch-debounce
 (fn [dispatches]
   (let [dispatches (if (sequential? dispatches) dispatches [dispatches])]
     (doseq [{:keys [id action dispatch timeout]
              :or   {action :dispatch}}
             dispatches]
       (case action
         :dispatch (do (cancel-timeout id)
                       (swap! debounced-events assoc id
                              {:timeout (js/setTimeout
                                         (fn []
                                           (swap! debounced-events dissoc id)
                                           (rf-router/dispatch dispatch))
                                         timeout)
                               :dispatch dispatch}))
         :cancel (cancel-timeout id)
         :flush (let [ev (get-in @debounced-events [id :dispatch])]
                  (cancel-timeout id)
                  (rf-router/dispatch ev))
         (println "Ignoring bad :dispatch-debounce action: " action ", id: " id))))))
