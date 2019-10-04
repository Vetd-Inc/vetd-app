(ns vetd-app.graphql
  (:require [vetd-app.util :as util]
            [re-frame.core :as rf]
            [reagent.core :as r]))

;; TODO move all this to reagent db ratom?

(def q->sub-id& (atom {}))

#_ (cljs.pprint/pprint @q->sub-id&)

(def sub-id->ratom& (atom {}))

#_ (println @sub-id->ratom&)

#_ (println (keys @sub-id->ratom&))

(def last-sub-id& (atom (mod (util/now) 10000)))

(defn get-next-sub-id []
  (swap! last-sub-id& inc))

(rf/reg-event-fx
 :gql/q
 (fn [{:keys [db ws local-store]} [_ q sub-id]]
   {:ws-send {:payload {:cmd :graphql
                        :return {:handler :gql/data
                                 :sub-id sub-id}
                        :query q
                        :subscription? false                        
                        :sub-id sub-id}}}))

(rf/reg-event-fx
 :gql/sub
 (fn [{:keys [db ws local-store]} [_ {:keys [admin?] :as q} sub-id]]
   {:ws-send {:payload {:cmd :graphql
                        :return {:handler :gql/data
                                 :sub-id sub-id}
                        :query q
                        :admin? admin?
                        :subscription? true
                        :sub-id sub-id}
              :subscription :start}}))

(rf/reg-event-fx
 :gql/data
 (fn [{:keys [db]} [_ {:keys [mtype payload]} {:keys [return] :as resp}]]
   (or (let [{:keys [sub-id]} return]
         (case mtype
           :data (do (if-let [ratom (@sub-id->ratom& sub-id)]
                       (when-not (= payload @ratom)
                         (reset! ratom payload))
                       (do (println "NO GRAPHQL RATOM!")
                           (println resp)))
                     nil)
           :error (do (println "GRAPHQL ERROR!")
                      (println resp)
                      nil)
           :complete (do :NO-OP nil)

           (do (println "WHAT IS THIS???")
               (println resp)
               {:toast {:type "error"
                        :title "Sorry, you do not have access to this page."}})))
       {})))

(rf/reg-event-fx
 :gql-resp
 (fn [{:keys [db]} [_ {:keys [mtype payload]} {:keys [return] :as resp}]]
   {}))

(rf/reg-event-fx
 :gql/unsubscribe
 (fn [{:keys [db]} [_ query sub-id]]
   (swap! q->sub-id& dissoc query)
   (swap! sub-id->ratom& dissoc sub-id)
   {:ws-send {:payload {:cmd :graphql
                        :return {:handler :gql-resp
                                 :sub-id sub-id}
                        :stop true
                        :sub-id sub-id}
              :subscription :stop}}))

(util/reg-sub-special
 :gql/q
 (fn [[_ query]]
   (let [q->sub-id @q->sub-id&
         sub-id->ratom @sub-id->ratom&]
     (if-let [ratom (-> query q->sub-id sub-id->ratom)]
       ratom
       (let [ratom (r/atom :loading)
             sub-id (get-next-sub-id)]
         (swap! q->sub-id& assoc query sub-id)
         (swap! sub-id->ratom& assoc sub-id ratom)
         ratom))))
 (fn [[_ query]]
   (let [q->sub-id @q->sub-id&
         sub-id (q->sub-id query)]
     (rf/dispatch-sync [:gql/q query sub-id]) 
     {:computation (fn [r]
                     (util/dispatch-debounce [:gql/q query sub-id]
                                             5000)
                     r)})))

(util/reg-sub-special
 :gql/sub
 (fn [[_ query]]
   (let [q->sub-id @q->sub-id&
         sub-id->ratom @sub-id->ratom&]
     (if-let [ratom (-> query q->sub-id sub-id->ratom)]
       ratom
       (let [ratom (r/atom :loading)
             sub-id (get-next-sub-id)]
         (swap! q->sub-id& assoc query sub-id)
         (swap! sub-id->ratom& assoc sub-id ratom)
         ratom))))
 (fn [[_ query]]
   (let [q->sub-id @q->sub-id&
         sub-id (q->sub-id query)]
     (rf/dispatch-sync [:gql/sub query sub-id])
     {:computation identity
      :on-dispose #(rf/dispatch [:gql/unsubscribe query sub-id])})))

