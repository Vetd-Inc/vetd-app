(ns com.vetd.app.buyers
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.common :as com]
            [com.vetd.app.auth :as auth]
            [com.vetd.app.util :as ut]
            [taoensso.timbre :as log]))

(defn search-prods-vendors->ids
  [q]
  (if (not-empty q)
    (let [ids (db/hs-query {:select [[:p.id :pid] [:o.id :vid]]
                            :from [[:products :p]]
                            :join [[:orgs :o] [:= :o.id :p.vendor_id]]
                            :where [:or
                                    [(keyword "~*") :p.pname (str ".*?" q ".*")]
                                    [(keyword "~*") :o.oname (str ".*?" q ".*")]]
                            :limit 30})
          pids (map :pid ids)
          vids (->> ids
                    (map :vid)
                    distinct)]
      {:product-ids pids
       :vendor-ids vids})
      {:product-ids []
       :vendor-ids []}))

(defn search-categories
  [q]
  (if (not-empty q)
    (db/hs-query {:select [[:c.*]]
                  :from [[:categories :c]]
                  :where [(keyword "~*") :c.cname (str ".*?" q ".*")]
                  :limit 5})))

(defn select-rounds-by-ids
  [b-id v-ids]
  (db/hs-query {:select [:*]
                :from [:rounds]
                :where [:and
                        [:= :buyer-id b-id]
                        [:in :vendor-id v-ids]]}))

(defn insert-preposal-req
  [buyer-id vendor-id]
  (db/insert! :preposal_reqs
              {:id (ut/uuid-str)
               :buyer-id buyer-id
               :vendor-id vendor-id
               :created (ut/now)}))

#_(select-prep-reqs-by-ids 3 [1 2 3 4])

(defn invert-vendor-data
  [m]
  (let [m1 (ut/fmap #(group-by (some-fn :vendor-id :id) %)
                    m)
        paths (for [[k v] m1
                    [k2 v2] v]
                [[k2 k] (first v2)])]
    (reduce (fn [agg [ks v]]
              (assoc-in agg ks v))
            {}
            paths)))

(defn insert-cart-item
  [buyer-id prod-id]
  (let [[id idstr] (auth/mk-id&str)]
    (-> (db/insert! :cart_items
                    {:id id
                     :idstr idstr
                     :buyer_id buyer-id                 
                     :product_id prod-id
                     :created (ut/now-ts)
                     :updated (ut/now-ts)})
        first)))

;; TODO there could be multiple preposals/rounds per buyer-vendor pair

;; TODO use session-id to verify permissions!!!!!!!!!!!!
(defmethod com/handle-ws-inbound :search
  [{:keys [buyer-id query]} ws-id sub-fn]
  (-> query
      search-prods-vendors->ids
      (assoc :categories
             (search-categories query))))

(defmethod com/handle-ws-inbound :request-preposal
  [{:keys [buyer-id vendor-id]} ws-id sub-fn]
  (insert-preposal-req buyer-id vendor-id))

(defmethod com/handle-ws-inbound :add-to-cart
  [{:keys [buyer-id prod-id]} ws-id sub-fn]
  (insert-cart-item buyer-id prod-id))
