(ns com.vetd.app.env
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [environ.core :as env]))

(def vetd-env (env/env :vetd-env))

(def prod?
  (= vetd-env "PROD"))

(def building?
  (= vetd-env "BUILD"))

(defn build-safe-env
  [k]
  (if-not building?
    (get env/env k)
    ""))

;; DB
(def pg-db {:dbtype (build-safe-env :db-type)
            :dbname (build-safe-env :db-name)
            :host (build-safe-env :db-host)
            :port (Integer. (build-safe-env :db-port))
            :user (build-safe-env :db-user)
            :password (build-safe-env :db-password)})

;; Hasura
(def hasura-ws-url (build-safe-env :hasura-ws-url))
(def hasura-http-url (build-safe-env :hasura-http-url))

(com/setup-env prod?)
