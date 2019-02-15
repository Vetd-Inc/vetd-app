(ns com.vetd.app.migrations
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.migragen :as mig]            
            [taoensso.timbre :as log]))


(def mig-2019-02-04-copy-from-categories-up
  (mig/mk-copy-from-up-fn "categories.sql"))

(def mig-2019-02-04-copy-from-categories-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :categories
    :where [:between :id 272814110001 272814110725]}))

(def mig-2019-02-04-copy-from-orgs-up
  (mig/mk-copy-from-up-fn "orgs.sql"))

(def mig-2019-02-04-copy-from-orgs-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :orgs
    :where [:between :id 273818389861 272814405123]}))

(def mig-2019-02-04-copy-from-products-up
  (mig/mk-copy-from-up-fn "products.sql"))

(def mig-2019-02-04-copy-from-products-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :products
    :where [:between :id 272814695125 272814743922]}))

(def mig-2019-02-05-copy-from-product-categories-up
  (mig/mk-copy-from-up-fn "product-categories.sql"))

(def mig-2019-02-05-copy-from-product-categories-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :product_categories
    :where [:between :id 273266515726 273266499717]}))

(def mig-2019-02-10-copy-from-admins-up
  (mig/mk-copy-from-up-fn "mig-2019-02-10-admins.sql"))

(def mig-2019-02-10-copy-from-admins-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :admins
    :where [:= :id 354836657068]}))

(def mig-2019-02-10-copy-from-users-up
  (mig/mk-copy-from-up-fn "mig-2019-02-10-users.sql"))

(def mig-2019-02-10-copy-from-users-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :users
    :where [:= :id 354836657067]}))

(def mig-2019-02-12-form-templates-up
  (mig/mk-copy-from-up-fn "mig-2019-02-12-form-templates.sql"))

(def mig-2019-02-12-form-templates-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :form_templates
    :where [:= :id 370382503635]}))

(def mig-2019-02-12-form-template-prompt-up
  (mig/mk-copy-from-up-fn "mig-2019-02-12-form-template-prompt.sql"))

(def mig-2019-02-12-form-template-prompt-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :form_template_prompt
    :where [:between :id 370382503634 370382503636]}))

(def mig-2019-02-12-prompts-up
  (mig/mk-copy-from-up-fn "mig-2019-02-12-prompts.sql"))

(def mig-2019-02-12-prompts-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :prompts
    :where [:between :id 370382503629 370382503633]}))

(def mig-2019-02-12-prompt-fields-up
  (mig/mk-copy-from-up-fn "mig-2019-02-12-prompt-fields.sql"))

(def mig-2019-02-12-prompt-fields-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :prompt_fields
    :where [:between :id 370382503630 370382503634]}))


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
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]

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
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]

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
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]

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
                              :dtype [:text]
                              :dsubtype [:text]
                              :subject [:bigint]
                              :descr [:text]
                              :notes [:text]

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
                    :name :form_templates
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :title [:text]
                              ;; preposal, profile etc????
                              :ftype [:text]
                              :fsubtype [:text]
                              :descr [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :form_template_prompt
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :form_template_id [:bigint]
                              :prompt_id [:bigint]
                              :sort [:integer]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]
    
    [:create-table {:schema :vetd
                    :name :forms
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :form_template_id [:bigint]
                              :title [:text]
                              :subject [:bigint]                              
                              :descr [:text]                              
                              :notes [:text]
                              :ftype [:text]
                              :fsubtype [:text]
                              :from_org_id [:bigint]
                              :from_user_id [:bigint]
                              :to_org_id [:bigint]
                              :to_user_id [:bigint]
                              :status [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :form_prompt
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :form_id [:bigint]
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
                              :org_id [:bigint]
                              :notes [:text]}
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
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]

    [:create-view {:schema :vetd
                   :name :prompts_by_form
                   :honey {:select [[:rp.id :rpid]
                                    :rp.form_id
                                    :p.id
                                    :p.idstr                                    
                                    :p.created
                                    :p.updated
                                    :p.deleted
                                    :p.prompt
                                    :p.descr]
                           :from [[:form_prompt :rp]]
                           :join [[:prompts :p]
                                  [:= :p.id :rp.prompt_id]]}
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]]
   
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
                  :down-fn mig-2019-02-10-copy-from-admins-down}]]
   
   [[2019 2 11 00 00]
    [:create-view {:schema :vetd
                   :name :prompts_by_template
                   :honey {:select [[:rp.id :rpid]
                                    :rp.form_template_id
                                    :rp.sort
                                    :p.id
                                    :p.idstr                                    
                                    :p.created
                                    :p.updated
                                    :p.deleted
                                    :p.prompt
                                    :p.descr]
                           :from [[:form_template_prompt :rp]]
                           :join [[:prompts :p]
                                  [:= :p.id :rp.prompt_id]]}
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]]

   [[2019 2 12 00 00]
    [:copy-from '{:name :mig-2019-02-12-form-templates
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-12-form-templates-up
                  :down-fn mig-2019-02-12-form-templates-down}]

    [:copy-from '{:name :mig-2019-02-12-form-templates-prompt
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-12-form-template-prompt-up
                  :down-fn mig-2019-02-12-form-template-prompt-down}]
    
    [:copy-from '{:name :mig-2019-02-12-prompts
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-12-prompts-up
                  :down-fn mig-2019-02-12-prompts-down}]
    
    [:copy-from '{:name :mig-2019-02-12-prompt-fields
                  :ns com.vetd.app.migrations
                  :up-fn mig-2019-02-12-prompt-fields-up
                  :down-fn mig-2019-02-12-prompt-fields-down}]]])

#_(mig/mk-migration-files migrations
                          "migrations")

