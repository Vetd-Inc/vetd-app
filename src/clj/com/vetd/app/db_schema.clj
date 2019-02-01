(ns com.vetd.app.db-schema
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.path :as path]
            [clojure.walk :as w]))

(def tables2
  {:users {:columns {:id :bigint
                     :idstr :text
                     :email :text
                     :pwd :text
                     :uname :text}}
   :sessions {:columns {:id :serial
                        :token :text
                        :user_id :bigint}}
   :orgs {:columns {:id :bigint
                    :idstr :text
                    :oname :text
                    :buyer_qm :bool
                    :vendor_qm :bool
                    :short_desc :text
                    :url :text
                    :long_desc :text}}
   :products {:columns {:id :bigint
                        :idstr :text
                        :pname :text
                        :vendor_id :bigint
                        :short_desc :text
                        :long_desc :text}}
   :memberships {:columns {:id :bigint
                           :idstr :text
                           :org_id :bigint
                           :user_id :bigint}}
   :preposals {:columns {:id :bigint
                         :idstr :text
                         :req_id :bigint
                         :buyer_id :bigint
                         :product_id :bigint
                         :pitch :text}}
   :preposal_reqs {:columns {:id :bigint
                             :idstr :text
                             :buyer_id :bigint
                             :buyer_user_id :bigint
                             :product_id :bigint}}
   ;; TODO add completed timestamp to rounds
   :rounds {:columns {:id :bigint
                      :idstr :text
                      :buyer_id :bigint
                      :status :text}}
   :round_product {:columns {:id :bigint
                             :idstr :text
                             :round_id :bigint
                             :product_id :bigint}}
   :cart_items {:columns {:id :bigint
                          :idstr :text
                          :buyer_id :bigint
                          :product_id :bigint}}})


(defn convert-kw
  [kw]
  (let [n (name kw)]
    (-> n
        (clojure.string/replace #"_qm$" "?")
        (clojure.string/replace #"_" "-")
        keyword)))


(defn mk-sql-field->clj-kw [tbls]
  (let [fields (-> (for [[_ v] tbls
                      [field _] (:columns v)]
                  field)
                distinct)]
    (into {}
          (for [f fields]
            [f (convert-kw f)]))))


(def sql-field->clj-kw (mk-sql-field->clj-kw tables2))

(def clj-kw->sql-field (clojure.set/map-invert sql-field->clj-kw))

(defn walk-sql-field->clj-kw [v]
  (w/prewalk-replace sql-field->clj-kw v))

(defn walk-clj-kw->sql-field [v]
  (w/prewalk-replace clj-kw->sql-field v))




























