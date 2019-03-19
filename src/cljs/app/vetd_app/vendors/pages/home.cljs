(ns vetd-app.vendors.pages.home
  (:require [vetd-app.flexer :as flx]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :v/nav-home
 (constantly
  {:nav {:path "/v/home/"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Vendor Home"}}}))

(rf/reg-event-fx
 :v/route-home
 (fn [{:keys [db]}]
   {:db (assoc db :page :v/home)
    :analytics/page {:name "Vendor Home"}}))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        prep-reqs& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:form-docs {:ftype "preposal"
                                                 :to-org-id @org-id&}
                                     [:id :title
                                      :doc-id :doc-title
                                      [:product [:id :pname]]
                                      [:from-org [:id :oname]]
                                      [:from-user [:id :uname]]
                                      [:to-org [:id :oname]]
                                      [:to-user [:id :uname]]
                                      [:prompts
                                       [:id :idstr :prompt :descr #_:sort ;; TODO sort
                                        [:fields
                                         [:id :idstr :fname :ftype
                                          :fsubtype :list? #_:sort]]]]
                                      [:responses
                                       [:id :prompt-id :notes
                                        [:fields [:id :pf-id :idx :sval :nval :dval]]]]]]]}])]
    (fn []
      (def preq1 @prep-reqs&)
      [:div
       (for [preq (:form-docs @prep-reqs&)]
         ^{:key (str "form" (:id preq))}
         [docs/c-form-maybe-doc (docs/mk-form-doc-state preq)])])))

#_ (cljs.pprint/pprint preq1)

#_
(cljs.pprint/pprint @(rf/subscribe [:gql/q
                                    {:queries
                                     [[:form-docs {:ftype "preposal"
                                                   :to-org-id @(rf/subscribe [:org-id])}
                                       [:id :title
                                        :doc-id :doc-title
                                        [:product [:id :pname]]
                                        [:from-org [:id :oname]]
                                        [:from-user [:id :uname]]
                                        [:to-org [:id :oname]]
                                        [:to-user [:id :uname]]
                                        [:prompts
                                         [:id :prompt :descr
                                          [:fields
                                           [:id :fname :ftype :fsubtype :list?]]]]
                                        [:responses
                                         [:id :prompt-id :notes
                                          [:fields [:id :idx :sval :nval :dval]]]]]]]}]))
