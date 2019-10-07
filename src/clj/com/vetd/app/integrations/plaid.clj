(ns com.vetd.app.integrations.plaid
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [com.vetd.app.auth :as auth]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db]
            [clojure.string :as string])
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
  [id secret key]
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
  [email suffix]
  (-> email
      (string/trim)
      (string/replace #"[^a-zA-Z0-9]" "_")
      (str "__" suffix)))

(defn file-timestamp []
  (-> (System/currentTimeMillis)
      (quot 1000)
      (mod (* 60 60 24 365 10))))

(defn get-s3-plaid-filename [email suffix]
  (str (mk-filename-base email suffix)
       (str "__" (file-timestamp) ".csv")))

(defn put-transactions->s3 [email transactions]
  (try
    (com/s3-put "vetd-plaid-data"
                (get-s3-plaid-filename email
                                       "data"))
    (catch Throwable e
      (com/log-error e))))

(defn put-creds->s3 [email transactions]
  (try
    (com/s3-put "vetd-plaid-creds"
                (get-s3-plaid-filename email
                                       "creds"))
    (catch Throwable e
      (com/log-error e))))

(defn handle-request [{:keys [params] :as req}]
  (try
    (let [{:keys [session-token]
           public-key :public_token} params
          access-token (exchange-token public-key)]
      (when-let [{:keys [email]} (auth/select-user-by-active-session-token session-token)]
        (do
          (put-creds->s3 email
                         access-token)
          (put-transactions->s3 email
                                (access-token->transactions access-token)))))
    (catch Throwable e
      (com/log-error e))))
