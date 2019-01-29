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
  {:orgs-BuyerOrg1 (* id-base 1)
   :orgs-VendorOrg2 (* id-base 2)
   :users-a-f.com (* id-base 3)
   :users-b-f.com (* id-base 4)
   :memb-a-f.com->BuyerOrg1 (* id-base 5)
   :memb-b-f.com->VendorOrg2 (* id-base 6)
   :prods-Sprocket1-VendorOrg2 (* id-base 7)
   :prods-Sprocket2-VendorOrg2 (* id-base 8)
   :orgs-VendorOrg3 (* id-base 9)
   :users-c-f.com (* id-base 10)
   :memb-c-f.com->VendorOrg3 (* id-base 11)
   :prods-Cogs1-VendorOrg3 (* id-base 12)
   :prods-Sprocket3-VendorOrg3 (* id-base 13)
   :prep-reqs-BuyerOrg1-Sprocket2 (* id-base 14)
   :preps-BuyerOrg1-Sprocket2 (* id-base 15)})



(db/drop-all sch/tables2)

#_(db/migrate)

(db/create-all2)

;; BuyerOrg1

(db/insert! "orgs"
            {:id (->id :orgs-BuyerOrg1)
             :idstr (-> ->id :orgs-BuyerOrg1 auth/base36->str)
             :created (ut/now-ts)
             :oname "Buyer Org 1"
             :url "buyer-org1.com"
             :buyer_qm true
             :vendor_qm false})

(db/insert! "users"
            {:id (->id :users-a-f.com)
             :idstr (-> ->id :users-a-f.com auth/base36->str)
             :created (ut/now-ts)
             :email "a@f.com"
             :pwd (pwd-hash "fff")
             :uname "Billy Buyer"})

(db/insert! "memberships"
            {:id (->id :memb-a-f.com->BuyerOrg1)
             :idstr (-> ->id :memb-a-f.com->BuyerOrg1 auth/base36->str)
             :created (ut/now-ts)
             :org_id (->id :orgs-BuyerOrg1)
             :user_id (->id :users-a-f.com)})

;; VendorOrg2

(db/insert! "orgs"
            {:id (->id :orgs-VendorOrg2)
             :idstr (-> ->id :orgs-VendorOrg2 auth/base36->str)
             :created (ut/now-ts)
             :oname "Vendor Org2"
             :url "vendor-org2.com"             
             :buyer_qm false
             :vendor_qm true})

(db/insert! "users"
            {:id (->id :users-b-f.com)
             :idstr (-> ->id :users-b-f.com auth/base36->str)
             :created (ut/now-ts)
             :email "b@f.com"
             :pwd (pwd-hash "fff")
             :uname "Vicki Vendor"})

(db/insert! "memberships"
            {:id (->id :memb-b-f.com->VendorOrg2)
             :idstr (-> ->id :memb-b-f.com->VendorOrg2 auth/base36->str)
             :created (ut/now-ts)
             :org_id (->id :orgs-VendorOrg2)
             :user_id  (->id :users-b-f.com)})

(db/insert! "products"
            {:id (->id :prods-Sprocket1-VendorOrg2)
             :idstr (-> ->id :prods-Sprocket1-VendorOrg2 auth/base36->str)
             :created (ut/now-ts)
             :pname "Original Sprocket"
             :vendor_id (->id :orgs-VendorOrg2)
             :short_desc "The World's First Sprocket"
             :long_desc "This sprocket has various applications. It has been engineered for peak performance." })

(db/insert! "products"
            {:id (->id :prods-Sprocket2-VendorOrg2)
             :idstr (-> ->id :prods-Sprocket2-VendorOrg2 auth/base36->str)             
             :created (ut/now-ts)
             :pname "2nd-gen Sprocket"
             :vendor_id (->id :orgs-VendorOrg2)
             :short_desc "A more refined sprocket."
             :long_desc "The 2nd generation of sprockets incorporate new features and qualities, without sacrificing aesthetics. Odor has been improved as well." })


;; VendorOrg3

(db/insert! "orgs"
            {:id (->id :orgs-VendorOrg3)
             :idstr (-> ->id :orgs-VendorOrg3 auth/base36->str)
             :created (ut/now-ts)
             :oname "Vendor Org3"
             :url "vendor-org3.com"
             :buyer_qm false
             :vendor_qm true})

(db/insert! "users"
            {:id (->id :users-c-f.com)
             :idstr (-> ->id :users-c-f.com auth/base36->str)
             :created (ut/now-ts)
             :email "c@f.com"
             :pwd (pwd-hash "fff")
             :uname "Sally Draper"})

(db/insert! "memberships"
            {:id (->id :memb-c-f.com->VendorOrg3)
             :idstr (-> ->id :memb-c-f.com->VendorOrg3 auth/base36->str)
             :created (ut/now-ts)
             :org_id (->id :orgs-VendorOrg3)
             :user_id  (->id :users-c-f.com)})

(db/insert! "products"
            {:id (->id :prods-Cogs1-VendorOrg3)
             :idstr (-> ->id :prods-Cogs1-VendorOrg3 auth/base36->str)
             :created (ut/now-ts)
             :pname "Comfortable Cog"
             :vendor_id (->id :orgs-VendorOrg3)
             :short_desc "A cog designed from the bottom up for total comfort."
             :long_desc "Traditionally, cogs have been uncomfortable and non-descript. But, now there's the comfortable cog. It features high-end comfort." })

(db/insert! "products"
            {:id (->id :prods-Sprocket3-VendorOrg3)
             :idstr (-> ->id :prods-Sprocket3-VendorOrg3 auth/base36->str)             
             :created (ut/now-ts)
             :pname "Sprickety Sprocket"
             :vendor_id (->id :orgs-VendorOrg3)
             :short_desc "A sprocket with panache!"
             :long_desc "It is very common to find sprockets that lack panache. This sprocket, however, has it in spades. Everyone is sure to notice when you utilize this sprocket for its intended purpose." })

(db/insert! "preposal_reqs"
            {:id (->id :prep-reqs-BuyerOrg1-Sprocket2)
             :idstr (-> ->id :prep-reqs-BuyerOrg1-Sprocket2 auth/base36->str)
             :created (ut/now-ts)
             :buyer_id (->id :orgs-BuyerOrg1)
             :buyer_user_id (->id :users-a-f.com)
             :product_id (->id :prods-Sprocket2-VendorOrg2)})

(db/insert! "preposals"
            {:id (->id :preps-BuyerOrg1-Sprocket2)
             :idstr (-> ->id :preps-BuyerOrg1-Sprocket2 auth/base36->str)
             :created (ut/now-ts)
             :buyer_id (->id :orgs-BuyerOrg1)
             :product_id (->id :prods-Sprocket2-VendorOrg2)})
