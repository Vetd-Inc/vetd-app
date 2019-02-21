(ns com.vetd.app.env
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [environ.core :as env]))

(defn get-vetd-env []
  (env/env :vetd-env)) 

(def prod?
  (-> env/env
      :vetd-env
      (= "PROD")))

(def building?
  (-> env/env
      :vetd-env
      (= "BUILD")))

(def pg-db-prod {:dbtype "postgresql"
                 :dbname "vetd1"
                 :host "vetd-db.chwslkxwld9a.us-east-1.rds.amazonaws.com"
                 :port 5432
                 :user "vetd" #_"hasura" 
                 ;; change pwd and move to prod env
                 :password "Wyl2bap2?"  #_"Hasura1"})

(def pg-db-dev {:dbtype "postgresql"
                :dbname "vetd1"
                :host "localhost"
                :port 5432
                :user "vetd"
                :password "vetd"})

(def pg-db
  (if prod?
    pg-db-prod
    pg-db-dev))


(def hasura-ws-url
  (if prod?
    "ws://172.31.0.60:8080/v1alpha1/graphql"
    "ws://localhost:8080/v1alpha1/graphql"))

(def hasura-http-url
  (if prod?
    "http://172.31.0.60:8080/v1alpha1/graphql"
    "http://localhost:8080/v1alpha1/graphql"))

(com/setup-env prod?)
