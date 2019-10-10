(ns com.vetd.app.env
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [environ.core :as env]
            [clj-postgresql.core :as pg]))

(def vetd-env (env/env :vetd-env))

(def all-env env/env)

(def prod?
  (= vetd-env "PROD"))

(def building?
  (= vetd-env "BUILD"))

(defmacro build-ignore
  "Ignore body when building."
  [& body]
  `(when-not building?
     ~@body))

(build-ignore 
 (def pg-db
   (pg/spec
    :dbname (env/env :db-name)
    :host (env/env :db-host)
    :port (Integer. (env/env :db-port))
    :user (env/env :db-user)
    :password (env/env :db-password)))

 ;; Hasura
 (def hasura-ws-url (env/env :hasura-ws-url))
 (def hasura-http-url (env/env :hasura-http-url))

 ;; Plaid
 (def plaid-client-id (env/env :plaid-client-id))
 (def plaid-public-key (env/env :plaid-public-key))
 (def plaid-secret (env/env :plaid-secret))
 )

(com/setup-env prod?)

;; Frontend env
(defmacro segment-frontend-write-key []
  (env/env :segment-frontend-write-key))
