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

(def mig-enums-2019-02-19-up
  (mig/mk-copy-from-up-fn "mig-enums-2019-02-19.sql"))

(def mig-enums-2019-02-19-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :enums
    :where [:= :id 426702348930]}))

(def mig-enum-vals-2019-02-19-up
  (mig/mk-copy-from-up-fn "mig-enum-vals-2019-02-19.sql"))

(def mig-enum-vals-2019-02-19-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :enum_vals
    :where [:between :id 426719628931 426719728932]}))




(def mig-prompts-2019-03-06-up
  (mig/mk-copy-from-up-fn "mig-prompts-2019-03-06.sql"))

(def mig-prompts-2019-03-06-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :prompts
    :where [:between :id 558587521452 558587751456]}))

(def mig-prompt-fields-2019-03-06-up
  (mig/mk-copy-from-up-fn "mig-prompt-fields-2019-03-06.sql"))

(def mig-prompt-fields-2019-03-06-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :prompt_fields
    :where [:between :id 558587801457 558588011461]}))

(def mig-form-template-prompt-2019-03-06-up
  (mig/mk-copy-from-up-fn "mig-form-template-prompt-2019-03-06.sql"))

(def mig-form-template-prompt-2019-03-06-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :form_template_prompt
    :where [:between :id 558733711465 558810631469]}))

(def mig-enums-2019-03-06-up
  (mig/mk-copy-from-up-fn "mig-enums-2019-03-06.sql"))

(def mig-enums-2019-03-06-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :enum_vals
    :where [:= :id 558733581462]}))

(def mig-enum-vals-2019-03-06-up
  (mig/mk-copy-from-up-fn "mig-enum-vals-2019-03-06.sql"))

(def mig-enum-vals-2019-03-06-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :enum_vals
    :where [:between :id 558733621463 558733661464]}))



(def mig-prompts-2019-03-07-up
  (mig/mk-copy-from-up-fn "mig-prompts-2019-03-07.sql"))

(def mig-prompts-2019-03-07-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :prompts
    :where [:between :id 567926257446 567926257448]}))

(def mig-prompt-fields-2019-03-07-up
  (mig/mk-copy-from-up-fn "mig-prompt-fields-2019-03-07.sql"))

(def mig-prompt-fields-2019-03-07-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :prompt_fields
    :where [:between :id 567926257449 567926257451]}))

(def mig-form-template-prompt-2019-03-07-up
  (mig/mk-copy-from-up-fn "mig-form-template-prompt-2019-03-07.sql"))

(def mig-form-template-prompt-2019-03-07-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :form_template_prompt
    :where [:between :id 567926257452 567926257454]}))

(def mig-prompt-fields-2019-03-12-up
  (mig/mk-copy-from-up-fn "mig-prompt-fields-2019-03-12.sql"))

(def mig-prompt-fields-2019-03-12-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :prompt_fields
    :where [:= :id 610341390082]}))

(def mig-form-templates-2019-03-26-up
  (mig/mk-copy-from-up-fn "mig-form-template-2019-03-26.sql"))

(def mig-form-templates-2019-03-26-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :form_templates
    :where [:between :id 732891594222 732891754223]}))

(def mig-form-templates-2019-04-10-up
  (mig/mk-copy-from-up-fn "mig-form-template-2019-04-10.sql"))

(def mig-form-templates-2019-04-10-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :form_templates
    :where [:= :id 860340375214]}))

(def mig-prompts-2019-05-07-up
  (mig/mk-copy-from-up-fn "mig-prompts-2019-05-07.sql"))

(def mig-prompts-2019-05-07-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :prompts
    :where [:= :id 1093760230399]}))

(def mig-prompt-fields-2019-05-07-up
  (mig/mk-copy-from-up-fn "mig-prompt-fields-2019-05-07.sql"))

