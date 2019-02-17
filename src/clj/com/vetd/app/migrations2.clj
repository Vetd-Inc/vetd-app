(ns com.vetd.app.migrations2
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.migragen2 :as mig]            
            [taoensso.timbre :as log]))


(def migrations
  [[:vetd :orgs
    [[2019 2 4 00 00]
     [:table&view {:columns {:id [:bigint :NOT :NULL]
                             :idstr [:text]
                             :created [:timestamp :with :time :zone]
                             :updated [:timestamp :with :time :zone]
                             :deleted [:timestamp :with :time :zone]
                             :oname [:text]
                             :buyer? [:boolean]
                             :vendor? [:boolean]
                             :short-desc [:text]
                             :long-desc [:text]
                             :url [:text]}
                   :view {:where [:= :deleted nil]}
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]

     [:copy-from {:up {:sym 'mig-orgs-2019-02-04-up
                       :file "orgs-2019-02-04.sql"}
                  :down
                  {:sym 'mig-orgs-2019-02-04-down
                   :honeysql
                   {:delete-from :orgs
                    :where [:between :id 273818389861 272814405123]}}}]]]

   [:vetd :products
    [[2019 2 4 00 00]
     [:table&view {:columns {:id [:bigint :NOT :NULL]
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
                   :view {:where [:= :deleted nil]}
                   :owner :vetd
                   :grants {:hasura [:SELECT]}}]

     [:copy-from {:up {:sym 'mig-2019-02-04-copy-from-products-up
                       :file "products-2019-02-04.sql"}
                  :down
                  {:sym 'mig-2019-02-04-copy-from-products-down
                   :honeysql
                   {:delete-from :products
                    :where [:between :id 272814695125 272814743922]}}}]]]])
