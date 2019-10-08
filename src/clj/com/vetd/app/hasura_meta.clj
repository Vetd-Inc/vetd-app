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
              [:vetd :products_by_round] [[:vetd :products]]
              [:vetd :categories_by_product] [[:vetd :categories]]
              [:vetd :categories_by_round] [[:vetd :categories]]              
              [:vetd :responses_by_doc] [[:vetd :responses]]
              [:vetd :response_prompt_by_doc] [[:vetd :responses]]
              [:vetd :response_prompt] [[:vetd :responses]]
              [:vetd :form_docs] [[:vetd :forms]]
              [:vetd :groups_by_org] [[:vetd :groups]]
              [:vetd :orgs_by_group] [[:vetd :orgs]]
              [:vetd :groups_by_product_discount] [[:vetd :groups]]
              [:vetd :products_by_group_discount] [[:vetd :products]]}
   :rels [{:tables [:vetd :categories
                    :vetd :rounds_by_category]
           :fields [:rounds]
           :cols [:id :category_id]
           :rel :one-many}

          {:tables [:vetd :rounds
                    :vetd :categories_by_round]
           :fields [:categories]
           :cols [:id :round_id]
           :rel :one-many}

          {:tables [:vetd :rounds
                    :vetd :products_by_round]
           :fields [:products]
           :cols [:id :round_id]
           :rel :one-many}
          
          {:tables [:vetd :rounds
                    :vetd :orgs]
           :fields [:buyer :rounds-out]
           :cols [:buyer_id :id]
           :rel :many-one}

          {:tables [:vetd :rounds
                    :vetd :docs]
           :fields [:init-doc :rounds]
           :cols [:doc_id :id]
           :rel :one-one}

          {:tables [:vetd :rounds
                    :vetd :form_templates]
           :fields [:req-form-template :round]
           :cols [:req_form_template_id :id]
           :rel :one-one}

          {:tables [:vetd :round_product
                    :vetd :rounds]
           :fields [:rounds :round-product]
           :cols [:round_id :id]
           :rel :many-one}

          {:tables [:vetd :round_product
                    :vetd :products]
           :fields [:product :round-products]
           :cols [:product_id :id]
           :rel :many-one}

          {:tables [:vetd :round_product
                    :vetd :form_docs]
           :fields [:vendor-response-form-docs :round-product]
           :cols [:id :subject]
           :rel :many-many}
          
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

          {:tables [:vetd :orgs
                    :vetd :groups_by_org]
           :fields [:groups]
           :cols [:id :org_id]
           :rel :many-many}

          {:tables [:vetd :orgs
                    :vetd :groups]
           :fields [:admin-groups :admin-orgs]
           :cols [:id :admin_org_id]
           :rel :one-many}

          {:tables [:vetd :orgs
                    :vetd :group_org_memberships]
           :fields [:group-memberships :orgs]
           :cols [:id :org_id]
           :rel :one-many}
          
          {:tables [:vetd :groups
                    :vetd :orgs_by_group]
           :fields [:orgs]
           :cols [:id :group_id]
           :rel :many-many}

          {:tables [:vetd :groups
                    :vetd :products_by_group_discount]
           :fields [:discounts]
           :cols [:id :group_id]
           :rel :one-many}

          {:tables [:vetd :products
                    :vetd :groups_by_product_discount]
           :fields [:discounts]
           :cols [:id :product_id]
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
                    :vetd :response_prompt_by_doc]
           :fields [:response-prompts]
           :cols [:id :doc_id]
           :rel :many-many}
          
          {:tables [:vetd :form_docs
                    :vetd :responses_by_doc]
           :fields [:responses]
           :cols [:doc_id :doc_id]
           :rel :many-many}

          {:tables [:vetd :form_docs
                    :vetd :response_prompt_by_doc]
           :fields [:response-prompts]
           :cols [:doc_id :doc_id]
           :rel :many-many}
          
          {:tables [:vetd :form_docs
                    :vetd :products]
           :fields [:product :form-docs]
           :cols [:subject :id]
           :rel :many-one}

          {:tables [:vetd :form_docs
                    :vetd :products]
           :fields [:doc_product :form-docs]
           :cols [:doc_subject :id]
           :rel :many-one}

          {:tables [:vetd :form_docs
                    :vetd :orgs]
           :fields [:doc-from-org :form-docs-out]
           :cols [:doc_from_org_id :id]
           :rel :many-one}

          {:tables [:vetd :form_docs
                    :vetd :orgs]
           :fields [:doc-to-org :form-docs-in]
           :cols [:doc_to_org_id :id]
           :rel :many-one}
          
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
           :fields [:users :responses]
           :cols [:prompt_id :id]
           :rel :many-one}
          
          {:tables [:vetd :responses
                    :vetd :resp_fields]
           :fields [:fields :response]
           :cols [:id :resp_id]
           :rel :one-many}

          {:tables [:vetd :responses
                    :vetd :response_prompt_fields]
           :fields [:response-prompt-fields]
           :cols [:id :resp_id]
           :rel :one-many}

          {:tables [:vetd :responses
                    :vetd :response_prompt]
           :fields [:subject-of-response-prompt]
           :cols [:id :subject]
           :rel :one-many}

          {:tables [:vetd :response_prompt
                    :vetd :responses]
           :fields [:subject-response-prompt]
           :cols [:subject :id]
           :rel :many-one}
          
          {:tables [:vetd :resp_fields
                    :vetd :prompt_fields]
           :fields [:prompt-field :resp-field]
           :cols [:pf_id :id]
           :rel :many-one}

          {:tables [:vetd :form_templates
                    :vetd :prompts_by_template]
           :fields [:prompts]
           :cols [:id :form_template_id]
           :rel :one-many}

          {:tables [:vetd :enums
                    :vetd :enum_vals]
           :fields [:vals :enum]
           :cols [:id :enum_id]
           :rel :one-many}

          ;; dummy to make ha/sync-query work for links table
          {:tables [:vetd :links
                    :vetd :rounds_by_category]
           :fields [:dummy]
           :cols [:id :category_id]
           :rel :one-many}

          {:tables [:vetd :stack_items
                    :vetd :products]
           :fields [:product :stack-items]
           :cols [:product_id :id]
           :rel :many-one}

          {:tables [:vetd :stack_items
                    :vetd :orgs]
           :fields [:buyer :stack-items]
           :cols [:buyer_id :id]
           :rel :many-one}

          {:tables [:vetd :feed_events
                    :vetd :orgs]
           :fields [:org :feed-events]
           :cols [:org_id :id]
           :rel :many-one}

          {:tables [:vetd :products
                    :vetd :agg_group_prod_rating]
           :fields [:agg_group_prod_rating]
           :cols [:id :product_id]
           :rel :many-many}

          {:tables [:vetd :products
                    :vetd :agg_group_prod_price]
           :fields [:agg_group_prod_price]
           :cols [:id :product_id]
           :rel :many-many}

          {:tables [:vetd :groups
                    :vetd :top_products_by_group]
           :fields [:top-products]
           :cols [:id :group_id]
           :rel :one-many}]})


#_
(mig/proc-hasura-meta-cfg2 hasura-meta-cfg)