(def mig-prompt-fields-2019-05-07-down
  (mig/mk-exe-honeysql-fn
   {:delete-from :prompt_fields
    :where [:= :id 1093790890400]}))


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
                              :url [:text]
                              :vendor_profile_doc_id [:bigint]} ;; TODO
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
                              :url [:text]
                              :profile_doc_id [:bigint]} ;; TODO
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
                              :user_id [:bigint] ;; TODO initiating user
                              :status [:text]
                              :active_qm [:boolean]}
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
                              :form_id [:bigint]
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
                              :resp_id [:bigint]
                              :replaced_by_id [:bigint] ;; TODO
                              :user_id [:bigint]}       ;; TODO
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
                              :sort [:integer]
                              :replaced_by_id [:bigint] ;; TODO
                              :user_id [:bigint]}       ;; TODO
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
                              :descr [:text]
                              :term [:text]} ;; TODO
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
                              :ftype [:text]
                              :fsubtype [:text]
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
                              :subject [:bigint] ;; TODO matches originating doc
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
                              :fsubtype [:text]
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
                              :enum_id [:bigint]
                              :fsubtype [:text]
                              :value [:text]
                              :label [:text]}
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
                                    :r.notes]
                           :from [[:doc_resp :dr]]
                           :join [[:responses :r]
                                  [:= :r.id :dr.resp_id]]}
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]
    [:create-view {:schema :vetd
                   :name :prompts_by_form
                   :honey {:select [[:rp.id :rpid]
                                    :rp.form_id
                                    :rp.sort
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
                  :down-fn mig-2019-02-12-prompt-fields-down}]]

   [[2019 2 18 00 00]
    [:create-view {:schema :vetd
                   :name :form_docs
                   :honey {:select [:f.id
                                    :f.idstr
                                    :f.created
                                    :f.updated
                                    :f.form_template_id
                                    :f.title
                                    :f.subject
                                    :f.descr
                                    :f.notes
                                    :f.ftype
                                    :f.fsubtype
                                    :f.from_org_id
                                    :f.from_user_id
                                    :f.to_org_id
                                    :f.to_user_id
                                    :f.status
                                    [:d.id :doc_id]
                                    [:d.idstr :doc_idstr] 
                                    [:d.created :doc_created] 
                                    [:d.updated :doc_updated]
                                    [:d.title :doc_title] 
                                    [:d.subject :doc_subject] 
                                    [:d.descr :doc_descr] 
                                    [:d.notes :doc_notes]
                                    [:d.dtype :doc_dtype]
                                    [:d.dsubtype :doc_dsubtype]
                                    [:d.from_org_id :doc_from_org_id]
                                    [:d.from_user_id :doc_from_user_id]
                                    [:d.to_org_id :doc_to_org_id]
                                    [:d.to_user_id :doc_to_user_id]]
                           :from [[:forms :f]]
                           :left-join [[:docs :d]
                                       [:and
                                        [:= :d.form_id :f.id]
                                        [:= :d.deleted nil]]]
                           :where [:= :f.deleted nil]}
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]]

   [[2019 2 19 00 00]
    [:copy-from '{:name :mig-enums-2019-02-19
                  :ns com.vetd.app.migrations
                  :up-fn mig-enums-2019-02-19-up
                  :down-fn mig-enums-2019-02-19-down}]

    [:copy-from '{:name :mig-enum-vals-2019-02-19
                  :ns com.vetd.app.migrations
                  :up-fn mig-enum-vals-2019-02-19-up
                  :down-fn mig-enum-vals-2019-02-19-down}]]

   [[2019 3 6 00 00]
    [:copy-from '{:name :mig-prompts-2019-03-06
                  :ns com.vetd.app.migrations
                  :up-fn mig-prompts-2019-03-06-up
                  :down-fn mig-prompts-2019-03-06-down}]

    [:copy-from '{:name :mig-prompt-fields-2019-03-06
                  :ns com.vetd.app.migrations
                  :up-fn mig-prompt-fields-2019-03-06-up
                  :down-fn mig-prompt-fields-2019-03-06-down}]

    [:copy-from '{:name :mig-form-template-prompt-2019-03-06
                  :ns com.vetd.app.migrations
                  :up-fn mig-form-template-prompt-2019-03-06-up
                  :down-fn mig-form-template-prompt-2019-03-06-down}]

    [:copy-from '{:name :mig-enums-2019-03-06
                  :ns com.vetd.app.migrations
                  :up-fn mig-enums-2019-03-06-up
                  :down-fn mig-enums-2019-03-06-down}]

    [:copy-from '{:name :mig-enum-vals-2019-03-06
                  :ns com.vetd.app.migrations
                  :up-fn mig-enum-vals-2019-03-06-up
                  :down-fn mig-enum-vals-2019-03-06-down}]]

   [[2019 3 7 00 00]
    [:copy-from '{:name :mig-prompts-2019-03-07
                  :ns com.vetd.app.migrations
                  :up-fn mig-prompts-2019-03-07-up
                  :down-fn mig-prompts-2019-03-07-down}]

    [:copy-from '{:name :mig-prompt-fields-2019-03-07
                  :ns com.vetd.app.migrations
                  :up-fn mig-prompt-fields-2019-03-07-up
                  :down-fn mig-prompt-fields-2019-03-07-down}]

    [:copy-from '{:name :mig-form-template-prompt-2019-03-07
                  :ns com.vetd.app.migrations
                  :up-fn mig-form-template-prompt-2019-03-07-up
                  :down-fn mig-form-template-prompt-2019-03-07-down}]]

   [[2019 3 8 00 00]

    [:create-view {:schema :vetd
                   :name :categories_by_round
                   :honey {:select [[:rc.id :rcid]
                                    :rc.round_id
                                    :c.id
                                    :c.idstr                                    
                                    :c.created
                                    :c.cname]
                           :from [[:round_category :rc]]
                           :join [[:categories :c]
                                  [:= :c.id :rc.category_id]]}
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]

    [:create-view {:schema :vetd
                   :name :products_by_round
                   :honey {:select [[:rp.id :rpid]
                                    :rp.round_id
                                    :p.id
                                    :p.idstr                                    
                                    :p.created
                                    :p.pname
                                    :p.vendor_id
                                    :p.short_desc
                                    :p.long_desc
                                    :p.logo
                                    :p.url]
                           :from [[:round_product :rp]]
                           :join [[:products :p]
                                  [:= :p.id :rp.product_id]]}
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]]

   [[2019 3 12 00 00]

    [:copy-from '{:name :mig-prompt-fields-2019-03-12
                  :ns com.vetd.app.migrations
                  :up-fn mig-prompt-fields-2019-03-12-up
                  :down-fn mig-prompt-fields-2019-03-12-down}]]

   [[2019 3 25 00 00]
    [:create-or-replace-view
     {:schema :vetd
      :name :prompts_by_template
      :honey {:select [[:rp.id :rpid]
                       [:rp.deleted :rp_deleted]
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

   [[2019 3 26 00 00]

    [:copy-from '{:name :mig-form-templates-2019-03-26
                  :ns com.vetd.app.migrations
                  :up-fn mig-form-templates-2019-03-26-up
                  :down-fn mig-form-templates-2019-03-26-down}]]

   [[2019 4 5 00 00]
    [:create-or-replace-view
     {:schema :vetd
      :name :categories_by_product
      :honey {:select [[:pc.id :pcid]
                       [:pc.id :ref_id]
                       [:pc.deleted :ref_deleted]
                       :pc.prod_id
                       :c.id
                       :c.idstr
                       :c.cname
                       :c.deleted]
              :from [[:product_categories :pc]]
              :join [[:categories :c]
                     [:= :c.id :pc.cat_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :products_by_round
      :honey {:select [[:rp.id :rpid]
                       [:rp.id :ref_id]
                       [:rp.deleted :ref_deleted]
                       :rp.round_id
                       :p.id
                       :p.idstr                                    
                       :p.created
                       :p.pname
                       :p.vendor_id
                       :p.short_desc
                       :p.long_desc
                       :p.logo
                       :p.url
                       :p.deleted]
              :from [[:round_product :rp]]
              :join [[:products :p]
                     [:= :p.id :rp.product_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :categories_by_round
      :honey {:select [[:rc.id :rcid]
                       :rc.round_id
                       [:rc.id :ref_id]
                       [:rc.deleted :ref_deleted]
                       :c.id
                       :c.idstr                                    
                       :c.created
                       :c.cname
                       :c.deleted]
              :from [[:round_category :rc]]
              :join [[:categories :c]
                     [:= :c.id :rc.category_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}} ]

    [:create-or-replace-view
     {:schema :vetd
      :name :prompts_by_form
      :honey {:select [[:fp.id :rpid]
                       [:fp.id :ref_id]
                       [:fp.deleted :ref_deleted]
                       :fp.form_id
                       :fp.sort
                       :p.id
                       :p.idstr                                    
                       :p.created
                       :p.updated
                       :p.deleted
                       :p.prompt
                       :p.descr]
              :from [[:form_prompt :fp]]
              :join [[:prompts :p]
                     [:= :p.id :fp.prompt_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :responses_by_doc
      :honey {:select [[:dr.id :drid]
                       :dr.doc_id
                       [:dr.id :ref_id]
                       [:dr.deleted :ref_deleted]
                       :r.id
                       :r.idstr                                    
                       :r.created
                       :r.updated
                       :r.deleted
                       :r.prompt_id
                       :r.user_id
                       :r.notes]
              :from [[:doc_resp :dr]]
              :join [[:responses :r]
                     [:= :r.id :dr.resp_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}} ]

    [:create-or-replace-view
     {:schema :vetd
      :name :rounds_by_product
      :honey {:select [[:rp.id :rcid]
                       :rp.product_id
                       [:rp.id :ref_id]
                       [:rp.deleted :ref_deleted]
                       :r.id
                       :r.idstr                                    
                       :r.created
                       :r.deleted
                       :r.buyer_id
                       :r.status]
              :from [[:round_product :rp]]
              :join [[:rounds :r]
                     [:= :r.id :rp.round_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :rounds_by_category
      :honey {:select [[:rc.id :rcid]
                       :rc.category_id
                       [:rc.id :ref_id]
                       [:rc.deleted :ref_deleted]
                       :r.id
                       :r.idstr                                    
                       :r.created
                       :r.deleted
                       :r.buyer_id
                       :r.status]
              :from [[:round_category :rc]]
              :join [[:rounds :r]
                     [:= :r.id :rc.round_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :categories_by_product
      :honey {:select [[:pc.id :pcid]
                       :pc.prod_id
                       [:pc.id :ref_id]
                       [:pc.deleted :ref_deleted]
                       :c.id
                       :c.idstr
                       :c.cname
                       :c.deleted]
              :from [[:product_categories :pc]]
              :join [[:categories :c]
                     [:= :c.id :pc.cat_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :prompts_by_template
      :honey {:select [[:rp.id :rpid]
                       [:rp.deleted :rp_deleted]
                       [:rp.id :ref_id]
                       [:rp.deleted :ref_deleted]
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

   [[2019 4 10 00 00]

    [:copy-from '{:name :mig-form-templates-2019-04-10
                  :ns com.vetd.app.migrations
                  :up-fn mig-form-templates-2019-04-10-up
                  :down-fn mig-form-templates-2019-04-10-down}]

    [:create-or-replace-view
     {:schema :vetd
      :name :prompts_by_template
      :honey {:select [[:rp.id :rpid]
                       [:rp.deleted :rp_deleted]
                       [:rp.id :ref_id]
                       [:rp.deleted :ref_deleted]
                       :rp.form_template_id
                       :rp.sort
                       :p.id
                       :p.idstr                                    
                       :p.created
                       :p.updated
                       :p.deleted
                       :p.prompt
                       :p.term
                       :p.descr]
              :from [[:form_template_prompt :rp]]
              :join [[:prompts :p]
                     [:= :p.id :rp.prompt_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :prompts_by_form
      :honey {:select [[:fp.id :rpid]
                       [:fp.id :ref_id]
                       [:fp.deleted :ref_deleted]
                       :fp.form_id
                       :fp.sort
                       :p.id
                       :p.idstr                                    
                       :p.created
                       :p.updated
                       :p.deleted
                       :p.prompt
                       :p.term                       
                       :p.descr]
              :from [[:form_prompt :fp]]
              :join [[:prompts :p]
                     [:= :p.id :fp.prompt_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:alter-table {:schema :vetd
                   :name :rounds
                   :columns
                   {:add {:doc_id [:bigint]}}}]

    [:alter-table {:schema :vetd
                   :name :resp_fields
                   :columns
                   {:add {:jval [:jsonb]}}}]]

   [[2019 4 11 00 00]

    [:create-or-replace-view
     {:schema :vetd
      :name :response_prompt_by_doc
      :honey {:select [[:dr.id :drid]
                       [:dr.doc_id :doc_id]
                       [:dr.id :ref_id]
                       [:dr.deleted :ref_deleted]
                       :r.id
                       :r.idstr                                    
                       :r.created
                       :r.updated
                       :r.deleted
                       :r.prompt_id
                       :r.user_id
                       :r.notes
                       [:p.idstr :prompt_idstr]
                       [:p.created :prompt_created]
                       [:p.updated :prompt_updated]
                       [:p.deleted :prompt_deleted]
                       [:p.prompt :prompt_prompt]
                       [:p.term :prompt_term]                       
                       [:p.descr :prompt_descr]]
              :from [[:doc_resp :dr]]
              :join [[:responses :r]
                     [:= :r.id :dr.resp_id]
                     [:prompts :p]
                     [:= :p.id :r.prompt_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :response_prompt_fields
      :honey {:select [[:rf.id :ref_id]
                       [:rf.deleted :ref_deleted]
                       :rf.id
                       :rf.idstr                                    
                       :rf.created
                       :rf.updated
                       :rf.deleted
                       :rf.resp_id
                       :rf.pf_id
                       :rf.idx
                       :rf.sval
                       :rf.nval
                       :rf.dval
                       :rf.jval
                       [:pf.id :prompt_field_id]                       
                       [:pf.idstr :prompt_field_idstr]
                       [:pf.created :prompt_field_created]
                       [:pf.updated :prompt_field_updated]
                       [:pf.deleted :prompt_field_deleted]
                       [:pf.fname :prompt_field_fname]                       
                       [:pf.descr :prompt_field_descr]
                       [:pf.ftype :prompt_field_ftype]
                       [:pf.fsubtype :prompt_field_fsubtype]
                       [:pf.list_qm :prompt_field_list_qm]
                       [:pf.sort :prompt_field_sort]]
              :from [[:resp_fields :rf]]
              :join [[:prompt_fields :pf]
                     [:= :rf.pf_id :pf.id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-view {:schema :vetd
                   :name :form_docs
                   :honey {:select [:f.id
                                    :f.idstr
                                    :f.created
                                    :f.updated
                                    :f.form_template_id
                                    :f.title
                                    :f.subject
                                    :f.descr
                                    :f.notes
                                    :f.ftype
                                    :f.fsubtype
                                    :f.from_org_id
                                    :f.from_user_id
                                    :f.to_org_id
                                    :f.to_user_id
                                    :f.status
                                    [:d.id :doc_id]
                                    [:d.idstr :doc_idstr] 
                                    [:d.created :doc_created] 
                                    [:d.updated :doc_updated]
                                    [:d.deleted :doc_deleted]                                    
                                    [:d.title :doc_title] 
                                    [:d.subject :doc_subject] 
                                    [:d.descr :doc_descr] 
                                    [:d.notes :doc_notes]
                                    [:d.dtype :doc_dtype]
                                    [:d.dsubtype :doc_dsubtype]
                                    [:d.from_org_id :doc_from_org_id]
                                    [:d.from_user_id :doc_from_user_id]
                                    [:d.to_org_id :doc_to_org_id]
                                    [:d.to_user_id :doc_to_user_id]]
                           :from [[:forms :f]]
                           :left-join [[:docs :d]
                                       [:and
                                        [:= :d.form_id :f.id]
                                        [:= :d.deleted nil]]]
                           :where [:= :f.deleted nil]}
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]]

   [[2019 4 12 00 00]
    
    [:alter-table {:schema :vetd, :name :round_category, :pk :id}]
    [:alter-table {:schema :vetd, :name :schema_migrations, :pk :id}]
    [:alter-table {:schema :vetd, :name :orgs, :pk :id}]
    [:alter-table {:schema :vetd, :name :categories, :pk :id}]
    [:alter-table {:schema :vetd, :name :products, :pk :id}]
    [:alter-table {:schema :vetd, :name :sessions, :pk :id}]
    [:alter-table {:schema :vetd, :name :users, :pk :id}]
    [:alter-table {:schema :vetd, :name :product_categories, :pk :id}]
    [:alter-table {:schema :vetd, :name :memberships, :pk :id}]
    [:alter-table {:schema :vetd, :name :rounds, :pk :id}]
    [:alter-table {:schema :vetd, :name :round_product, :pk :id}]
    [:alter-table {:schema :vetd, :name :prompts, :pk :id}]
    [:alter-table {:schema :vetd, :name :resp_fields, :pk :id}]
    [:alter-table {:schema :vetd, :name :docs, :pk :id}]
    [:alter-table {:schema :vetd, :name :doc_resp, :pk :id}]
    [:alter-table {:schema :vetd, :name :form_templates, :pk :id}]
    [:alter-table {:schema :vetd, :name :prompt_fields, :pk :id}]
    [:alter-table {:schema :vetd, :name :form_template_prompt, :pk :id}]
    [:alter-table {:schema :vetd, :name :forms, :pk :id}]
    [:alter-table {:schema :vetd, :name :form_prompt, :pk :id}]
    [:alter-table {:schema :vetd, :name :responses, :pk :id}]
    [:alter-table {:schema :vetd, :name :enums, :pk :id}]
    [:alter-table {:schema :vetd, :name :enum_vals, :pk :id}]
    [:alter-table {:schema :vetd, :name :admins, :pk :id}]
    [:alter-table {:schema :vetd
                   :name :rounds
                   :columns
                   {:add {:title [:text]}}}]]

   [[2019 4 26 00 00]
    
    [:alter-table {:schema :vetd
                   :name :rounds
                   :columns
                   {:add {:req_form_template_id [:bigint]}}}]]

   [[2019 5 6 00 00]
    
    [:alter-table {:schema :vetd
                   :name :round_product
                   :columns
                   {:add {:result [:integer]
                          :reason [:text]}}}]]

   [[2019 5 7 00 00]

    [:alter-table {:schema :vetd
                   :name :responses
                   :columns
                   {:add {:subject_type [:text]}}}]

    [:copy-from '{:name :mig-prompts-2019-05-07
                  :ns com.vetd.app.migrations
                  :up-fn mig-prompts-2019-05-07-up
                  :down-fn mig-prompts-2019-05-07-down}]

    [:copy-from '{:name :mig-prompt-fields-2019-05-07
                  :ns com.vetd.app.migrations
                  :up-fn mig-prompt-fields-2019-05-07-up
                  :down-fn mig-prompt-fields-2019-05-07-down}]

    [:create-or-replace-view
     {:schema :vetd
      :name :response_prompt
      :honey {:select [:r.id
                       :r.idstr                                    
                       :r.created
                       :r.updated
                       :r.deleted
                       :r.prompt_id
                       :r.user_id
                       :r.notes
                       :r.subject
                       :r.subject_type
                       [:p.idstr :prompt_idstr]
                       [:p.created :prompt_created]
                       [:p.updated :prompt_updated]
                       [:p.deleted :prompt_deleted]
                       [:p.prompt :prompt_prompt]
                       [:p.term :prompt_term]                       
                       [:p.descr :prompt_descr]]
              :from [[:responses :r]]
              :join [[:prompts :p]
                     [:= :p.id :r.prompt_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]]

   [[2019 5 16 00 00]

    [:alter-table {:schema :vetd
                   :name :round_product
                   :columns
                   {:add {:sort [:integer]}}}]]
   
   [[2019 6 5 00 00]

    [:alter-table {:schema :vetd
                   :name :products
                   :columns
                   {:add {:score [:numeric]}}}]

    [:raw-script {:schema :vetd
                  :name :update-product-scores-2019-06-05}]]

   [[2019 6 6 00 00]
    
    [:alter-table {:schema :vetd
                   :name :docs
                   :columns
                   {:add {:result [:integer]
                          :reason [:text]}}}]]

   [[2019 6 18 00 00]
    
    [:create-or-replace-view
     {:schema :vetd
      :name :rounds_by_category
      :honey {:select [[:rc.id :rcid]
                       :rc.category_id
                       [:rc.id :ref_id]
                       [:rc.deleted :ref_deleted]
                       :r.id
                       :r.idstr                                    
                       :r.created
                       :r.deleted
                       :r.buyer_id
                       :r.status
                       :r.req_form_template_id
                       :r.doc_id]
              :from [[:round_category :rc]]
              :join [[:rounds :r]
                     [:= :r.id :rc.round_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :response_prompt_by_doc
      :honey {:select [[:dr.id :drid]
                       [:dr.doc_id :doc_id]
                       [:dr.id :ref_id]
                       [:dr.deleted :ref_deleted]
                       :r.id
                       :r.idstr                                    
                       :r.created
                       :r.updated
                       :r.deleted
                       :r.prompt_id
                       :r.user_id
                       :r.notes
                       [:p.idstr :prompt_idstr]
                       [:p.created :prompt_created]
                       [:p.updated :prompt_updated]
                       [:p.deleted :prompt_deleted]
                       [:p.prompt :prompt_prompt]
                       [:p.term :prompt_term]                       
                       [:p.descr :prompt_descr]]
              :from [[:doc_resp :dr]]
              :join [[:responses :r]
                     [:= :r.id :dr.resp_id]
                     [:prompts :p]
                     [:= :p.id :r.prompt_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :responses_by_doc
      :honey {:select [[:dr.id :drid]
                       :dr.doc_id
                       [:dr.id :ref_id]
                       [:dr.deleted :ref_deleted]
                       :r.id
                       :r.idstr                                    
                       :r.created
                       :r.updated
                       :r.deleted
                       :r.prompt_id
                       :r.user_id
                       :r.notes
                       :r.subject]
              :from [[:doc_resp :dr]]
              :join [[:responses :r]
                     [:= :r.id :dr.resp_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view
     {:schema :vetd
      :name :rounds_by_product
      :honey {:select [[:rp.id :rcid]
                       :rp.product_id
                       [:rp.id :ref_id]
                       [:rp.deleted :ref_deleted]
                       :r.id
                       :r.idstr                                    
                       :r.created
                       :r.deleted
                       :r.buyer_id
                       :r.status
                       :r.req_form_template_id
                       :r.doc_id]
              :from [[:round_product :rp]]
              :join [[:rounds :r]
                     [:= :r.id :rp.round_id]]}
      :owner :vetd
      :grants {:hasura [:SELECT]}}]]

   [[2019 6 25 00 00]
    
    [:create-table {:schema :vetd
                    :name :links
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :key [:text] ; secure key to allow execution and read (will be part of link URI)
                              :cmd [:text] ; command to execute upon link visit
                              :input_data [:text] ; an EDN-parseable string defining args to pass to cmd
                              :output_data [:text] ; this is populated with the results of cmd(input_data) as an EDN-parseable string
                              :expires_action [:timestamp :with :time :zone] ; time when actionability expires
                              :expires_read [:timestamp :with :time :zone] ; time when readability expires (often dynamic over life of link)
                              :max_uses_action [:integer] ; maximum times the link can be actioned
                              :max_uses_read [:integer] ; maximum times output_data can be read
                              :uses_action [:integer] ; the number of times the link was visited
                              :uses_read [:integer]} ; the number of times the output_data was read
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:alter-table {:schema :vetd, :name :links, :pk :id}]]])

#_(mig/mk-migration-files migrations
                          "migrations")
