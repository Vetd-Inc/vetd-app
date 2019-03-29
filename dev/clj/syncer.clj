(ns syncer
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as hs]
            [com.vetd.app.server :as svr]
            [aleph.http :as ah]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [cheshire.core :as json]))

(defonce ws& (atom nil))

(defn ws-on-closed []
  :NOOP)

(defn ws-ib-handler
  [data]
  (-> data svr/read-transit-string clojure.pprint/pprint))

(defn mk-ws []
  (let [ws @(ah/websocket-client #_"ws://localhost:5080/ws"
                                 "wss://app.vetd.com/ws")]
    (ms/on-closed ws ws-on-closed)
    (ms/consume ws-ib-handler ws)
    (reset! ws& ws)
    ws))

#_(mk-ws)

#_(svr/respond-transit {:cmd :graphql
                        :sub-id (keyword (gensym))
                      :return :yes
                      :subscription? false
                      :query {:queries [[:forms {:_limit 1} [:id :__typename]]]}}
                     @ws&)

#_(svr/respond-transit {:cmd :graphql
                        :sub-id (keyword (gensym))
                      :return :yes
                      :subscription? false
                        :query {:queries
                                [[:forms
                                  {:ftype ["vendor-profile" "product-profile"]}
                                  [:id :__typename :ftype
                                   [:prompts [:id :__typename :prompt
                                              [:fields [:id :__typename :fname]]]]]]]}}
                     @ws&)

#_(.close @ws&)
