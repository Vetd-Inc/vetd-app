(ns com.vetd.app.env
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [environ.core :as env]))

(defn get-vetd-env []
  (env/env :vetd-env)) 

(def prod?
  (= (get-vetd-env) "PROD"))

(def building?
  (= (get-vetd-env) "BUILD"))

;; DB
(def pg-db {:dbtype (env/env :db-type)
            :dbname (env/env :db-name)
            :host (env/env :db-host)
            :port (Integer. (env/env :db-port))
            :user (env/env :db-user)
            :password (env/env :db-password)})

;; Hasura
(def hasura-ws-url (env/env :hasura-ws-url))
(def hasura-http-url (env/env :hasura-http-url))

(com/setup-env prod?)
