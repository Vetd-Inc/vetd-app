(ns com.vetd.app.migrations
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.migragen :as mig]            
            [com.vetd.app.db-copier :as cp]
            [com.vetd.app.db :as db]            
            [clojure.java.jdbc :as j]
            [taoensso.timbre :as log]
            clojure.edn
            clojure.pprint))


(def mig-2019-02-04-copy-from-categories-up
  (mig/mk-copy-from-up-fn "data/categories.sql"))

(def mig-2019-02-04-copy-from-categories-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :categories
    :where [:between :id 272814110001 272814110725]}))

(def mig-2019-02-04-copy-from-orgs-up
  (mig/mk-copy-from-up-fn "data/orgs.sql"))

(def mig-2019-02-04-copy-from-orgs-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :orgs
    :where [:between :id 273818389861 272814405123]}))

(def mig-2019-02-04-copy-from-products-up
  (mig/mk-copy-from-up-fn "data/products.sql"))

(def mig-2019-02-04-copy-from-products-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :products
    :where [:between :id 272814695125 272814743922]}))

(def mig-2019-02-05-copy-from-product-categories-up
  (mig/mk-copy-from-up-fn "data/product-categories.sql"))

(def mig-2019-02-05-copy-from-product-categories-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :product_categories
    :where [:between :id 273266515726 273266499717]}))

(def mig-2019-02-10-copy-from-admins-up
  (mig/mk-copy-from-up-fn "data/mig-2019-02-10-admins.sql"))

(def mig-2019-02-10-copy-from-admins-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :admins
    :where [:= :id 354836657068]}))

(def mig-2019-02-10-copy-from-users-up
  (mig/mk-copy-from-up-fn "data/mig-2019-02-10-users.sql"))

(def mig-2019-02-10-copy-from-users-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :users
    :where [:= :id 354836657067]}))


