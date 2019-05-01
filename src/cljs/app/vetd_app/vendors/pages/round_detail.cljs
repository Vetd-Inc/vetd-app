(ns vetd-app.vendors.pages.round-detail
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))


(rf/reg-event-fx
 :v/nav-round-detail
 (fn [_ [_ round-idstr]]
   {:nav {:path (str "/v/rounds/" round-idstr)}}))

(rf/reg-event-fx
 :v/route-round-detail
 (fn [{:keys [db]} [_ round-idstr]]
   {:db (assoc db
               :page :v/round-detail
               :page-params {:round-idstr round-idstr})}))


(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        prep-reqs& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:form-docs {:ftype "round-product-requirements"
                                                 :to-org-id @org-id&}
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
                                       [:id :idstr :prompt :descr #_:sort ;; TODO sort
                                        [:fields {:_order_by {:sort :asc}
                                                  :deleted nil}
                                         [:id :idstr :fname :ftype
                                          :fsubtype :list? #_:sort]]]]
                                      [:responses {:ref-deleted nil}
                                       [:id :prompt-id :notes
                                        [:fields 
                                         [:id :pf-id :idx :sval :nval :dval :jval]]]]]]]}])]
    (fn []
      (def preq1 @prep-reqs&)
      [:div
       (for [preq (:form-docs @prep-reqs&)]
         ^{:key (str "form" (:id preq))}
         [docs/c-form-maybe-doc
          (docs/mk-form-doc-state preq)
          {:show-submit true}])])))



