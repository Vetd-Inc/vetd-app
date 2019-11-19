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

    [:alter-table {:schema :vetd, :name :links, :pk :id}]]

   [[2019 7 12 00 00]
    
    [:alter-table {:schema :vetd
                   :name :memberships
                   :columns
                   {:add {:status [:text]}}}]]

   [[2019 7 12 00 1]

    [:create-table {:schema :vetd
                    :name :groups
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :gname [:text]
                              :admin_org_id [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :group_org_memberships
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :group_id [:bigint]
                              :org_id [:bigint]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view {:schema :vetd
                              :name :groups_by_org
                              :honey {:select [[:gom.id :ref_id]
                                               [:gom.deleted :ref_deleted]
                                               :gom.org_id
                                               :g.id
                                               :g.idstr                                    
                                               :g.created
                                               :g.deleted
                                               :g.gname
                                               :g.admin_org_id]
                                      :from [[:group_org_memberships :gom]]
                                      :join [[:groups :g]
                                             [:= :g.id :gom.group_id]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view {:schema :vetd
                              :name :orgs_by_group
                              :honey {:select [[:gom.id :ref_id]
                                               [:gom.deleted :ref_deleted]
                                               :gom.group_id
                                               :o.id
                                               :o.idstr                                    
                                               :o.created
                                               :o.deleted
                                               :o.oname
                                               :o.buyer_qm 
                                               :o.vendor_qm
                                               :o.short_desc
                                               :o.long_desc
                                               :o.url
                                               :o.vendor_profile_doc_id]
                                      :from [[:group_org_memberships :gom]]
                                      :join [[:orgs :o]
                                             [:= :o.id :gom.group_id]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 7 12 00 2]

    [:create-table {:schema :vetd
                    :name :group_discounts
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :group_id [:bigint]
                              :product_id [:bigint]
                              :descr [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]]

   [[2019 7 12 00 3]

    [:create-or-replace-view {:schema :vetd
                              :name :orgs_by_group
                              :honey {:select [[:gom.id :ref_id]
                                               [:gom.deleted :ref_deleted]
                                               :gom.group_id
                                               :o.id
                                               :o.idstr                                    
                                               :o.created
                                               :o.deleted
                                               :o.oname
                                               :o.buyer_qm 
                                               :o.vendor_qm
                                               :o.short_desc
                                               :o.long_desc
                                               :o.url
                                               :o.vendor_profile_doc_id]
                                      :from [[:group_org_memberships :gom]]
                                      :join [[:orgs :o]
                                             [:= :o.id :gom.org_id]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]
    
    [:create-or-replace-view {:schema :vetd
                              :name :products_by_group_discount
                              :honey {:select [[:gd.id :ref_id]
                                               [:gd.deleted :ref_deleted]
                                               :gd.group_id
                                               :p.id
                                               :p.idstr                                    
                                               :p.created
                                               :p.deleted
                                               :p.pname
                                               :p.vendor_id
                                               :p.short_desc
                                               :p.long_desc
                                               :p.logo
                                               :p.url
                                               :p.profile_doc_id]
                                      :from [[:group_discounts :gd]]
                                      :join [[:products :p]
                                             [:= :p.id :gd.product_id]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view {:schema :vetd
                              :name :groups_by_product_discount
                              :honey {:select [[:gd.id :ref_id]
                                               [:gd.deleted :ref_deleted]
                                               :gd.product_id
                                               :g.id
                                               :g.idstr                                    
                                               :g.created
                                               :g.deleted
                                               :g.gname
                                               :g.admin_org_id]
                                      :from [[:group_discounts :gd]]
                                      :join [[:groups :g]
                                             [:= :g.id :gd.group_id]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 7 12 00 4]
    
    [:create-or-replace-view {:schema :vetd
                              :name :products_by_group_discount
                              :honey {:select [[:gd.id :ref_id]
                                               [:gd.deleted :ref_deleted]
                                               [:gd.descr :group_discount_descr]
                                               :gd.descr
                                               :gd.group_id
                                               :p.id
                                               :p.idstr                                    
                                               :p.created
                                               :p.deleted
                                               :p.pname
                                               :p.vendor_id
                                               :p.short_desc
                                               :p.long_desc
                                               :p.logo
                                               :p.url
                                               :p.profile_doc_id]
                                      :from [[:group_discounts :gd]]
                                      :join [[:products :p]
                                             [:= :p.id :gd.product_id]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view {:schema :vetd
                              :name :groups_by_product_discount
                              :honey {:select [[:gd.id :ref_id]
                                               [:gd.deleted :ref_deleted]
                                               [:gd.descr :group_discount_descr]
                                               :gd.product_id
                                               :g.id
                                               :g.idstr                                    
                                               :g.created
                                               :g.deleted
                                               :g.gname
                                               :g.admin_org_id]
                                      :from [[:group_discounts :gd]]
                                      :join [[:groups :g]
                                             [:= :g.id :gd.group_id]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 7 31 0 0]
    
    [:create-table {:schema :vetd
                    :name :stack_items
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :product_id [:bigint]
                              :buyer_id [:bigint]
                              :status [:text]
                              :price_amount [[:numeric 12 2]]
                              :price_period [:text]
                              :renewal_date [:timestamp :with :time :zone]
                              :renewal_reminder [:bool]
                              :rating [[:numeric 12 2]]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]]

   [[2019 8 1 0 0]
    
    [:create-or-replace-view {:schema :vetd
                              :name :agg_group_prod_rating
                              :honey {:select [[:gom.group_id :group_id]
                                               [:si.product_id :product_id]
                                               [:%count.si.id :count_stack_items]
                                               [:si.rating :rating]]
                                      :from [[:group_org_memberships :gom]]
                                      :join [[:stack_items :si]
                                             [:= :si.buyer_id :gom.org_id]]
                                      :group-by [:gom.group_id :si.product_id :si.rating]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]
   
   [[2019 8 2 00 4]
    
    [:create-or-replace-view {:schema :vetd
                              :name :docs_to_fields
                              :honey {:select [[:d.id :doc_id]
                                               [:d.dtype :doc_dtype]
                                               [:d.dsubtype :doc_dsubtype]
                                               [:d.title :doc_title]
                                               [:d.from_user_id :doc_from_user_id]
                                               [:d.to_org_id :doc_to_org_id]
                                               [:d.from_org_id :doc_from_org_id]
                                               [:d.to_user_id :doc_to_user_id]
                                               [:d.subject :doc_subject]
                                               [:d.result :doc_result]
                                               [:d.reason :doc_reason]
                                               [:r.subject :response_subject]
                                               [:r.subject_type :response_subject_type]
                                               [:rf.id :resp_field_id]
                                               [:rf.nval :resp_field_nval]
                                               [:rf.sval :resp_field_sval]
                                               [:rf.dval :resp_field_dval]
                                               [:rf.jval :resp_field_jval]
                                               [:rf.idx :resp_field_idx]
                                               [:p.id :prompt_id]
                                               [:p.prompt :prompt_prompt]
                                               [:p.term :prompt_term]
                                               [:pf.id :prompt_field_id]
                                               [:pf.fname :prompt_field_fname]
                                               [:pf.list_qm :prompt_field_list_qm]
                                               [:pf.ftype :prompt_field_ftype]
                                               [:pf.fsubtype :prompt_field_fsubtype]            
                                               [:pf.sort :prompt_field_sort]]
                                      :from [[:docs :d]]
                                      :join [[:doc_resp :dr] [:and [:= :d.id :dr.doc_id] [:= :dr.deleted nil]]
                                             [:responses :r] [:and [:= :dr.resp_id :r.id] [:= :r.deleted nil]]
                                             [:resp_fields :rf] [:and [:= :rf.resp_id :r.id] [:= :rf.deleted nil]]
                                             [:prompts :p] [:and [:= :r.prompt_id :p.id] [:= :p.deleted nil]]
                                             [:prompt_fields :pf] [:and [:= :pf.id :rf.pf_id] [:= :pf.deleted nil]]]
                                      :where [:= :d.deleted nil]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 8 12 00 00]
    
    [:alter-table {:schema :vetd
                   :name :products
                   :columns
                   {:add {:profile_score [[:numeric 12 2]]
                          :profile_score_updated [:timestamp :with :time :zone]}}}]

    [:create-or-replace-view {:schema :vetd
                              :name :docs_to_fields
                              :honey {:select [[:d.id :doc_id]
                                               [:d.dtype :doc_dtype]
                                               [:d.dsubtype :doc_dsubtype]
                                               [:d.title :doc_title]
                                               [:d.from_user_id :doc_from_user_id]
                                               [:d.to_org_id :doc_to_org_id]
                                               [:d.from_org_id :doc_from_org_id]
                                               [:d.to_user_id :doc_to_user_id]
                                               [:d.subject :doc_subject]
                                               [:d.result :doc_result]
                                               [:d.reason :doc_reason]
                                               [:d.created :doc_created]
                                               [:d.updated :doc_updated]
                                               [:r.subject :response_subject]
                                               [:r.subject_type :response_subject_type]
                                               [:r.created :response_created]
                                               [:r.updated :response_updated]
                                               [:rf.id :resp_field_id]
                                               [:rf.nval :resp_field_nval]
                                               [:rf.sval :resp_field_sval]
                                               [:rf.dval :resp_field_dval]
                                               [:rf.jval :resp_field_jval]
                                               [:rf.idx :resp_field_idx]
                                               [:rf.created :resp_field_created]
                                               [:rf.updated :resp_field_updated]
                                               [:p.id :prompt_id]
                                               [:p.prompt :prompt_prompt]
                                               [:p.term :prompt_term]
                                               [:p.created :prompt_created]
                                               [:p.updated :prompt_updated]
                                               [:pf.id :prompt_field_id]
                                               [:pf.fname :prompt_field_fname]
                                               [:pf.list_qm :prompt_field_list_qm]
                                               [:pf.ftype :prompt_field_ftype]
                                               [:pf.fsubtype :prompt_field_fsubtype]            
                                               [:pf.sort :prompt_field_sort]
                                               [:pf.created :prompt_field_created]
                                               [:pf.updated :prompt_field_updated]]
                                      :from [[:docs :d]]
                                      :join [[:doc_resp :dr] [:and [:= :d.id :dr.doc_id] [:= :dr.deleted nil]]
                                             [:responses :r] [:and [:= :dr.resp_id :r.id] [:= :r.deleted nil]]
                                             [:resp_fields :rf] [:and [:= :rf.resp_id :r.id] [:= :rf.deleted nil]]
                                             [:prompts :p] [:and [:= :r.prompt_id :p.id] [:= :p.deleted nil]]
                                             [:prompt_fields :pf] [:and [:= :pf.id :rf.pf_id] [:= :pf.deleted nil]]]
                                      :where [:= :d.deleted nil]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]
   
   [[2019 8 16 0 0]
    
    [:create-or-replace-view {:schema :vetd
                              :name :agg_group_prod_rating
                              :honey {:select [[:gom.group_id :group_id]
                                               [:si.product_id :product_id]
                                               [:%count.si.id :count_stack_items]
                                               [:si.rating :rating]]
                                      :from [[:group_org_memberships :gom]]
                                      :join [[:stack_items :si]
                                             [:and
                                              [:= :si.buyer_id :gom.org_id]
                                              [:= :si.deleted nil]]]
                                      :group-by [:gom.group_id :si.product_id :si.rating]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 8 19 0 0]
    
    [:create-or-replace-view {:schema :vetd
                              :name :agg_group_prod_price
                              :honey {:select [[:gom.group_id :group_id]
                                               [:si.product_id :product_id]
                                               [(honeysql.core/raw "percentile_disc (0.5) WITHIN GROUP (ORDER BY (CASE WHEN si.price_period = 'monthly' THEN (si.price_amount * 12) WHEN si.price_period = 'free' THEN 0 ELSE si.price_amount END))") :median_price]]
                                      :from [[:group_org_memberships :gom]]
                                      :join [[:stack_items :si]
                                             [:and
                                              [:= :si.buyer_id :gom.org_id]
                                              [:= :si.deleted nil]
                                              [:<> :si.price_period nil]]]
                                      :group-by [:gom.group_id :si.product_id]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 8 23 0 0]
    
    [:alter-table {:schema :vetd
                   :name :stack_items
                   :columns
                   {:add {:renewal_day_of_month [:integer]}}}]]

   [[2019 8 28 0 0]

    [:create-or-replace-view {:schema :vetd
                              :name :top_products_by_group
                              :honey {:select [[:gom.group_id :group_id]
                                               [:si.product_id :product_id]
                                               [:%count.si.id :count_stack_items]]
                                      :from [[:group_org_memberships :gom]]
                                      :join [[:stack_items :si]
                                             [:and
                                              [:= :si.buyer_id :gom.org_id]
                                              [:= :si.deleted nil]]]
                                      :group-by [:gom.group_id :si.product_id]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 9 3 0 0]
    [:create-or-replace-view {:schema :vetd
                              :name :docs_to_fields
                              :honey {:select [[:d.id :doc_id]
                                               [:d.dtype :doc_dtype]
                                               [:d.dsubtype :doc_dsubtype]
                                               [:d.title :doc_title]
                                               [:d.from_user_id :doc_from_user_id]
                                               [:d.to_org_id :doc_to_org_id]
                                               [:d.from_org_id :doc_from_org_id]
                                               [:d.to_user_id :doc_to_user_id]
                                               [:d.subject :doc_subject]
                                               [:d.result :doc_result]
                                               [:d.reason :doc_reason]
                                               [:d.created :doc_created]
                                               [:d.updated :doc_updated]
                                               [:r.id :response_id]
                                               [:r.subject :response_subject]
                                               [:r.subject_type :response_subject_type]
                                               [:r.created :response_created]
                                               [:r.updated :response_updated]
                                               [:rf.id :resp_field_id]
                                               [:rf.nval :resp_field_nval]
                                               [:rf.sval :resp_field_sval]
                                               [:rf.dval :resp_field_dval]
                                               [:rf.jval :resp_field_jval]
                                               [:rf.idx :resp_field_idx]
                                               [:rf.created :resp_field_created]
                                               [:rf.updated :resp_field_updated]
                                               [:p.id :prompt_id]
                                               [:p.prompt :prompt_prompt]
                                               [:p.term :prompt_term]
                                               [:p.created :prompt_created]
                                               [:p.updated :prompt_updated]
                                               [:pf.id :prompt_field_id]
                                               [:pf.fname :prompt_field_fname]
                                               [:pf.list_qm :prompt_field_list_qm]
                                               [:pf.ftype :prompt_field_ftype]
                                               [:pf.fsubtype :prompt_field_fsubtype]            
                                               [:pf.sort :prompt_field_sort]
                                               [:pf.created :prompt_field_created]
                                               [:pf.updated :prompt_field_updated]]
                                      :from [[:docs :d]]
                                      :join [[:doc_resp :dr] [:and [:= :d.id :dr.doc_id] [:= :dr.deleted nil]]
                                             [:responses :r] [:and [:= :dr.resp_id :r.id] [:= :r.deleted nil]]
                                             [:resp_fields :rf] [:and [:= :rf.resp_id :r.id] [:= :rf.deleted nil]]
                                             [:prompts :p] [:and [:= :r.prompt_id :p.id] [:= :p.deleted nil]]
                                             [:prompt_fields :pf] [:and [:= :pf.id :rf.pf_id] [:= :pf.deleted nil]]]
                                      :where [:= :d.deleted nil]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 9 18 00 00]

    [:alter-table {:schema :vetd
                   :name :group_discounts
                   :columns
                   {:add {:origin_id [:bigint]
                          ;; TODO this sould be removed as it's unused
                          :long_descr [:text]}}}]]
   
   [[2019 9 19 00 00]

    [:alter-table {:schema :vetd
                   :name :group_discounts
                   :columns
                   {:add {:redemption_descr [:text]}}}]]

   [[2019 9 30 0 0]
    
    [:create-or-replace-view {:schema :vetd
                              :name :products_by_group_discount
                              :honey {:select [[:gd.id :ref_id]
                                               [:gd.deleted :ref_deleted]
                                               [:gd.descr :group_discount_descr]
                                               [:gd.redemption_descr :group_discount_redemption_descr]
                                               :gd.descr
                                               :gd.redemption_descr
                                               :gd.group_id
                                               :p.id
                                               :p.idstr                                    
                                               :p.created
                                               :p.deleted
                                               :p.pname
                                               :p.vendor_id
                                               :p.short_desc
                                               :p.long_desc
                                               :p.logo
                                               :p.url
                                               :p.profile_doc_id]
                                      :from [[:group_discounts :gd]]
                                      :join [[:products :p]
                                             [:= :p.id :gd.product_id]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]

    [:create-or-replace-view {:schema :vetd
                              :name :groups_by_product_discount
                              :honey {:select [[:gd.id :ref_id]
                                               [:gd.deleted :ref_deleted]
                                               [:gd.descr :group_discount_descr]
                                               [:gd.redemption_descr :group_discount_redemption_descr]
                                               :gd.product_id
                                               :g.id
                                               :g.idstr                                    
                                               :g.created
                                               :g.deleted
                                               :g.gname
                                               :g.admin_org_id]
                                      :from [[:group_discounts :gd]]
                                      :join [[:groups :g]
                                             [:= :g.id :gd.group_id]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 10 3 0 0 ]
    
    [:create-index {:idx-name :idx_products_vendor_id
                    :schema :vetd
                    :table :products
                    :columns [:vendor_id]}]
    
    [:create-index {:idx-name :idx_group_discounts_group_id
                    :schema :vetd
                    :table :group_discounts
                    :columns [:group_id]}]
    
    [:create-index {:idx-name :idx_group_discounts_product_id
                    :schema :vetd
                    :table :group_discounts
                    :columns [:product_id]}]
    
    [:create-index {:idx-name :idx_product_categories_prod_id
                    :schema :vetd
                    :table :product_categories
                    :columns [:prod_id]}]]

   [[2019 10 7 0 0 ]

    [:create-table {:schema :vetd
                    :name :journal_entries
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :session_id [:bigint]
                              :jtype [:text]
                              :entry [:jsonb]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]]

   [[2019 10 8 9 15]

    [:create-table {:schema :vetd
                    :name :feed_events
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :journal_entry_id [:bigint]
                              :journal_entry_created [:timestamp :with :time :zone]
                              :org_id [:bigint]
                              :ftype [:text]
                              :data [:jsonb]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]]

   [[2019 10 11 11 00]

    [:create-table {:schema :vetd
                    :name :email_sent_log
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :user_id [:bigint]
                              :etype [:text]
                              :org_id [:bigint]
                              :data [:jsonb]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]]

   [[2019 10 15 1 0]

    [:create-table {:schema :vetd
                    :name :unsubscribes
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :user_id [:bigint]
                              :org_id [:bigint]
                              :etype [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]]

   [[2019 11 7 00 00]

    [:alter-table {:schema :vetd
                   :name :rounds
                   :columns ;; used primarily for storing prompt-ids to prefill initiation form
                   {:add {:initiation_form_prefill [:jsonb]}}}]]

   [[2019 11 13 0 0]

    ;; VIEW: Recent Rounds By Group
    ;; in-progress, or complete
    ;; ordered by created
    
    [:create-or-replace-view {:schema :vetd
                              :name :recent_rounds_by_group
                              :honey {:select [[:gom.group_id :group_id]
                                               [:r.id :round_id]]
                                      :from [[:group_org_memberships :gom]]
                                      :join [[:rounds :r]
                                             [:and
                                              [:= :r.buyer_id :gom.org_id]
                                              [:= :r.deleted nil]
                                              [:in :r.status (honeysql.core/raw "('in-progress', 'complete')")]]]
                                      :group-by [:gom.group_id :r.id]
                                      :order-by [[:r.created :desc]]}
                              :owner :vetd
                              :grants {:hasura [:SELECT]}}]]

   [[2019 11 18 0 0]
    
    [:create-index {:idx-name :idx_rounds_req_form_template_id
                    :schema :vetd
                    :table :rounds
                    :columns [:req_form_template_id]}]

    [:create-index {:idx-name :idx_doc_resp_deleted
                    :schema :vetd
                    :table :doc_resp
                    :columns [:deleted]}]

    [:create-index {:idx-name :idx_doc_resp_doc_id
                    :schema :vetd
                    :table :doc_resp
                    :columns [:doc_id]}]

    [:create-index {:idx-name :idx_doc_resp_resp_id
                    :schema :vetd
                    :table :doc_resp
                    :columns [:resp_id]}]

    [:create-index {:idx-name :idx_docs_dtype
                    :schema :vetd
                    :table :docs
                    :columns [:dtype]}]

    [:create-index {:idx-name :idx_docs_form_id
                    :schema :vetd
                    :table :docs
                    :columns [:form_id]}]

    [:create-index {:idx-name :idx_docs_subject
                    :schema :vetd
                    :table :docs
                    :columns [:subject]}]

    [:create-index {:idx-name :idx_feed_events_id
                    :schema :vetd
                    :table :feed_events
                    :columns [:id]}]

    [:create-index {:idx-name :idx_feed_events_created
                    :schema :vetd
                    :table :feed_events
                    :columns [:created]}]

    [:create-index {:idx-name :idx_feed_events_org_id
                    :schema :vetd
                    :table :feed_events
                    :columns [:org_id]}]

    [:create-index {:idx-name :idx_feed_events_ftype
                    :schema :vetd
                    :table :feed_events
                    :columns [:ftype]}]

    [:create-index {:idx-name :idx_form_prompt_form_id
                    :schema :vetd
                    :table :form_prompt
                    :columns [:form_id]}]

    [:create-index {:idx-name :idx_form_prompt_prompt_id
                    :schema :vetd
                    :table :form_prompt
                    :columns [:prompt_id]}]

    [:create-index {:idx-name :idx_form_prompt_sort
                    :schema :vetd
                    :table :form_prompt
                    :columns [:sort]}]

    [:create-index {:idx-name :idx_form_template_prompt_prompt_id
                    :schema :vetd
                    :table :form_template_prompt
                    :columns [:prompt_id]}]

    [:create-index {:idx-name :idx_form_template_prompt_form_template_id
                    :schema :vetd
                    :table :form_template_prompt
                    :columns [:form_template_id]}]

    [:create-index {:idx-name :idx_form_template_prompt_sort
                    :schema :vetd
                    :table :form_template_prompt
                    :columns [:sort]}]

    [:create-index {:idx-name :idx_form_templates_ftype
                    :schema :vetd
                    :table :form_templates
                    :columns [:ftype]}]

    [:create-index {:idx-name :idx_forms_form_template_id
                    :schema :vetd
                    :table :forms
                    :columns [:form_template_id]}]

    [:create-index {:idx-name :idx_forms_ftype
                    :schema :vetd
                    :table :forms
                    :columns [:ftype]}]

    [:create-index {:idx-name :idx_forms_to_org_id
                    :schema :vetd
                    :table :forms
                    :columns [:to_org_id]}]

    [:create-index {:idx-name :idx_forms_from_org_id
                    :schema :vetd
                    :table :forms
                    :columns [:from_org_id]}]

    [:create-index {:idx-name :idx_group_discounts_id
                    :schema :vetd
                    :table :group_discounts
                    :columns [:id]}]

    [:create-index {:idx-name :idx_group_discounts_origin_id
                    :schema :vetd
                    :table :group_discounts
                    :columns [:origin_id]}]

    [:create-index {:idx-name :idx_group_org_memberships_id
                    :schema :vetd
                    :table :group_org_memberships
                    :columns [:id]}]

    [:create-index {:idx-name :idx_group_org_memberships_group_id
                    :schema :vetd
                    :table :group_org_memberships
                    :columns [:group_id]}]

    [:create-index {:idx-name :idx_group_org_memberships_org_id
                    :schema :vetd
                    :table :group_org_memberships
                    :columns [:org_id]}]

    [:create-index {:idx-name :idx_groups_admin_org_id
                    :schema :vetd
                    :table :groups
                    :columns [:admin_org_id]}]

    [:create-index {:idx-name :idx_groups_id
                    :schema :vetd
                    :table :groups
                    :columns [:id]}]

    [:create-index {:idx-name :idx_journal_entries_id
                    :schema :vetd
                    :table :journal_entries
                    :columns [:id]}]

    [:create-index {:idx-name :idx_journal_entries_jtype
                    :schema :vetd
                    :table :journal_entries
                    :columns [:jtype]}]

    [:create-index {:idx-name :idx_links_key
                    :schema :vetd
                    :table :links
                    :columns [:key]}]

    [:create-index {:idx-name :idx_memberships_org_id
                    :schema :vetd
                    :table :memberships
                    :columns [:org_id]}]

    [:create-index {:idx-name :idx_memberships_user_id
                    :schema :vetd
                    :table :memberships
                    :columns [:user_id]}]

    [:create-index {:idx-name :idx_product_categories_cat_id
                    :schema :vetd
                    :table :product_categories
                    :columns [:cat_id]}]

    [:create-index {:idx-name :idx_products_idstr
                    :schema :vetd
                    :table :products
                    :columns [:idstr]}]

    [:create-index {:idx-name :idx_products_score
                    :schema :vetd
                    :table :products
                    :columns [:score]}]

    [:create-index {:idx-name :idx_products_profile_score
                    :schema :vetd
                    :table :products
                    :columns [:profile_score]}]

    [:create-index {:idx-name :idx_prompt_fields_ftype
                    :schema :vetd
                    :table :prompt_fields
                    :columns [:ftype]}]

    [:create-index {:idx-name :idx_prompt_fields_prompt_id
                    :schema :vetd
                    :table :prompt_fields
                    :columns [:prompt_id]}]

    [:create-index {:idx-name :idx_prompt_fields_sort
                    :schema :vetd
                    :table :prompt_fields
                    :columns [:sort]}]

    [:create-index {:idx-name :idx_prompts_term
                    :schema :vetd
                    :table :prompts
                    :columns [:term]}]

    [:create-index {:idx-name :idx_resp_fields_pf_id
                    :schema :vetd
                    :table :resp_fields
                    :columns [:pf_id]}]

    [:create-index {:idx-name :idx_resp_fields_resp_id
                    :schema :vetd
                    :table :resp_fields
                    :columns [:resp_id]}]

    [:create-index {:idx-name :idx_responses_prompt_id
                    :schema :vetd
                    :table :responses
                    :columns [:prompt_id]}]

    [:create-index {:idx-name :idx_responses_subject
                    :schema :vetd
                    :table :responses
                    :columns [:subject]}]

    [:create-index {:idx-name :idx_round_category_category_id
                    :schema :vetd
                    :table :round_category
                    :columns [:category_id]}]

    [:create-index {:idx-name :idx_round_category_round_id
                    :schema :vetd
                    :table :round_category
                    :columns [:round_id]}]

    [:create-index {:idx-name :idx_round_product_product_id
                    :schema :vetd
                    :table :round_product
                    :columns [:product_id]}]

    [:create-index {:idx-name :idx_round_product_round_id
                    :schema :vetd
                    :table :round_product
                    :columns [:round_id]}]

    [:create-index {:idx-name :idx_round_product_sort
                    :schema :vetd
                    :table :round_product
                    :columns [:sort]}]

    [:create-index {:idx-name :idx_rounds_buyer_id
                    :schema :vetd
                    :table :rounds
                    :columns [:buyer_id]}]

    [:create-index {:idx-name :idx_rounds_status
                    :schema :vetd
                    :table :rounds
                    :columns [:status]}]

    [:create-index {:idx-name :idx_rounds_created
                    :schema :vetd
                    :table :rounds
                    :columns [:created]}]

    [:create-index {:idx-name :idx_sessions_user_id
                    :schema :vetd
                    :table :sessions
                    :columns [:user_id]}]

    [:create-index {:idx-name :idx_sessions_token
                    :schema :vetd
                    :table :sessions
                    :columns [:token]}]

    [:create-index {:idx-name :idx_stack_items_buyer_id
                    :schema :vetd
                    :table :stack_items
                    :columns [:buyer_id]}]

    [:create-index {:idx-name :idx_stack_items_id
                    :schema :vetd
                    :table :stack_items
                    :columns [:id]}]

    [:create-index {:idx-name :idx_stack_items_product_id
                    :schema :vetd
                    :table :stack_items
                    :columns [:product_id]}]

    [:create-index {:idx-name :idx_stack_items_status
                    :schema :vetd
                    :table :stack_items
                    :columns [:status]}]

    [:create-index {:idx-name :idx_unsubscribes_etype
                    :schema :vetd
                    :table :unsubscribes
                    :columns [:etype]}]

    [:create-index {:idx-name :idx_unsubscribes_org_id
                    :schema :vetd
                    :table :unsubscribes
                    :columns [:org_id]}]

    [:create-index {:idx-name :idx_unsubscribes_user_id
                    :schema :vetd
                    :table :unsubscribes
                    :columns [:user_id]}]

    [:create-index {:idx-name :idx_users_email
                    :schema :vetd
                    :table :users
                    :columns [:email]}]]

   [[2019 11 19 0 0]

    [:create-table {:schema :vetd
                    :name :threads
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :group_id [:bigint]
                              :user_id [:bigint]
                              :org_id [:bigint]
                              :title [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-index {:idx-name :idx_threads_id
                    :schema :vetd
                    :table :threads
                    :columns [:id]}]

    [:create-index {:idx-name :idx_threads_group_id
                    :schema :vetd
                    :table :threads
                    :columns [:group_id]}]
    
    [:create-table {:schema :vetd
                    :name :messages
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :updated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :thread_id [:bigint]
                              :user_id [:bigint]
                              :org_id [:bigint]
                              :text [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-index {:idx-name :idx_messages_id
                    :schema :vetd
                    :table :messages
                    :columns [:id]}]
    
    [:create-index {:idx-name :idx_messages_thread_id
                    :schema :vetd
                    :table :messages
                    :columns [:thread_id]}]]   

   ])


#_(mig/mk-migration-files migrations
                          "migrations")
