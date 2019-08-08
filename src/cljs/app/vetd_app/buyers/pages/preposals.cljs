(ns vetd-app.buyers.pages.preposals
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [clojure.string :as s]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(def default-preposals-filter {:status #{"live"}
                               :features #{}
                               :categories #{}})

;;;; Events
(rf/reg-event-fx
 :b/nav-preposals
 (constantly
  {:nav {:path "/b/preposals"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Buyers Preposals"}}}))

(rf/reg-event-fx
 :b/route-preposals
 (fn [{:keys [db]}]
   {:db (assoc db :page :b/preposals)
    :analytics/page {:name "Buyers Preposals"}}))

(rf/reg-event-fx
 :b/preposals-filter.add
 (fn [{:keys [db]} [_ group value & [{:keys [event-label]}]]]
   {:db (update-in db [:preposals-filter group] conj value)
    :analytics/track {:event "Filter"
                      :props {:category "Preposals"
                              :label event-label}}}))

(rf/reg-event-fx
 :b/preposals-filter.remove
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:preposals-filter group] disj value)}))

(rf/reg-event-fx
 :b/preposals.reject
 (fn [{:keys [db]} [_ id reason]]
   {:ws-send {:payload {:cmd :b/preposals.set-result
                        :return {:handler :b/preposals.reject-return
                                 :id id}
                        :id id
                        :result 0
                        :reason reason
                        :buyer-id (util/db->current-org-id db)}}}))

(rf/reg-event-fx
 :b/preposals.reject-return
 (fn [{:keys [db]} [_ _ {{:keys [id]} :return}]]
   {:toast {:type "success"
            :title "PrePosal Rejected"}
    :analytics/track {:event "Reject"
                      :props {:category "Preposal"
                              :label id}}}))

(rf/reg-event-fx
 :b/preposals.undo-reject
 (fn [{:keys [db]} [_ id]]
   {:ws-send {:payload {:cmd :b/preposals.set-result
                        :id id
                        :result nil
                        :reason nil
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Undo Reject"
                      :props {:category "Preposal"
                              :label id}}}))

(rf/reg-event-fx
 :b/create-preposal-req
 (fn [{:keys [db]} [_ product vendor]]
   {:ws-send {:payload {:cmd :b/create-preposal-req
                        :return {:handler :b/create-preposal-req-return
                                 :product product
                                 :vendor vendor}
                        :prep-req {:from-org-id (->> (:active-memb-id db)
                                                     (get (group-by :id (:memberships db)))
                                                     first
                                                     :org-id)
                                   :from-user-id (-> db :user :id)
                                   :prod-id (:id product)}}}}))

(rf/reg-event-fx
 :b/create-preposal-req-return
 (fn [_ [_ _ {{:keys [product vendor]} :return}]]
   {:toast {:type "success"
            :title "PrePosal Requested"
            :message "We'll be in touch with next steps."}
    :analytics/track {:event "Request"
                      :props {:category "Preposals"
                              :label (str (:pname product) " by " (:oname vendor))}}}))

;;;; Subscriptions
(rf/reg-sub
 :preposals-filter
 :preposals-filter)

;;;; Components
(defn c-preposal-list-item
  [{:keys [id idstr result response-prompts product from-org] :as preposal}]
  (let [{:keys [pname logo rounds discounts]} product
        {:keys [oname]} from-org
        rejected? (= 0 result)
        preposal-v-fn (partial docs/get-value-by-term response-prompts)
        product-v-fn (->> product
                          :form-docs
                          first
                          :response-prompts
                          (partial docs/get-value-by-term))]
    [:> ui/Item {:on-click #(rf/dispatch [:b/nav-preposal-detail idstr])}
     [bc/c-product-logo logo]
     [:> ui/ItemContent
      [:> ui/ItemHeader pname " " [:small " by " oname]
       (when (empty? rounds) 
         [bc/c-reject-preposal-button id rejected? {:icon? true}])]
      [:> ui/ItemMeta [bc/c-pricing-estimate preposal-v-fn]]
      [:> ui/ItemDescription (bc/product-description product-v-fn)]
      [:> ui/ItemExtra
       (when (and (empty? (:rounds product))
                  (not rejected?))
         [bc/c-start-round-button {:etype :product
                                   :eid (:id product)
                                   :ename (:pname product)
                                   :props {:floated "right"}
                                   :popup-props {:position "bottom right"}}])
       [bc/c-tags product product-v-fn discounts]]]
     (when (not-empty (:rounds product))
       [bc/c-round-in-progress {:round-idstr (-> product :rounds first :idstr)
                                :props {:ribbon "right"
                                        :style {:position "absolute"
                                                :marginLeft -14}}}])]))

(defn filter-preposals
  "filter-map contains sets that define the included values for a certain group/trait.
  An empty set makes all values be included for a certain group."
  [preposals {:as filter-map
              :keys [status features categories]
              :or {status #{}
                   features #{}
                   categories #{}}}]
  (cond->> preposals ; roughly ordered by how much they slim the threading input
    (features "discounts-available") (filter (comp seq :discounts :product))
    (features "free-trial") (filter #(some-> %
                                             :product
                                             :form-docs
                                             first
                                             :response-prompts
                                             (docs/get-value-by-term :product/free-trial?)
                                             s/lower-case
                                             (= "yes")))
    (seq status) (filter (fn [{:keys [result]}]
                           (or (and (status "live")
                                    (= result nil))
                               (and (status "rejected")
                                    (= result 0)))))
    (seq categories) (#(->> (for [{:keys [product] :as preposal} %
                                  {:keys [id]} (:categories product)]
                              (when (categories id) preposal))
                            (remove nil?)
                            distinct))))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])]
    (when @org-id&
      (let [filter& (rf/subscribe [:preposals-filter])
            group-ids& (rf/subscribe [:group-ids])
            preps& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:docs {:dtype "preposal"
                                            :to-org-id @org-id&
                                            :_order_by {:created :desc}
                                            :deleted nil}
                                     [:id :idstr :title :result :reason
                                      [:product
                                       [:id :pname :logo
                                        [:form-docs {:doc-deleted nil
                                                     :ftype "product-profile"
                                                     :_order_by {:created :desc}
                                                     :_limit 1}
                                         [:id
                                          [:response-prompts {:prompt-term ["product/description"
                                                                            "product/free-trial?"]
                                                              :ref_deleted nil}
                                           [:id :prompt-id :notes :prompt-prompt :prompt-term
                                            [:response-prompt-fields
                                             [:id :prompt-field-fname :idx :sval :nval :dval]]]]]]
                                        [:rounds {:buyer-id @org-id&
                                                  :deleted nil}
                                         [:id :idstr :created :status]]
                                        [:categories {:ref-deleted nil}
                                         [:id :idstr :cname]]
                                        [:discounts {:id @group-ids&}
                                         [:group-discount-descr]]]]
                                      [:from-org [:id :oname]]
                                      [:from-user [:id :uname]]
                                      [:to-org [:id :oname]]
                                      [:to-user [:id :uname]]
                                      [:response-prompts {:ref_deleted nil}
                                       [:id :prompt-id :notes :prompt-prompt :prompt-term
                                        [:response-prompt-fields
                                         [:id :prompt-field-fname :idx :sval :nval :dval]]]]]]]}])]
        (fn []
          (if (= :loading @preps&)
            [cc/c-loader]
            (let [unfiltered-preposals (:docs @preps&)]
              (if (seq unfiltered-preposals)
                [:div.container-with-sidebar ; only show categories of preposals that have been let past the status filter
                 (let [categories (->> (filter-preposals unfiltered-preposals (select-keys @filter& [:status]))
                                       (map (comp :categories :product))
                                       flatten
                                       (map #(select-keys % [:id :cname]))
                                       (group-by :id))]
                   [:div.sidebar
                    [:> ui/Segment
                     [:h2 "Filter"]
                     [:h4 "Status"]
                     [:> ui/Checkbox {:label "Live"
                                      :checked (-> @filter& :status (contains? "live") boolean)
                                      :on-change (fn [_ this]
                                                   (rf/dispatch [(if (.-checked this)
                                                                   :b/preposals-filter.add
                                                                   :b/preposals-filter.remove)
                                                                 :status
                                                                 "live"]))}]
                     [:> ui/Checkbox {:label "Rejected"
                                      :checked (-> @filter& :status (contains? "rejected") boolean)
                                      :on-change (fn [_ this]
                                                   (rf/dispatch [(if (.-checked this)
                                                                   :b/preposals-filter.add
                                                                   :b/preposals-filter.remove)
                                                                 :status
                                                                 "rejected"]))}]
                     [:h4 "Trial"]
                     [:> ui/Checkbox {:label "Free Trial"
                                      :checked (-> @filter& :features (contains? "free-trial") boolean)
                                      :on-change (fn [_ this]
                                                   (rf/dispatch [(if (.-checked this)
                                                                   :b/preposals-filter.add
                                                                   :b/preposals-filter.remove)
                                                                 :features
                                                                 "free-trial"]))}]
                     (when (not-empty @group-ids&)
                       [:<>
                        [:h4 "Discounts"]
                        [:> ui/Checkbox {:label "Discounts Available"
                                         :checked (-> @filter& :features (contains? "discounts-available") boolean)
                                         :on-change (fn [_ this]
                                                      (rf/dispatch [(if (.-checked this)
                                                                      :b/preposals-filter.add
                                                                      :b/preposals-filter.remove)
                                                                    :features
                                                                    "discounts-available"]))}]])
                     (when (not-empty categories)
                       [:<>
                        [:h4 "Category"]
                        (doall
                         (for [[id v] categories]
                           (let [category (first v)]
                             ^{:key id} 
                             [:> ui/Checkbox {:label (str (:cname category) " (" (count v) ")")
                                              :checked (-> @filter& :categories (contains? id) boolean)
                                              :on-change (fn [_ this]
                                                           (rf/dispatch [(if (.-checked this)
                                                                           :b/preposals-filter.add
                                                                           :b/preposals-filter.remove)
                                                                         :categories
                                                                         id
                                                                         {:event-label (str "Added Category: " (:cname category))}]))}])))])]])
                 [:> ui/ItemGroup {:class "inner-container results"}
                  (let [preposals (filter-preposals unfiltered-preposals @filter&)]
                    (if (seq preposals)
                      (for [preposal preposals]
                        ^{:key (:id preposal)}
                        [c-preposal-list-item preposal])
                      [:> ui/Segment {:placeholder true}
                       [:> ui/Header {:icon true}
                        [:> ui/Icon {:name "wpforms"}]
                        "No PrePosals match your filter choices."]
                       [:div {:style {:text-align "center"
                                      :margin-top 10}}
                        "To request a new PrePosal, search for "
                        [:a {:style {:cursor "pointer"}
                             :onClick #(rf/dispatch [:b/nav-search])}
                         "Products & Categories."]
                        [:br] [:br]
                        "Or, simply forward any sales emails you receive to forward@vetd.com."]]))]]
                [:> ui/Grid
                 [:> ui/GridRow
                  [:> ui/GridColumn {:computer 2 :mobile 0}]
                  [:> ui/GridColumn {:computer 12 :mobile 16}
                   [:> ui/Segment {:placeholder true}
                    [:> ui/Header {:icon true}
                     [:> ui/Icon {:name "wpforms"}]
                     "You don't have any PrePosals."]
                    [:div {:style {:text-align "center"
                                   :margin-top 10}}
                     "To get started, request a PrePosal from the "
                     [:a {:style {:cursor "pointer"}
                          :onClick #(rf/dispatch [:b/nav-search])}
                      "Products & Categories"]
                     " page."
                     [:br] [:br]
                     "Or, simply forward any sales emails you receive to forward@vetd.com."]]]
                  [:> ui/GridColumn {:computer 2 :mobile 0}]]]))))))))
