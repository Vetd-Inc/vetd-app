(ns com.vetd.app.email-client
  (:require [com.vetd.app.util :as ut]
            [com.vetd.app.common :as com]
            [com.vetd.app.db :as db]
            [clj-http.client :as client]))

(def sendgrid-api-key "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")
(def sendgrid-api-url "https://api.sendgrid.com/v3/")
(def sendgrid-default-from "info@vetd.com")
(def sendgrid-default-from-name "Vetd Team")
(def sendgrid-default-template-id "d-0942e461a64943018bd0d1d6cf711e21") ; "Simple" template

(def common-opts
  {:as :json
   :content-type :json
   :coerce :always
   :throw-exceptions false
   :headers {"Authorization" (str "Bearer " sendgrid-api-key)}})

;; NOTE when :success is true, :resp is nil.
(defn- request
  [endpoint & [params headers]]
  (try (let [resp (-> (client/post (str sendgrid-api-url endpoint)
                                   (merge-with merge 
                                               common-opts
                                               {:form-params params}
                                               {:headers headers}))
                      :body)]
         {:success (not (seq (:errors resp)))
          :resp resp})
       (catch Exception e
         (com/log-error e)
         {:success false
          :resp {:error {:message (.getMessage e)}}})))

(defn send-template-email
  [to data & [{:keys [from from-name template-id]}]]
  (request "mail/send"
           {:personalizations [{:to [{:email to}]
                                :dynamic_template_data data}]
            :from {:email (or from sendgrid-default-from)
                   :name (or from-name sendgrid-default-from-name)}
            :template_id (or template-id sendgrid-default-template-id)}))

;;;; Example Usage
#_(send-template-email
   "chris@vetd.com"
   {:subject "Vetd Buying Platform"
    :preheader "You're going to want to see what's in this email"
    :main-content "Here is some example content."})

(defn unsubscribe
  "Unsubscribe a user from an email type."
  [{:keys [user-id org-id etype]}]
  (let [[id idstr] (ut/mk-id&str)]
    (db/insert! :unsubscribes
                {:id id
                 :idstr idstr
                 :created (ut/now-ts)
                 :updated (ut/now-ts)
                 :deleted nil
                 :user_id user-id
                 :org_id org-id
                 :etype etype})
    id))
