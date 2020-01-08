(ns vetd-app.vendors.pages.preposal-detail
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))


(rf/reg-event-fx
 :v/nav-preposal-detail
 (fn [_ [_ preposal-idstr]]
   {:nav {:path (str "/v/estimates/" preposal-idstr)}}))

(rf/reg-event-fx
 :v/route-preposal-detail
 (fn [{:keys [db]} [_ preposal-idstr]]
   {:db (assoc db
               :page :v/preposal-detail
               :page-params {:preposal-idstr preposal-idstr})}))

(rf/reg-sub
 :preposal-idstr
 :<- [:page-params]
 :preposal-idstr)

(defn c-page []
  (let [preposal-idstr& (rf/subscribe [:preposal-idstr])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:form-docs {:ftype "preposal"
                                             :id (util/base31->num @preposal-idstr&)
                                             ;; :to-org-id @org-id&
                                             :_order_by {:created :desc}}
                                 [:id :title :ftype :fsubtype
                                  :doc-id :doc-title
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
    (fn []
      (if (= :loading @preps&)
        [cc/c-loader]
        [:> ui/Grid {:stackable true}
         (let [form-doc (first (:form-docs @preps&))]
           [:> ui/GridRow
            [:> ui/GridColumn {:computer 4 :mobile 16}]
            [:> ui/GridColumn {:computer 8 :mobile 16}
             [bc/c-back-button]
             [:> ui/Segment {:class "detail-container"}
              [:h3 "Estimate Request - " (:oname (:from-org form-doc))]
              [:p "Requested By: " (:uname (:from-user form-doc)) " at " (:oname (:from-org form-doc))]
              [:p "Product: " (:pname (:product form-doc))]
              [:p "Status: " (if (:doc-id form-doc) "Submitted (you can still make changes)" "Never Submitted")]
              [docs/c-form-maybe-doc
               (docs/mk-form-doc-state form-doc)
               {:show-submit true}]]]
            [:> ui/GridColumn {:computer 4 :mobile 16}]])]))))
