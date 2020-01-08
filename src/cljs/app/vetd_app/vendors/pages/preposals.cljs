(ns vetd-app.vendors.pages.preposals
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

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

(defn c-profile-completion-cta
  [vendor-profile products]
  (let [remove-blanks (partial remove (comp empty? :fields))
        incomplete-company-profile? (or (-> vendor-profile :form-docs empty?)
                                        (some->> vendor-profile
                                                 :form-docs
                                                 first
                                                 ((juxt :prompts (comp remove-blanks :responses)))
                                                 (map count)
                                                 ;; has more prompts than responses?
                                                 (apply >)))
        min-responses-per-complete 10
        incomplete-product-profiles? (or (-> products :products empty?)
                                         (some->> products
                                                  :products
                                                  (map (comp count remove-blanks :responses first :form-docs))
                                                  (some (partial > min-responses-per-complete))))]
    (when (or incomplete-company-profile?
              incomplete-product-profiles?)
      [:> ui/GridRow
       [:> ui/GridColumn {:computer 4 :mobile 16}]
       [:> ui/GridColumn {:computer 8 :mobile 16}
        [:> ui/Message {:info true
                        :header "Improve your chances of getting buyer attention"
                        :content (r/as-element
                                  [:<>
                                   [:p "Vendors that have "
                                    (when incomplete-company-profile?
                                      "a complete Company Profile")
                                    (when (and incomplete-company-profile?
                                               incomplete-product-profiles?)
                                      " and ")
                                    (when incomplete-product-profiles?
                                      "a complete Product Profile for each of their products")
                                    " are more likely to get a response from buyers."]
                                   (when incomplete-product-profiles?
                                     [:> ui/Button {:on-click #(rf/dispatch [:v/nav-products])
                                                    :color "blue"
                                                    :icon true
                                                    :labelPosition "left"
                                                    :style (merge {:margin-top 14}
                                                                  (when incomplete-company-profile?
                                                                    {:float "right"}))}
                                      "Add/Edit Product Profile(s)"
                                      [:> ui/Icon {:name "compose"}]])
                                   (when incomplete-company-profile?
                                     [:> ui/Button {:on-click #(rf/dispatch [:v/nav-profile])
                                                    :color "teal"
                                                    :icon true
                                                    :labelPosition "left"
                                                    :style {:margin-top 14}}
                                      "Edit Company Profile"
                                      [:> ui/Icon {:name "compose"}]])
                                   [:div {:style {:clear "both"}}]
                                   ])}]]
       [:> ui/GridColumn {:computer 4 :mobile 16}]])))

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
                                     [:id :pf-id :idx :sval :nval :dval :jval]]]]]]]}])
        vendor-profile& (rf/subscribe [:gql/sub
                                       {:queries
                                        [[:form-docs {:ftype "vendor-profile"
                                                      :doc-from-org-id @org-id&
                                                      :_order_by {:created :desc}
                                                      :_limit 1}
                                          [:id :doc-id
                                           [:prompts {:deleted nil
                                                      :ref-deleted nil}
                                            [:id]]
                                           [:responses {:deleted nil
                                                        :ref-deleted nil}
                                            [:id
                                             [:fields {:_where {:_or [{:sval {:_neq ""}}
                                                                      {:nval {:_is_null false}}]}
                                                       :deleted nil}
                                              [:id]]]]]]]}])
        products& (rf/subscribe [:gql/sub
                                 {:queries
                                  [[:products {:vendor-id @org-id&
                                               :deleted nil}
                                    [:id
                                     [:form-docs {:ftype "product-profile"}
                                      [:id :doc-id
                                       [:responses {:deleted nil
                                                    :ref-deleted nil}
                                        [:id
                                         [:fields {:_where {:sval {:_neq ""}}
                                                   :deleted nil}
                                          [:id]]]]]]]]]}])]
    (fn []
      (if (= :loading @preps&)
        [cc/c-loader]
        (if (seq (:form-docs @preps&))
          [:> ui/Grid {:stackable true}
           (let [num-pending-estimate-requests (count (remove :doc-id (:form-docs @preps&)))]
             [:> ui/GridRow
              [:> ui/GridColumn {:computer 4 :mobile 16}]
              [:> ui/GridColumn {:computer 8 :mobile 16}
               [:> ui/Message {:header (str (if (zero? num-pending-estimate-requests)
                                              "No"
                                              num-pending-estimate-requests)
                                            " pending estimate request(s)")
                               :content (r/as-element
                                         [:p "You currently have " num-pending-estimate-requests " pricing estimate request(s) pending your response. When you submit an estimate, the buyer will have a chance to review your personalized pricing info and pitch, and Vetd will let you know if they would like to move forward."])}]]
              [:> ui/GridColumn {:computer 4 :mobile 16}]])
           [c-profile-completion-cta @vendor-profile& @products&]
           (for [form-doc (:form-docs @preps&)]
             ^{:key (str "form" (:id form-doc))}
             [:> ui/GridRow
              [:> ui/GridColumn {:computer 4 :mobile 16}]
              [:> ui/GridColumn {:computer 8 :mobile 16}
               [:> ui/Segment {:class "detail-container"}
                [:h3 "Estimate Request - " (:oname (:from-org form-doc))]
                [:p "Requested By: " (:uname (:from-user form-doc)) " at " (:oname (:from-org form-doc))]
                [:p "Product: " (:pname (:product form-doc))]
                [:p "Status: " (if (:doc-id form-doc) "Submitted (you can still make changes)" "Never Submitted")]
                [docs/c-form-maybe-doc
                 (docs/mk-form-doc-state form-doc)
                 {:show-submit true}]]]
              [:> ui/GridColumn {:computer 4 :mobile 16}]])]
          [:> ui/Grid
           [:> ui/GridRow
            [:> ui/GridColumn {:computer 4 :mobile 16}]
            [:> ui/GridColumn {:computer 8 :mobile 16}
             [:> ui/Segment {:placeholder true
                             :class "how-vetd-works"}
              [:> ui/Header {:icon true}
               [:> ui/Icon {:name "clipboard outline"}]
               "No one has requested an estimate for your products."]
              [:p {:style {:text-align "center"}}
               "When a buyer requests an estimate for one of your products, " [:br] "you will be able to provide a price estimate and personalized pitch here."]]]
            [:> ui/GridColumn {:computer 4 :mobile 16}]]
           [c-profile-completion-cta @vendor-profile& @products&]])))))
