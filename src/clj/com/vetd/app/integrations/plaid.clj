(ns com.vetd.app.integrations.plaid
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [com.vetd.app.auth :as auth]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [clojure.data.csv :as csv]
            [clojure.string :as s])
  (:import com.plaid.client.PlaidClient
	         com.plaid.client.PlaidClient$Builder
	         com.plaid.client.request.ItemPublicTokenExchangeRequest
	         com.plaid.client.response.ItemPublicTokenExchangeResponse
	         com.plaid.client.request.AccountsGetRequest
	         com.plaid.client.response.AccountsGetResponse
           com.plaid.client.response.Account
           com.plaid.client.response.Account$Balances
           com.plaid.client.request.ItemGetRequest
           com.plaid.client.response.ItemGetResponse
           com.plaid.client.response.ItemStatus
           com.plaid.client.request.InstitutionsGetByIdRequest
           com.plaid.client.response.InstitutionsGetByIdResponse
           com.plaid.client.response.Institution
           com.plaid.client.request.TransactionsGetRequest
           com.plaid.client.response.TransactionsGetResponse
           java.util.Date))

(defonce client& (atom nil))

(def plaid-client-id "5d7a6fce2dd19f001492b4fd")
(def plaid-public-key "90987208d894ddc82268098f566e9b")
(def plaid-secret "c997b042798933339ec830000bf3a7")

(defn build-client*
  [id secret public-key]
  (.. (PlaidClient/newBuilder)
      (clientIdAndSecret id secret)
      (publicKey public-key)
      sandboxBaseUrl ;; TODO production url!!!!!!!!!
      build))

(defn build-client
  []
  (try
    (if-let [client @client&]
      client
      (reset! client&
              (build-client* plaid-client-id
                             plaid-secret
                             plaid-public-key)))
    (catch Throwable e
      (com/log-error e))))

(defn exchange-token 
	[p-token]
	(.. (build-client)
	    service
	    (itemPublicTokenExchange (ItemPublicTokenExchangeRequest. p-token))
      execute
	    body
      getAccessToken))

(defn access-token->transactions* [access-token]
  (.. (build-client)
      service
      (transactionsGet
       (TransactionsGetRequest. access-token
                                (Date. (- (System/currentTimeMillis)
                                          (* 1000 60 60 24 100)))
                                (Date.)))
      execute
      body
      getTransactions))

(defn mk-transactions [tran-obj]
  {:name (.getName tran-obj)
   :date (.getDate tran-obj)
   :amount (.getAmount tran-obj)})

(defn access-token->transactions [access-token]
  (try
    (for [t (access-token->transactions* access-token)]
      (mk-transactions t))
    (catch Throwable e
      (com/log-error e))))

(defn mk-filename-base
  [oname suffix]
  (-> oname
      s/trim
      (s/replace #"[^a-zA-Z0-9]" "_")
      (str "__" suffix)))

(defn file-timestamp []
  (-> (System/currentTimeMillis)
      (quot 1000)
      (mod (* 60 60 24 365 10))))

(defn get-s3-plaid-filename [oname suffix extension]
  (str (mk-filename-base oname suffix) "__" (file-timestamp) "." extension))

(defn put-creds->s3 [oname creds]
  (try (com/s3-put "vetd-bank-creds"
                   (get-s3-plaid-filename oname "creds" "txt")
                   creds)
       (catch Throwable e
         (com/log-error e))))

(defn put-transactions->s3 [oname transactions]
  (try (com/s3-put "vetd-plaid-transaction-data"
                   (get-s3-plaid-filename oname "data" "csv")
                   (with-out-str
                     (csv/write-csv *out*
                                    (map (juxt :name :date :amount)
                                         transactions))))
       (catch Throwable e
         (com/log-error e))))

(defmethod com/handle-ws-inbound :b/stack.store-plaid-token
  [{:keys [buyer-id public-token]} ws-id sub-fn]
  (try (let [access-token (exchange-token public-token)
             oname (-> buyer-id auth/select-org-by-id :oname)]
         (do (put-creds->s3 oname access-token)
             (put-transactions->s3 oname (access-token->transactions access-token))))
       (catch Throwable e
         (com/log-error e))))
