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
                  :down-fn mig-2019-02-04-copy-from-products-down}]]])


#_(mig/mk-migration-files migrations
                        "migrations")
