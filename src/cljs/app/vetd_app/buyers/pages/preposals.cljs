(ns vetd-app.buyers.pages.preposals
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

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
 :b/preposals-filter.add-selected-category
 (fn [{:keys [db]} [_ category]]
   {:db (update-in db [:preposals-filter :selected-categories] conj (:id category))
    :analytics/track {:event "Filter"
                      :props {:category "Preposals"
                              :label (str "Added Category: " (:cname category))}}}))

(rf/reg-event-fx
 :b/preposals-filter.remove-selected-category
 (fn [{:keys [db]} [_ category]]
   {:db (update-in db [:preposals-filter :selected-categories] disj (:id category))}))

;;;; Subscriptions
(rf/reg-sub
 :preposals-filter
 :preposals-filter)

;; a set of Category ID's to allow through filter (if empty, let all categories through)
(rf/reg-sub
 :preposals-filter/selected-categories
 :<- [:preposals-filter]
 :selected-categories)

;;;; Components
(defn c-preposal
  "Component to display Preposal as a list item."
  [{:keys [id idstr product from-org responses]}]
  (let [pricing-estimate-value (docs/get-field-value responses "Pricing Estimate" "value" :nval)
        pricing-estimate-unit (docs/get-field-value responses "Pricing Estimate" "unit" :sval)
        pricing-estimate-details (docs/get-field-value responses "Pricing Estimate" "details" :sval)
        free-trial? (= "yes" (docs/get-field-value responses "Do you offer a free trial?" "value" :sval))
        product-profile-responses (-> product :form-docs first :response-prompts)]
    [:> ui/Item {:onClick #(rf/dispatch [:b/nav-preposal-detail idstr])}
     ;; TODO make config var 's3-base-url'
     [:div.product-logo {:style {:background-image
                                 (str "url('https://s3.amazonaws.com/vetd-logos/" (:logo product) "')")}}]
     [:> ui/ItemContent
      [:> ui/ItemHeader
       (:pname product) " " [:small " by " (:oname from-org)]]
      [:> ui/ItemMeta
       (if pricing-estimate-value
         [:span
          (util/currency-format pricing-estimate-value)
          " / "
          pricing-estimate-unit
          " "
          [:small "(estimate) " pricing-estimate-details]]
         pricing-estimate-details)]
      [:> ui/ItemDescription (or (docs/get-field-value-from-response-prompt product-profile-responses
                                                                            "Describe your product or service"
                                                                            "value"
                                                                            :sval)
                                 "No description available.")]
      
      [:> ui/ItemExtra
       (when (empty? (:rounds product))
         [bc/c-start-round-button {:etype :product
                                   :eid (:id product)
                                   :ename (:pname product)
                                   :props {:floated "right"}}])
       [bc/c-categories product]
       (when free-trial? [:> ui/Label {:class "free-trial-tag"
                                       :color "gray"
                                       :size "small"
                                       :tag true}
                          "Free Trial"])]]
     (when (not-empty (:rounds product))
       [bc/c-round-in-progress {:props {:ribbon "right"
                                        :style {:position "absolute"
                                                :marginLeft -14}}}])]))

(defn filter-preposals
  [preposals selected-categories]
  (->> (for [{:keys [product] :as preposal} preposals
             category (:categories product)]
         (when (selected-categories (:id category))
           preposal))
       (remove nil?)
       distinct))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])]
    (when @org-id&
      (let [selected-categories& (rf/subscribe [:preposals-filter/selected-categories])
            preps& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:docs {:dtype "preposal"
                                            :to-org-id @org-id&}
                                     [:id :idstr :title
                                      [:product [:id :pname :logo
                                                 [:form-docs {:doc_deleted nil
                                                              :ftype "product-profile"
                                                              :_order_by {:created :desc}
                                                              :_limit 1}
                                                  [:id 
                                                   [:response-prompts {:prompt-prompt "Describe your product or service"
                                                                       :ref_deleted nil}
                                                    [:id :prompt-id :notes :prompt-prompt
                                                     [:response-prompt-fields
                                                      [:id :prompt-field-fname :idx :sval :nval :dval]]]]]]
                                                 [:rounds {:buyer-id @org-id&
                                                           :status "active"}
                                                  [:id :created :status]]
                                                 [:categories [:id :idstr :cname]]]]
                                      [:from-org [:id :oname]]
                                      [:from-user [:id :uname]]
                                      [:to-org [:id :oname]]
                                      [:to-user [:id :uname]]
                                      [:responses {:ref-deleted nil}
                                       [:id :prompt-id :notes
                                        [:prompt 
                                         [:id :prompt]]
                                        [:fields {:deleted nil}
                                         [:id :pf-id :idx :sval :nval :dval :jval
                                          [:prompt-field [:id :fname]]]]]]]]]}])]
        (fn []
          [:div.container-with-sidebar
           (let [categories (->> @preps&
                                 :docs
                                 (map (comp :categories :product))
                                 flatten
                                 (map #(select-keys % [:id :cname]))
                                 (group-by :id))]
             (when (not-empty categories)
               [:div.sidebar
                [:h4 "Filter By Category"]
                (doall
                 (for [[id v] categories]
                   (let [category (first v)]
                     ^{:key id} 
                     [:> ui/Checkbox {:label (str (:cname category) " (" (count v) ")")
                                      :checked (boolean (@selected-categories& id))
                                      :onChange (fn [_ this]
                                                  (if (.-checked this)
                                                    (rf/dispatch [:b/preposals-filter.add-selected-category category])
                                                    (rf/dispatch [:b/preposals-filter.remove-selected-category category])))}])))]))
           [:> ui/ItemGroup {:class "inner-container results"}
            (if (= :loading @preps&)
              [cc/c-loader]
              (let [preposals (cond-> (:docs @preps&)
                                (seq @selected-categories&) (filter-preposals @selected-categories&))]
                (if (seq preposals)
                  (for [preposal preposals]
                    ^{:key (:id preposal)}
                    [c-preposal preposal])
                  [:div {:style {:width 500
                                 :margin "70px auto"}}
                   [:h3 {:style {:margin-bottom 5}} "You currently don't have any Preposals."]
                   "To get started, request a Preposal from the "
                   [:a {:style {:cursor "pointer"}
                        :onClick #(rf/dispatch [:b/nav-search])}
                    "Products & Categories"]
                   " page."
                   [:br]
                   "Or, simply forward any sales emails you receive to forward@vetd.com."])))]])))))