(def migrations
  [[[2019 2 4 00 00]
    [:create-table {:schema :vetd
                    :name :orgs
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :oname [:text]
                              :buyer_qm [:boolean]
                              :vendor_qm [:boolean]
                              :short_desc [:text]
                              :long_desc [:text]
                              :url [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :products
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :pname [:text]
                              :vendor_id [:bigint]
                              :short_desc [:text]
                              :long_desc [:text]
                              :logo [:text]
                              :url [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :users
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :uname [:text]
                              :email [:text]
                              :pwd [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT :INSERT]}}]

    [:create-table {:schema :vetd
                    :name :sessions
                    :columns {:id [:bigint :NOT :NULL]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :token [:text]
                              :user_id [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT :INSERT]}}]

    [:create-table {:schema :vetd
                    :name :categories
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :cname [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :product_categories
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :prod_id [:bigint]
                              :cat_id  [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-view {:schema :vetd
                   :name :categories_by_product
                   :honey {:select [[:pc.id :pcid]
                                    :pc.prod_id
                                    :c.id
                                    :c.idstr
                                    :c.cname]
                           :from [[:product_categories :pc]]
                           :join [[:categories :c]
                                  [:= :c.id :pc.cat_id]]}
                   :owner :vetd}]

    [:create-table {:schema :vetd
                    :name :memberships
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :org_id [:bigint]
                              :user_id [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :rounds
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :buyer_id [:bigint]
                              :status [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :round_category
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :round_id [:bigint]
                              :category_id [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :round_product
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :round_id [:bigint]
                              :product_id [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-view {:schema :vetd
                   :name :rounds_by_category
                   :honey {:select [[:rc.id :rcid]
                                    :rc.category_id
                                    :r.id
                                    :r.idstr                                    
                                    :r.created
                                    :r.buyer_id
                                    :r.status]
                           :from [[:round_category :rc]]
                           :join [[:rounds :r]
                                  [:= :r.id :rc.round_id]]}
                   :owner :vetd}]

    [:create-view {:schema :vetd
                   :name :rounds_by_product
                   :honey {:select [[:rp.id :rcid]
                                    :rp.product_id
                                    :r.id
                                    :r.idstr                                    
                                    :r.created
                                    :r.buyer_id
                                    :r.status]
                           :from [[:round_product :rp]]
                           :join [[:rounds :r]
                                  [:= :r.id :rp.round_id]]}
                   :owner :vetd}]

    [:copy-from '{:name :categories
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-04-copy-from-categories-up
                  :down-fn mig-2019-02-04-copy-from-categories-down}]

    [:copy-from '{:name :orgs
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-04-copy-from-orgs-up
                  :down-fn mig-2019-02-04-copy-from-orgs-down}]

    [:copy-from '{:name :products
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-04-copy-from-products-up
                  :down-fn mig-2019-02-04-copy-from-products-down}]]

   [[2019 2 5 00 00]
    [:copy-from '{:name :product_categories
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-05-copy-from-product-categories-up
                  :down-fn mig-2019-02-05-copy-from-product-categories-down}]]

   [[2019 2 6 00 00]
    [:create-table {:schema :vetd
                    :name :docs
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :title [:text]
                              :dtype [:text] ;;preposal, profile etc???
                              :descr [:text]
                              :notes [:text]                              
                              :from_org_id [:bigint]
                              :from_user_id [:bigint]
                              :to_org_id [:bigint]
                              :to_user_id [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :doc_resp
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :doc_id [:bigint]
                              :resp_id [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :req_template
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :title [:text]
                              ;; preposal, profile etc????
                              :rtype [:text]
                              :descr [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :req_template_prompt
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :req_template_id [:bigint]
                              :prompt_id [:bigint]
                              :sort [:integer]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]
    
    [:create-table {:schema :vetd
                    :name :reqs
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :req_template_id [:bigint]
                              :title [:text]
                              :descr [:text]                              
                              :notes [:text]
                              :from_org_id [:bigint]
                              :from_user_id [:bigint]
                              :to_org_id [:bigint]
                              :to_user_id [:bigint]
                              :status [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :req_prompt
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :req_id [:bigint]
                              :prompt_id [:bigint]
                              :sort [:integer]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]
    
    [:create-table {:schema :vetd
                    :name :prompts
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :prompt [:text]
                              :descr [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :prompt_fields
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :prompt_id [:bigint]
                              :fname [:text]
                              :descr [:text]
                              :dtype [:text]
                              :list_qm [:boolean]
                              :sort [:integer]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :responses
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :prompt_id [:bigint]
                              :user_id [:bigint]
                              :org_id [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :resp_fields
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :resp_id [:bigint]
                              :pf_id [:bigint]
                              :idx [:integer] ;; for lists
                              :sval [:text]
                              :nval [[:numeric 12 3]]
                              :dval [:timestamp :with :time :zone]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :enums
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :dtype [:text]
                              :descr [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :enum_vals
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :value [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-view {:schema :vetd
                   :name :responses_by_doc
                   :honey {:select [[:dr.id :drid]
                                    :dr.doc_id
                                    :r.id
                                    :r.idstr                                    
                                    :r.created
                                    :r.updated
                                    :r.deleted
                                    :r.prompt_id
                                    :r.user_id
                                    :r.org_id]
                           :from [[:doc_resp :dr]]
                           :join [[:responses :r]
                                  [:= :r.id :dr.resp_id]]}
                   :owner :vetd}]

    [:create-view {:schema :vetd
                   :name :prompts_by_req
                   :honey {:select [[:rp.id :rpid]
                                    :rp.req_id
                                    :p.id
                                    :p.idstr                                    
                                    :p.created
                                    :p.updated
                                    :p.deleted
                                    :p.prompt
                                    :p.descr]
                           :from [[:req_prompt :rp]]
                           :join [[:prompts :p]
                                  [:= :p.id :rp.prompt_id]]}
                   :owner :vetd}]]
   
   [[2019 2 10 00 00]
    [:create-table {:schema :vetd
                    :name :admins
                    :columns {:id [:bigint :NOT :NULL]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :user_id [:bigint]}
                    :owner :vetd
                    :grants {}}]
    [:copy-from '{:name :mig-2019-02-10-copy-from-users
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-10-copy-from-users-up
                  :down-fn mig-2019-02-10-copy-from-users-down}]
    [:copy-from '{:name :mig-2019-02-10-copy-from-admins
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-10-copy-from-admins-up
                  :down-fn mig-2019-02-10-copy-from-admins-down}]]])


(def hasura-meta-cfg2
  {:remote_schemas []
   :query_templates []
   :rels [{:tables [:vetd :categories
                    :vetd :rounds_by_category]
           :fields [:rounds]
           :cols [:id :category_id]
           :rel :one-many}

          {:tables [:vetd :orgs
                    :vetd :memberships]
           :fields [:memberships :org]
           :cols [:id :org_id]
           :rel :one-many}

          {:tables [:vetd :orgs
                    :vetd :products]
           :fields [:products :vendor]
           :cols [:id :vendor_id]
           :rel :one-many}

          {:tables [:vetd :products
                    :vetd :rounds_by_product]
           :fields [:rounds]
           :cols [:id :product_id]
           :rel :many-many}

          {:tables [:vetd :products
                    :vetd :categories_by_product]
           :fields [:categories]
           :cols [:id :prod_id]
           :rel :many-many}

          {:tables [:vetd :users
                    :vetd :memberships]
           :fields [:memberships :user]
           :cols [:id :user_id]
           :rel :one-many}

          {:tables [:vetd :users
                    :vetd :sessions]
           :fields [:sessions :user]
           :cols [:id :user_id]
           :rel :one-many}

          {:tables [:vetd :docs
                    :vetd :responses_by_doc]
           :fields [:responses]
           :cols [:id :doc_id]
           :rel :many-many}

          {:tables [:vetd :docs
                    :vetd :orgs]
           :fields [:from-org :docs-out]
           :cols [:from_org_id :id]
           :rel :many-one}

          {:tables [:vetd :docs
                    :vetd :orgs]
           :fields [:to-org :docs-in]
           :cols [:to_org_id :id]
           :rel :many-one}

          {:tables [:vetd :docs
                    :vetd :users]
           :fields [:from-user :docs-out]
           :cols [:from_user_id :id]
           :rel :many-one}

          {:tables [:vetd :docs
                    :vetd :users]
           :fields [:to-user :docs-in]
           :cols [:to_user_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :orgs]
           :fields [:from-org :reqs-out]
           :cols [:from_org_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :orgs]
           :fields [:to-org :reqs-in]
           :cols [:to_org_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :users]
           :fields [:from-user :reqs-out]
           :cols [:from_user_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :users]
           :fields [:to-user :reqs-in]
           :cols [:to_user_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :prompts_by_req]
           :fields [:prompts]
           :cols [:id :req_id]
           :rel :one-many}

          {:tables [:vetd :prompts
                    :vetd :prompt_fields]
           :fields [:fields :prompt]
           :cols [:id :prompt_id]
           :rel :one-many}

          {:tables [:vetd :responses
                    :vetd :prompts]
           :fields [:prompt :responses]
           :cols [:prompt_id :id]
           :rel :many-one}

          {:tables [:vetd :responses
                    :vetd :users]
           :fields [:prompt :users]
           :cols [:prompt_id :id]
           :rel :many-one}

          {:tables [:vetd :responses
                    :vetd :prompts]
           :fields [:prompt :responses]
           :cols [:prompt_id :id]
           :rel :many-one}

          {:tables [:vetd :responses
                    :vetd :users]
           :fields [:user :responses]
           :cols [:user_id :id]
           :rel :many-one}
          
          {:tables [:vetd :responses
                    :vetd :orgs]
           :fields [:org :responses]
           :cols [:org_id :id]
           :rel :many-one}

          {:tables [:vetd :responses
                    :vetd :resp_fields]
           :fields [:fields :response]
           :cols [:id :resp_id]
           :rel :one-many}

          {:tables [:vetd :resp_fields
                    :vetd :prompt_fields]
           :fields [:prompt-field :resp-field]
           :cols [:pf_id :id]
           :rel :many-one}]})

#_(mig/mk-migration-files migrations
                          "migrations")

#_
(mig/proc-hasura-meta-cfg
 hasura-meta-cfg)

#_
(mig/proc-hasura-meta-cfg2
 hasura-meta-cfg2)


#_(let [[id1 idstr1] (ut/mk-id&str)
      [id2 idstr2] (ut/mk-id&str)
      [id3 idstr3] (ut/mk-id&str)
      [id4 idstr4] (ut/mk-id&str)]
  
  (db/insert! :prompts
              {:id id1
               :idstr idstr1
               :created (ut/now-ts)
               :updated (ut/now-ts)
               :deleted nil
               :prompt "Pricing Estimate"
               :descr "In what range would you expect this buyer's costs to fall?"})

  (db/insert! :prompt_fields
              {:id id2
               :idstr idstr2
               :created (ut/now-ts)
               :updated (ut/now-ts)
               :deleted nil
               :prompt_id id1
               :fname "Low"
               :descr nil
               :dtype :n
               :list_qm false
               :sort 0})

  (db/insert! :prompt_fields
              {:id id3
               :idstr idstr3
               :created (ut/now-ts)
               :updated (ut/now-ts)
               :deleted nil
               :prompt_id id1
               :fname "High"
               :descr nil
               :dtype :n
               :list_qm false
               :sort 1})

  (db/insert! :prompt_fields
              {:id id4
               :idstr idstr4
               :created (ut/now-ts)
               :updated (ut/now-ts)
               :deleted nil
               :prompt_id id1
               :fname "Unit"
               :descr nil
               :dtype :e-price-per
               :list_qm false
               :sort 2})

  (db/insert! :req_templates
              {:id id5
               :idstr idstr5
               :created (ut/now-ts)
               :updated (ut/now-ts)
               :deleted nil
               :title "Preposal Request"
               :rtype :preposal1
               :descr "Basic Preposal Request"})

  (db/insert! :req_template_prompt
              {:id id6
               :idstr idstr6
               :created (ut/now-ts)
               :updated (ut/now-ts)
               :deleted nil
               :req_template_id id5
               :prompt_id id1
               :sort 0}))



#_(let [[id idstr] (ut/mk-id&str)]
  (db/insert! :prompts
              {:id id
               :idstr idstr
               :created (ut/now-ts)
               :updated (ut/now-ts)
               :deleted nil
               :prompt "Pitch"
               :descr "Why do we believe you are a fit for this product?"}))

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
