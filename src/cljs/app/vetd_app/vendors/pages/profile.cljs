(ns vetd-app.vendors.pages.profile
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]))

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
   {:db (assoc db :page :v/profile)}))

(defn c-page []
  (let [_ (println "its getting called")
        org-id& (rf/subscribe [:org-id])
        existing-profile& (rf/subscribe [:gql/sub
                                         {:queries
                                          [[:form-docs {:ftype "vendor-profile"
                                                        :doc-from-org-id @org-id&
                                                        :_order_by {:created :desc}
                                                        :_limit 1}
                                            [:id :title :doc-id :doc-title
                                             :ftype :fsubtype
                                             [:doc-from-org [:id :oname]]
                                             [:doc-to-org [:id :oname]]
                                             [:prompts {:ref-deleted nil
                                                        :_order_by {:sort :asc}}
                                              [:id :idstr :prompt :descr #_:sort ;; TODO sort
                                               [:fields {:deleted nil
                                                         :_order_by {:sort :asc}}
                                                [:id :idstr :fname :ftype
                                                 :fsubtype :list? #_:sort]]]]
                                             [:responses
                                              {:ref-deleted nil}
                                              [:id :prompt-id :notes
                                               [:fields {:deleted nil}
                                                [:id :pf-id :idx :sval :nval :dval :jval]]]]]]]}])]
    (fn []
      (if (= :loading @existing-profile&)
        [cc/c-loader]
        (if-let [form-doc (some-> @existing-profile&
                                  :form-docs
                                  first)]
          [docs/c-form-maybe-doc
           (docs/mk-form-doc-state (assoc form-doc
                                          :to-org (:doc-from-org form-doc)))
           {:show-submit true}]
          (let [profile-forms& (rf/subscribe [:gql/sub
                                              {:queries
                                               [[:forms {:ftype "vendor-profile"
                                                         :deleted nil
                                                         :_order_by {:created :desc}
                                                         :_limit 1}
                                                 [:id :title :ftype :fsubtype
                                                  [:prompts {:ref-deleted nil
                                                             :_order_by {:sort :asc}}
                                                   [:id :idstr :prompt :descr #_:sort ;; TODO sort
                                                    [:fields {:deleted nil
                                                              :_order_by {:sort :asc}}
                                                     [:id :idstr :fname :ftype
                                                      :fsubtype :list? #_:sort]]]]]]]}])
                profile-form (first (:forms @profile-forms&))]
            [docs/c-form-maybe-doc (docs/mk-form-doc-state (assoc profile-form
                                                                  ;; this is reversed because of preposal request logic
                                                                  :to-org {:id @org-id&}))
             {:show-submit true}]))))))
