(ns vetd-app.graphql
  (:require [vetd-app.util :as ut]
            [re-frame.core :as rf]
            [reagent.core :as r]))

;; TODO move all this to reagent db ratom?

(def q->sub-id& (atom {}))

#_ (cljs.pprint/pprint @q->sub-id&)

(def sub-id->ratom& (atom {}))

#_ (println @sub-id->ratom&)

(def last-sub-id& (atom (mod (ut/now) 10000)))

(defn get-next-sub-id []
  (swap! last-sub-id& inc))

(rf/reg-event-fx
 :gql/q
 (fn [{:keys [db ws]} [_ q sub-id]]
   {:ws-send {:payload {:cmd :graphql
                        :return {:handler :gql/data
                                 :sub-id sub-id}
                        :query q
                        :subscription? false                        
                        :sub-id sub-id}}}))

(rf/reg-event-fx
 :gql/sub
 (fn [{:keys [db ws]} [_ q sub-id]]
   {:ws-send {:payload {:cmd :graphql
                        :return {:handler :gql/data
                                 :sub-id sub-id}
                        :query q
                        :subscription? true
                        :sub-id sub-id}}}))

(rf/reg-event-fx
 :gql/data
 (fn [{:keys [db]} [_ {:keys [mtype payload]} {:keys [return] :as resp}]]
   (def resp1 resp)
   (let [{:keys [sub-id]} return]
     (case mtype
       :data (if-let [ratom (@sub-id->ratom& sub-id)]
               (reset! ratom payload)
               (do (def no-ratom1 resp)
                   (println "NO GRAPHQL RATOM!")
                   (println resp)))
       :error (do (def err1 resp)
                  (println "GRAPHQL ERROR!")
                  (println err1))
       :complete :NO-OP))
   {}))

(rf/reg-event-fx
 :gql/unsubscribe
 (fn [{:keys [db]} [_ query sub-id]]
#_   (println "gql/unsubscribe")
#_   (println sub-id)
#_   (println query)   
   (swap! q->sub-id& dissoc query)
   (swap! sub-id->ratom& dissoc sub-id)
   {:ws-send {:payload {:cmd :graphql
                        :return {:handler :gql-resp
                                 :sub-id sub-id}
                        :stop true
                        :sub-id sub-id}}}))

(ut/reg-sub-special
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
                     (ut/dispatch-debounce [:gql/q query sub-id]
                                           5000)
                     r)})))

(ut/reg-sub-special
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
