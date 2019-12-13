(ns vetd-app.vendors.pages.preposals
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :v/nav-preposals
 (constantly
  {:nav {:path "/v/estimates"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Vendor Preposals"}}}))

(rf/reg-event-fx
 :v/route-preposals
 (fn [{:keys [db]}]
   {:db (assoc db :page :v/preposals)}))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:form-docs {:ftype "preposal"
                                             :to-org-id @org-id&
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
         (for [form-doc (:form-docs @preps&)]
           ^{:key (str "form" (:id form-doc))}
           [:> ui/GridRow
            [:> ui/GridColumn {:computer 4 :mobile 16}]
            [:> ui/GridColumn {:computer 8 :mobile 16}
             [:> ui/Segment {:class "detail-container"}
              [:h2 (str "Estimate for "(:oname (:from-org form-doc)))]
              [:p "Product: " (:pname (:product form-doc))]
              [:p "Status: " (if (:doc-id form-doc) "Submitted (you can still make changes)" "Never Submitted")]
              [docs/c-form-maybe-doc
               (docs/mk-form-doc-state form-doc)
               {:show-submit true}]]]
            [:> ui/GridColumn {:computer 4 :mobile 16}]])]))))
