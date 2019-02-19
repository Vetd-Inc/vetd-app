(ns com.vetd.app.hasura-meta
  (:require [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.migragen :as mig]))

(def hasura-meta-cfg
  {:remote_schemas []
   :query_templates []
   :inherits {[:vetd :prompts_by_form] [[:vetd :prompts]]
              [:vetd :prompts_by_template] [[:vetd :prompts]]
              [:vetd :rounds_by_category] [[:vetd :rounds]]
              [:vetd :rounds_by_product] [[:vetd :rounds]]
              [:vetd :categories_by_product] [[:vetd :categories]]
              [:vetd :responses_by_doc] [[:vetd :responses]]
              [:vetd :form_docs] [[:vetd :forms]]}
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

          {:tables [:vetd :form_docs
                    :vetd :responses_by_doc]
           :fields [:responses]
           :cols [:doc_id :doc_id]
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

          {:tables [:vetd :docs
                    :vetd :products]
           :fields [:product :docs]
           :cols [:subject :id]
           :rel :many-one}
          
          {:tables [:vetd :docs
                    :vetd :forms]
           :fields [:form :docs]
           :cols [:form_id :id]
           :rel :many-one}
          
          {:tables [:vetd :forms
                    :vetd :products]
           :fields [:product :forms]
           :cols [:subject :id]
           :rel :many-one}

          {:tables [:vetd :forms
                    :vetd :orgs]
           :fields [:from-org :forms-out]
           :cols [:from_org_id :id]
           :rel :many-one}

          {:tables [:vetd :forms
                    :vetd :orgs]
           :fields [:to-org :forms-in]
           :cols [:to_org_id :id]
           :rel :many-one}

          {:tables [:vetd :forms
                    :vetd :users]
           :fields [:from-user :forms-out]
           :cols [:from_user_id :id]
           :rel :many-one}

          {:tables [:vetd :forms
                    :vetd :users]
           :fields [:to-user :forms-in]
           :cols [:to_user_id :id]
           :rel :many-one}

          {:tables [:vetd :forms
                    :vetd :prompts_by_form]
           :fields [:prompts :forms]
           :cols [:id :form_id]
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
          
#_          {:tables [:vetd :responses
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

          {:tables [:vetd :form_templates
                    :vetd :prompts_by_template]
           :fields [:prompts]
           :cols [:id :form_templates]
           :rel :one-many}]})



#_
(mig/proc-hasura-meta-cfg2 hasura-meta-cfg)
