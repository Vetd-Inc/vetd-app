(ns com.vetd.app.threads
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.journal :as journal]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.auth :as auth]
            [com.vetd.app.rounds :as rounds]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.docs :as docs]
            [taoensso.timbre :as log]
            [clj-time.coerce :as tc]
            [clojure.string :as s]))

;; (defn insert-stack-item
;;   [{:keys [product-id buyer-id status price-amount price-period
;;            renewal-date renewal-day-of-month renewal-reminder rating]}]
;;   (let [[id idstr] (ut/mk-id&str)]
;;     (db/insert! :stack_items
;;                 {:id id
;;                  :idstr idstr
;;                  :created (ut/now-ts)
;;                  :updated (ut/now-ts)
;;                  :product_id product-id
;;                  :buyer_id buyer-id
;;                  :status status
;;                  :price_amount price-amount
;;                  :price_period price-period
;;                  :renewal_date renewal-date
;;                  :renewal_day_of_month renewal-day-of-month
;;                  :renewal_reminder renewal-reminder
;;                  :rating rating})
;;     id))

(defmethod com/handle-ws-inbound :g/threads.create
  [{:keys [title message group-id user-id org-id]} ws-id sub-fn]
  (com/sns-publish
   :ui-misc
   "Discussion Thread Created"
   (str "Discussion Thread Created\n\n"
        "Group ID: " group-id
        "\nTitle: " title
        "\nMessage: " message
        "\nAuthor's Org: " (-> org-id auth/select-org-by-id :oname))
   {:org-id org-id})
  {})
