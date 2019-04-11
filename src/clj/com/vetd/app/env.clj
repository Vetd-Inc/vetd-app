(ns com.vetd.app.env
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [environ.core :as env]
            [clj-postgresql.core :as pg]))

(def vetd-env (env/env :vetd-env))

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
 (def hasura-http-url (env/env :hasura-http-url)))

(com/setup-env prod?)

;; Frontend env
(defmacro segment-frontend-write-key []
  (env/env :segment-frontend-write-key))
