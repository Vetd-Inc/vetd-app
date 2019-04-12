(ns vetd-app.vendors.pages.preposals
  (:require [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :v/nav-preposals
 (constantly
  {:nav {:path "/v/preposals"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Vendor Preposals"}}}))

(rf/reg-event-fx
 :v/route-preposals
 (fn [{:keys [db]}]
   {:db (assoc db :page :v/preposals)
    :analytics/page {:name "Vendor Preposals"}}))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        prep-reqs& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:form-docs {:ftype "preposal"
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
