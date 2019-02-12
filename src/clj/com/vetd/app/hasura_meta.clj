(ns com.vetd.app.hasura-meta
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.migragen :as mig]))


(def hasura-meta-cfg
  {:remote_schemas []
   :query_templates []
   :rels [{:tables [:vetd :categories
                    :vetd :rounds_by_category]
           :fields [:rounds]
           :cols [:id :category_id]
           :rel :one-many}

          {:tables [:vetd :orgs
                    :vetd :memberships]
           :fields [:memberships :org]
           :cols [:id :org_id]
           :rel :one-many}

          {:tables [:vetd :orgs
                    :vetd :products]
           :fields [:products :vendor]
           :cols [:id :vendor_id]
           :rel :one-many}

          {:tables [:vetd :products
                    :vetd :rounds_by_product]
           :fields [:rounds]
           :cols [:id :product_id]
           :rel :many-many}

          {:tables [:vetd :products
                    :vetd :categories_by_product]
           :fields [:categories]
           :cols [:id :prod_id]
           :rel :many-many}

          {:tables [:vetd :users
                    :vetd :memberships]
           :fields [:memberships :user]
           :cols [:id :user_id]
           :rel :one-many}

          {:tables [:vetd :users
                    :vetd :sessions]
           :fields [:sessions :user]
           :cols [:id :user_id]
           :rel :one-many}

          {:tables [:vetd :docs
                    :vetd :responses_by_doc]
           :fields [:responses]
           :cols [:id :doc_id]
           :rel :many-many}

          {:tables [:vetd :docs
                    :vetd :orgs]
           :fields [:from-org :docs-out]
           :cols [:from_org_id :id]
           :rel :many-one}

          {:tables [:vetd :docs
                    :vetd :orgs]
           :fields [:to-org :docs-in]
           :cols [:to_org_id :id]
           :rel :many-one}

          {:tables [:vetd :docs
                    :vetd :users]
           :fields [:from-user :docs-out]
           :cols [:from_user_id :id]
           :rel :many-one}

          {:tables [:vetd :docs
                    :vetd :users]
           :fields [:to-user :docs-in]
           :cols [:to_user_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :orgs]
           :fields [:from-org :reqs-out]
           :cols [:from_org_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :orgs]
           :fields [:to-org :reqs-in]
           :cols [:to_org_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :users]
           :fields [:from-user :reqs-out]
           :cols [:from_user_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :users]
           :fields [:to-user :reqs-in]
           :cols [:to_user_id :id]
           :rel :many-one}

          {:tables [:vetd :reqs
                    :vetd :prompts_by_req]
           :fields [:prompts]
           :cols [:id :req_id]
           :rel :one-many}

          {:tables [:vetd :prompts
                    :vetd :prompt_fields]
           :fields [:fields :prompt]
           :cols [:id :prompt_id]
           :rel :one-many}

          {:tables [:vetd :responses
                    :vetd :prompts]
           :fields [:prompt :responses]
           :cols [:prompt_id :id]
           :rel :many-one}

          {:tables [:vetd :responses
                    :vetd :users]
           :fields [:prompt :users]
           :cols [:prompt_id :id]
           :rel :many-one}

          {:tables [:vetd :responses
                    :vetd :prompts]
           :fields [:prompt :responses]
           :cols [:prompt_id :id]
           :rel :many-one}

          {:tables [:vetd :responses
                    :vetd :users]
           :fields [:user :responses]
           :cols [:user_id :id]
           :rel :many-one}
          
          {:tables [:vetd :responses
                    :vetd :orgs]
           :fields [:org :responses]
           :cols [:org_id :id]
           :rel :many-one}

          {:tables [:vetd :responses
                    :vetd :resp_fields]
           :fields [:fields :response]
           :cols [:id :resp_id]
           :rel :one-many}

          {:tables [:vetd :resp_fields
                    :vetd :prompt_fields]
           :fields [:prompt-field :resp-field]
           :cols [:pf_id :id]
           :rel :many-one}

          {:tables [:vetd :req_templates
                    :vetd :prompts_by_template]
           :fields [:prompts]
           :cols [:id :req_template_id]
           :rel :one-many}]})



#_
(mig/proc-hasura-meta-cfg2 hasura-meta-cfg)
