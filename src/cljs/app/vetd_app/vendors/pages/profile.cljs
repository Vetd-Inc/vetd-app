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
        existing-profile& (rf/subscribe [:gql/sub
                                         {:queries
                                          [[:form-docs {:ftype "vendor-profile"
                                                        :doc-from-org-id @org-id&
                                                        :_order_by {:created :desc}
                                                        :_limit 1}
                                            [:id :title :doc-id :doc-title
                                             :ftype :fsubtype
                                             [:from-org [:id :oname]]
                                             [:from-user [:id :uname]]
                                             [:to-org [:id :oname]]
                                             [:to-user [:id :uname]]
                                             [:prompts {:deleted nil
                                                        :_order_by {:sort :asc}}
                                              [:id :idstr :prompt :descr #_:sort ;; TODO sort
                                               [:fields
                                                [:id :idstr :fname :ftype
                                                 :fsubtype :list? #_:sort]]]]
                                             [:responses
                                              [:id :prompt-id :notes
                                               [:fields [:id :pf-id :idx :sval :nval :dval]]]]]]]}])]
    (fn []
      (if (= :loading @existing-profile&)
        [cc/c-loader]
        (if (not-empty (:form-docs @existing-profile&))
          [docs/c-form-maybe-doc
           (docs/mk-form-doc-state (first (:form-docs @existing-profile&)))
           {:show-submit true}]
          (let [profile-forms& (rf/subscribe [:gql/sub
                                              {:queries
                                               [[:forms {:ftype "vendor-profile"
                                                         :_order_by {:created :desc}
                                                         :_limit 1}
                                                 [:id :title :ftype :fsubtype
                                                  [:prompts {:deleted nil
                                                             :_order_by {:sort :asc}}
                                                   [:id :idstr :prompt :descr #_:sort ;; TODO sort
                                                    [:fields
                                                     [:id :idstr :fname :ftype
                                                      :fsubtype :list? #_:sort]]]]]]]}])
                profile-form (first (:forms @profile-forms&))]
            [docs/c-form-maybe-doc (docs/mk-form-doc-state (assoc profile-form
                                                                  ;; this is reversed because of preposal request logic
                                                                  :to-org {:id @org-id&}))
             {:show-submit true}]))))))
