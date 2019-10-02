(ns com.vetd.app.integrations.plaid
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [com.vetd.app.env :as env]
            [com.vetd.app.db :as db])
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
  ( .. (PlaidClient/newBuilder)
   (clientIdAndSecret id secret)
   (publicKey public-key)
   sandboxBaseUrl ;; TODO production url!!!!!!!!!
   build))

(defn build-client
  [id, secret, key]
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
	( .. client
	     service
	     (itemPublicTokenExchange (ItemPublicTokenExchangeRequest. p-token))
             execute
	     body
         getAccessToken))

(defn get-transactions-obj [access-token]
  (.. client
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

(defn public-token->transactions [public-token]
  (try
    (for [t (-> public-token
                exchange-token
                get-transactions-obj)]
      (mk-transactions t))
    (catch Throwable e
      (com/log-error e))))

(defn get-s3-plaid-data-filename [])

(defn put-transactions->s3 [transactions]
  (com/s3-put "vetd-plaid-data"
              ))

(defn get&save-transactions [{:keys [params] :as req}]
  (try
    (-> params
        :public_token
        public-token->transactions
        put-transactions->s3)
    (catch Throwable e
      (com/log-error e))))
