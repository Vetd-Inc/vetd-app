(ns com.vetd.app.forward
  (:require [com.vetd.app.email-client :as ec]))

(defn forward
  [message]
  (ec/send-template-email
   "forward@vetd.com"
   {:message message}
   {:template-id "d-c49de401cdbb48009ec1955f810dcc41"}))
