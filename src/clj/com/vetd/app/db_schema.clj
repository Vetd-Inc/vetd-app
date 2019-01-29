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
                         :req_id :int
                         :buyer_id :int
                         :product_id :int
                         :pitch :text}}
   :preposal_reqs {:columns {:id :bigint
                             :idstr :text
                             :buyer_id :int
                             :buyer_user_id :int
                             :product_id :int}}
   ;; TODO add completed timestamp to rounds
   :rounds {:columns {:id :bigint
                      :idstr :text
                      :buyer_id :int
                      :status :text}}
   :round_product {:columns {:id :bigint
                             :idstr :text
                             :round_id :int
                             :product_id :int}}})


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


(def views {})

(def paths
  {[[:users :orgs] [:orgs :users]]
   {:path [:users/id :memberships/user-id
           :memberships/org-id :orgs/id]
    :cardinality :one->many
    #_ :where }

   [[:orgs :products] [:products :orgs]]
   {:path [:orgs/id :products/vendor-id]
    :cardinality :one->many}
   
   [[:preposals :buyers] [:orgs :preposals-in]]
   {:path [:preposals/buyer-id :orgs/id]
    :cardinality :many->one}

   [[:preposals :products] [:products :preposals]]
   {:path [:preposals/product-id :products/id]
    :cardinality :many->one}

   [[:preposal_reqs :preposals] [:preposals :preposal_reqs]]
   {:path [:preposal_reqs/id :preposals/req-id]
    :cardinality :many->one}

   [[:preposal_reqs :products] [:products :preposal-reqs]]
   {:path [:preposal_reqs/product-id :products/id]
    :cardinality :many->one}

   [[:preposal_reqs :buyers] [:orgs :preposal-reqs-out]]
   {:path [:preposal_reqs/buyer-id :orgs/id]
    :cardinality :many->one}
   
   [[:sessions :users] [:users :sessions]]
   {:path [:sessions/user-id :users/id]
    :cardinality :many->one}})


(defn schema->prototype
  [m]
  (assoc
   (ut/traverse-values
    #(case %
       :string "s"
       :int 0
       :double 0.0
       :bool true
       :timestamp 0
       :object {:_x 0})
    m)
   :id "schema"))







