(ns test-data
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.db :as db]
            [com.vetd.app.db-schema :as sch]
            [com.vetd.app.auth :as auth]))

(defn ez-uuid [v]
  (java.util.UUID/fromString
   (format 
    "%s0000000-0000-0000-0000-00000000000%s" v v)))

(def pwd-hash
  {"fff"
   "bcrypt+sha512$1d1e4233d82747c2404fb19a329f5811$12$3d91ed00a85b3f5a62b151192be60161fc0cd009a95cab1b"})

(def id-base 100000)

(def ->id
  {:orgs-Vetd (auth/mk-id&str)
   :users-a-f.com (auth/mk-id&str)
   :memb-a-f.com->Vetd (auth/mk-id&str)})



#_(db/drop-all sch/tables2)

#_(db/migrate)

#_(db/create-all2)

;; BuyerOrg1

(db/insert! :orgs
            {:id (first (->id :orgs-Vetd))
             :idstr (second (->id :orgs-Vetd))
             :created (ut/now-ts)
             :oname "Vetd"
             :url "vetd.com"
             :buyer_qm true
             :vendor_qm false})

(db/insert! "users"
            {:id (first (->id :users-a-f.com))
             :idstr (second (->id :users-a-f.com))
             :created (ut/now-ts)
             :email "a@f.com"
             :pwd (pwd-hash "fff")
             :uname "Billy Buyer"})

(db/insert! "memberships"
            {:id (first (->id :memb-a-f.com->Vetd))
             :idstr (second (-> ->id :memb-a-f.com->Vetd))
             :created (ut/now-ts)
             :org_id (first (->id :orgs-Vetd))
             :user_id (first (->id :users-a-f.com))})
