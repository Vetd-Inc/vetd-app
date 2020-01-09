(ns vetd-app.vendors.fixtures
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.fixtures :as cf]
            [re-frame.core :as rf]))

(defn container [body]
  (let [org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:form-docs {:ftype "preposal"
                                             :to-org-id @org-id&
                                             :_order_by {:created :desc}}
                                 [:id :idstr :title :ftype :fsubtype
                                  :doc-id :doc-title :created :updated
                                  [:product [:id :pname]]
                                  [:from-org [:id :oname]]
                                  [:from-user [:id :uname]]
                                  [:to-org [:id :oname]]
                                  [:to-user [:id :uname]]
                                  [:prompts {:_order_by {:sort :asc}
                                             :deleted nil
                                             :ref-deleted nil}
                                   [:id :idstr :prompt :descr :sort
                                    [:fields {:_order_by {:sort :asc}
                                              :deleted nil}
                                     [:id :idstr :fname :ftype
                                      :fsubtype :list? :sort]]]]
                                  [:responses {:ref-deleted nil}
                                   [:id :prompt-id :notes
                                    [:fields 
                                     [:id :pf-id :idx :sval :nval :dval :jval]]]]]]]}])]
    (fn [body]
      (let [num-pending-estimate-requests (some->> @preps&
                                                   :form-docs
                                                   (remove :doc-id)
                                                   count)]
        [:> ui/Container {:class "main-container"}
         [cf/c-top-nav [(merge {:text "Estimates"
                                :pages #{:v/preposals}
                                :event [:v/nav-preposals]}
                               (when (and num-pending-estimate-requests
                                          (not (zero? num-pending-estimate-requests)))
                                 {:badge {:color "teal"
                                          :content num-pending-estimate-requests}}))
                        {:text "Company Profile"
                         :pages #{:v/profile}
                         :event [:v/nav-profile]}
                        {:text "Your Products"
                         :pages #{:v/products}
                         :event [:v/nav-products]}
                        {:text "VetdRounds"
                         :pages #{:v/rounds}
                         :event [:v/nav-rounds]}]]
         body
         [:div {:style {:height 100}}]]))))
