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


#_(let [[id1 idstr1] (ut/mk-id&str)
        [id2 idstr2] (ut/mk-id&str)
        [id3 idstr3] (ut/mk-id&str)
        [id4 idstr4] (ut/mk-id&str)
        [id5 idstr5] (ut/mk-id&str)
        [id6 idstr6] (ut/mk-id&str)
        [id7 idstr7] (ut/mk-id&str)
        [id8 idstr8] (ut/mk-id&str)]

    (db/insert! :req_templates
                {:id id7
                 :idstr idstr7
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :title "Preposal Request"
                 :rtype "preposal"
                 :rsubtype "preposal1"                 
                 :descr "Basic Preposal Request"})
    
    (db/insert! :prompts
                {:id id1
                 :idstr idstr1
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :prompt "Pricing Estimate"
                 :descr "In what range would you expect this buyer's costs to fall?"})

    (db/insert! :req_template_prompt
                {:id id6
                 :idstr idstr6
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :req_template_id id7
                 :prompt_id id1
                 :sort 0})
    
    (db/insert! :prompt_fields
                {:id id2
                 :idstr idstr2
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :prompt_id id1
                 :fname "value"
                 :descr nil
                 :dtype "n"
                 :list_qm false
                 :sort 0})

    (db/insert! :prompt_fields
                {:id id4
                 :idstr idstr4
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :prompt_id id1
                 :fname "unit"
                 :descr nil
                 :dtype "e-price-per"
                 :list_qm false
                 :sort 2})

    (db/insert! :prompts
                {:id id5
                 :idstr idstr5
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :prompt "Pitch"
                 :descr "Why do we believe you are a fit for this product?"})

    (db/insert! :req_template_prompt
                {:id id8
                 :idstr idstr8
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :req_template_id id7
                 :prompt_id id5
                 :sort 1})
    
    (db/insert! :prompt_fields
                {:id id6
                 :idstr idstr6
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :prompt_id id5
                 :fname "value"
                 :descr nil
                 :dtype "s"
                 :list_qm false
                 :sort 0}))



#_(let [[id idstr] (ut/mk-id&str)]
  )

#_(let [[id idstr] (ut/mk-id&str)]
  (db/insert! :users
              {:id id
               :idstr idstr
               :created (ut/now-ts)
               :updated (ut/now-ts)
               :deleted nil
               :uname "Vetd Support"
               :email "admin"
               :pwd "bcrypt+sha512$3b6415538cad5da4f44467c6f56a3cbe$12$569225967125ab9256b1799616ab63e5186b8f64ad99cd6e"}))

#_(let [[id idstr] (ut/mk-id&str)]
  (db/insert! :admins
              {:id id
               :created (ut/now-ts)
               :updated (ut/now-ts)
               :deleted nil
               :user_id 354804007067}))
