(ns com.vetd.app.migragen
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [clojure.java.jdbc :as j]
            [taoensso.timbre :as log]
            [honeysql.core :as hny]
            [clojure.java.io :as io]
            clojure.edn))



(defmulti mk-sql (fn [[op]] op))

(defn drop-table-if-exists [schema table]
  (format "DROP TABLE IF EXISTS %s.%s;"
          schema
          table))

(defn drop-view-if-exists [schema view]
  (format "DROP VIEW IF EXISTS %s.%s;"
          schema
          view))

(defmethod mk-sql :create-table
  [[_ {:keys [schema columns owner grants] table :name}]]
  (let [schema' (name schema)
        table' (name table)
        owner' (name owner)]
    {:name-part (format "create-%s-table" table')
     :up (->> (concat [(drop-table-if-exists schema'
                                             table')
                       (j/create-table-ddl (format "%s.%s"
                                                   schema'
                                                   table')
                                           (for [[k v] columns]
                                             (into [k] v)))
                       (when owner
                         (format "ALTER TABLE %s.%s OWNER TO %s"
                                 schema'
                                 table'
                                 owner'))]
                      (for [[k vs] grants
                            v      vs]
                        (format "GRANT %s ON TABLE %s.%s TO %s;"
                                (name v)
                                schema'
                                table'
                                (name k))))
              (remove nil?))
     :down [(drop-table-if-exists schema'
                                  table')]}))

(defmethod mk-sql :create-view
  [[_ {:keys [schema honey owner grants] view :name}]]
  (let [schema' (name schema)
        view' (name view)
        owner' (name owner)]
    {:name-part (format "create-%s-view" view')
     :up (->> (concat [(drop-view-if-exists schema'
                                            view')
                       (format "CREATE VIEW %s.%s AS %s;"
                               schema'
                               view'
                               (first
                                (hny/format honey
                                            :allow-dashed-names? true
                                            :quoting :ansi)))
                       (when owner
                         (format "ALTER VIEW %s.%s OWNER TO %s"
                                 schema'
                                 view'
                                 owner'))])
              (remove nil?))
     :down [(drop-view-if-exists schema'
                                 view')]}))

(defn mk-migration-file-name
  [path idx [yr mo da hr mi] name-part up-down]
  (format "%s/%04d%02d%02d%02d%02d%02d-%s.%s.sql"
          path
          yr mo da hr mi idx
          name-part up-down))

(defn mk-migration-file-contents
  [sqls]
  (clojure.string/join "\n--;;\n"
                       sqls))

(defn mk-migration-file
  [path idx [dtime stmt]]
  (let [{:keys [name-part up down]} (mk-sql stmt)]
    (spit (mk-migration-file-name path idx dtime name-part "up")
          (mk-migration-file-contents up))
    (spit (mk-migration-file-name path idx dtime name-part "down")
          (mk-migration-file-contents down))))

(defn mk-migration-files
  [migrations-def dest-path]
  (map-indexed (partial mk-migration-file dest-path)
   (for [[dtime & stmts] migrations-def
         s stmts]
     [dtime s])))


#_ (mk-migration-files migrations
                       "/home/bill/tmp1")

#_ (mk-migration-files migrations
                       (-> "migrations"
                           io/resource
                           .getPath))

(def migrations
  [[[2019 2 4 02 16]
    [:create-table {:schema :vetd
                    :name :orgs
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :udpated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]
                              :oname [:text]
                              :buyer_qm [:boolean]
                              :vendor_qm [:boolean]
                              :short_desc [:text]
                              :long_desc [:text]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]
    [:create-table {:schema :vetd
                    :name :users
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :udpated [:timestamp :with :time :zone]
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
                              :udpated [:timestamp :with :time :zone]
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
                              :udpated [:timestamp :with :time :zone]
                              :deleted [:timestamp :with :time :zone]}
                    :owner :vetd
                    :grants {:hasura [:SELECT]}}]

    [:create-table {:schema :vetd
                    :name :product_categories
                    :columns {:id [:bigint :NOT :NULL]
                              :idstr [:text]
                              :created [:timestamp :with :time :zone]
                              :udpated [:timestamp :with :time :zone]
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
                              :udpated [:timestamp :with :time :zone]
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
                              :udpated [:timestamp :with :time :zone]
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
                              :udpated [:timestamp :with :time :zone]
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
                              :udpated [:timestamp :with :time :zone]
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
                   :owner :vetd}]]])
