(ns vetd-app.pages.vendors.v-home
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :v/nav-home
 (fn [{:keys [db]} _]
   {:nav {:path "/v/home/"}}))

(rf/reg-event-db
 :v/route-home
 (fn [db [_ query-params]]
   (assoc db
          :page :v/home
          :query-params query-params)))

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
                                       [:id :idstr :prompt :descr
                                        [:fields
                                         [:id :idstr :fname :ftype :fsubtype :list?]]]]
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
