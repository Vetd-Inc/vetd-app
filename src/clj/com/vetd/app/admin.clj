(ns com.vetd.app.admin
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.docs :as docs]
            [taoensso.timbre :as log]))

(defn search-orgs->ids
  [q]
  (if (not-empty q)
    (let [ids (db/hs-query {:select [[:o.id :oid]]
                            :from [[:orgs :o]]
                            :where [(keyword "~*") :o.oname (str ".*?" q ".*")]
                            :limit 30})
          vids (->> ids
                    (map :oid)
                    distinct)]
      {:org-ids vids})
      {:org-ids []}))


(defmethod com/handle-ws-inbound :a/search
  [{:keys [query]} ws-id sub-fn]
  (search-orgs->ids query))

(defmethod com/handle-ws-inbound :a/create-preposal-req
  [{:keys [prep-req]} ws-id sub-fn]
  (docs/create-preposal-req-form prep-req))

(defmethod com/handle-ws-inbound :a/create-form-template-prompt
  [{:keys [form-template-id]} ws-id sub-fn]
  (docs/create-blank-form-template-prompt form-template-id))

(defmethod com/handle-ws-inbound :a/dissoc-template-prompt
  [{:keys [form-template-prompt-id]} ws-id sub-fn]
  (docs/delete-template-prompt form-template-prompt-id))
