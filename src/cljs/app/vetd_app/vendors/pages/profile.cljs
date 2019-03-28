(ns vetd-app.vendors.pages.profile
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :v/nav-profile
 (constantly
  {:nav {:path "/v/profile"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Vendor Edit Profile"}}}))

(rf/reg-event-fx
 :v/route-profile
 (fn [{:keys [db]}]
   {:db (assoc db :page :v/profile)
    :analytics/page {:name "Vendor Edit Profile"}}))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        profile& (rf/subscribe [:gql/sub
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
      (if (= :loading @profile&)
        [cc/c-loader]
        [:<>
         (for [profile (:form-docs @profile&)]
           ^{:key (str "form" (:id profile))}
           [docs/c-form-maybe-doc (docs/mk-form-doc-state profile)])]))))
