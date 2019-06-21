(ns com.vetd.app.sendgrid
  (:require [clj-http.client :as client]))

(def sendgrid-api-key "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")
(def sendgrid-api-url "https://api.sendgrid.com/v3/")
(def sendgrid-default-from "info@vetd.com")
(def sendgrid-default-from-name "Vetd Team")
;; TODO add a default sendgrid template id
(def sendgrid-default-template-id "")

(def common-opts
  {:as :json
   :content-type :json
   :coerce :always
   :throw-exceptions false
   :headers {"Authorization" (str "Bearer " sendgrid-api-key)}})

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
         {:success false
          :resp {:error {:message "Unknown error."}}})))

;; Example usage of :substitutions key
;; {:%name% "Jerry Seinfield"
;;  :%planName% "Standard Plan"}
;; If, in your sendgrid template, you had e.g., "Hi %name%"
(defn- send-email
  [to from subject payload & {:keys [substitutions]}]
  (request "mail/send"
           (merge {:personalizations [{:to [{:email to}]
                                       :subject subject
                                       :substitutions substitutions}]
                   :from {:email from
                          :name sendgrid-default-from-name}}
                  payload)))

(defn send-text-email
  [to subject message
   & {:keys [from]
      :or {from sendgrid-default-from}}]
  (send-email to from subject
              {:content [{:type "text" :value message}]}))

(defn send-html-email
  [to subject message
   & {:keys [from]
      :or {from sendgrid-default-from}}]
  (send-email to from subject
              {:content [{:type "text/html" :value message}]}))

(defn send-template-email
  [to subject message
   & {:keys [from template-id substitutions]
      :or {from sendgrid-default-from
           template-id sendgrid-default-template-id}}]
  (send-email to from subject
              {:content [{:type "text/html" :value message}]
               :template_id template-id}
              :substitutions substitutions))
