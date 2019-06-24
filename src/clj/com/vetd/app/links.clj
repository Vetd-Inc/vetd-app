(ns com.vetd.app.links
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.util :as ut]))

;; Links have a command (cmd) and data, as well as metadata defining its validity.
;; They also have a key.
;; Some commands:
;;   - create-verified-account
;;   - reset-password
;;   - accept-invitation
;; Possible data (respective):
;;   - an account map
;;   - user id
;;   - map with org-id and role
;; Metadata:
;;   - max-uses-action (default = 1)
;;   - max-uses-read (default = 1)
;;   - expires-action (default = current time + 7 days) accepts unixtime
;;   - expires-read (default = unixtime 0, usually reset to future time upon action) accepts unixtime
;;   - uses-action (default = 0)
;;   - uses-read (default = 0)

(defn insert-link
  [{:keys [cmd data max-uses-action max-uses-read
           expires-action expires-read] :as link}]
  (let [[id idstr] (ut/mk-id&str)]
    {:id id
     :idstr idstr
     :key (ut/mk-strong-key)
     :cmd cmd
     :data (str data)
     :max_uses_action (or max-uses-action 1)
     :max_uses_read (or max-uses-read 1)
     :expires_action (java.sql.Timestamp.
                      (or expires-action
                          (+ (ut/now) (* 1000 60 60 24 7)))) ; 7 days from now
     :expires_read (java.sql.Timestamp.
                    (or expires-read 0))
     :uses_action 0
     :uses_read 0
     :created (ut/now-ts)
     :updated (ut/now-ts)}
    #_(-> (db/insert! :links
                      {:id id
                       :idstr idstr
                       :key (ut/mk-strong-key)
                       :cmd cmd
                       :data (str data)
                       :max_uses_action (or max-uses-action 1)
                       :max_uses_read (or max-uses-read 1)
                       :expires_action (java.sql.Timestamp.
                                        (or expires-action
                                            (+ (ut/now) (* 1000 60 60 24 7)))) ; 7 days from now
                       :expires_read (java.sql.Timestamp.
                                      (or expires-read 0))
                       :uses_action 0
                       :uses_read 0
                       :created (ut/now-ts)
                       :updated (ut/now-ts)})
          first)))


(insert-link {:cmd :create-verified-account
              :data {:uname "johnn boy"
                     :email "jj@gmail.com"
                     :org-name "Vetd Inc"}
              :max-uses-action 2})
{:max_uses_action 2,
 :expires_read #inst "1970-01-01T00:00:00.000000000-00:00",
 :key "f3ebyb4vrqsgvngrponxxwwk",
 :uses_read 0,
 :updated #inst "2019-06-24T19:39:29.021000000-00:00",
 :created #inst "2019-06-24T19:39:29.021000000-00:00",
 :idstr "c26604x7l",
 :id 1510436905348,
 :uses_action 0,
 :expires_action #inst "2019-07-01T19:39:29.021000000-00:00",
 :max_uses_read 1,
 :cmd :create-verified-account,
 :data "{:uname \"johnn boy\", :email \"jj@gmail.com\", :org-name \"Vetd Inc\"}"}

(defn sync-round-vendor-req-forms
  [round-id]
  (let [{:keys [buyer-id req-form-template] :as round} (-> [[:rounds {:id round-id}
                                                             [:buyer-id
                                                              [:req-form-template
                                                               [:id]]]]]
                                                           ha/sync-query
                                                           :rounds
                                                           first)
        form-template-id (:id req-form-template)
        rps (-> [[:round-product {:round-id round-id}
                  [:id :product-id :deleted
                   [:vendor-response-form-docs
                    [:id :doc-id]]
                   [:product
                    [:vendor-id]]]]]
                ha/sync-query
                :round-product)
        prod-id->exists (->> rps
                             (group-by :product-id)
                             (ut/fmap (partial some
                                               (comp nil? :deleted))))
        to-add (filter (partial sync-round-vendor-req-forms-to-add
                                prod-id->exists)
                       rps)
        to-remove (filter (partial sync-round-vendor-req-forms-to-remove
                                   prod-id->exists)
                          rps)]
    (doseq [{:keys [id vendor-id product]} to-add]
      (docs/create-form-from-template {:form-template-id form-template-id
                                       :from-org-id buyer-id
                                       :to-org-id (:vendor-id product)
                                       :subject id
                                       :title (format "Round Req Form -- round %d / prod %d "
                                                      round-id
                                                      vendor-id)}))
    (doseq [{:keys [id] forms :vendor-response-form-docs :as r} to-remove]
      (docs/update-deleted :round_product id)
      (doseq [{form-id :id doc-id :doc-id} forms]
        (when doc-id
          (docs/update-deleted :docs doc-id))
        (docs/update-deleted :forms form-id)))
    [to-add to-remove]))
