(ns vetd-app.buyers.pages.search
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [vetd-app.url :as url]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(def last-query-id (atom 0))

(defn get-next-query-id []
  (swap! last-query-id inc))

(def default-search-filter {:features #{}})

;;;; Events
(rf/reg-event-fx
 :b/nav-search
 (fn [_ [_ search-term]]
   {:nav {:path (str "/b/search"
                     (when search-term (str "/" (js/encodeURI search-term))))}
    :analytics/track {:event "Navigate"
                      :props {:category "Navigation"
                              :label "Buyers Products & Categories"}}}))

(rf/reg-event-fx
 :b/route-search
 (fn [{:keys [db]} [_ search-term]]
   (merge
    {:db (assoc db :page :b/search)
     :analytics/page {:name "Buyers Products & Categories"}}
    (when search-term
      {:dispatch [:b/update-search-term search-term :bypass-url-fx true]}))))

(rf/reg-event-fx
 :b/update-search-term
 [(rf/inject-cofx :url)]
 (fn [{:keys [db url]} [_ search-term & {:keys [bypass-url-fx]}]]
   (merge {:db (assoc db
                      :search-term search-term
                      :page-params {:waiting-for-debounce? true})
           :dispatch-debounce [{:id :b/search
                                :dispatch [:b/search search-term]
                                :timeout 250}]}
          (when-not bypass-url-fx
            {:url (url/replace-end url (:search-term db) search-term)}))))

(rf/reg-event-fx
 :b/search-filter.add
 (fn [{:keys [db]} [_ group value & [{:keys [event-label]}]]]
   {:db (update-in db [:search-filter group] conj value)
    :analytics/track {:event "Filter"
                      :props {:category "Search"
                              :label event-label}}}))

(rf/reg-event-fx
 :b/search-filter.remove
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:search-filter group] disj value)}))

(rf/reg-event-fx
 :b/search
 (fn [{:keys [db ws]} [_ q-str q-type]]
   (let [qid (get-next-query-id)]
     {:db (assoc db :buyer-qid qid)
      :ws-send {:payload {:cmd :b/search
                          :return {:handler :b/ws-search-result-ids
                                   :qid qid}
                          :query q-str
                          :buyer-id (util/db->current-org-id db)
                          :qid qid}}})))

(rf/reg-event-fx
 :b/ws-search-result-ids
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   (if (= (:buyer-qid db) (:qid return))
     {:db (assoc db
                 :b/search-result-ids results
                 :page-params {:waiting-for-debounce? false})}
     {})))

(rf/reg-event-fx
 :b/start-round
 (fn [{:keys [db]} [_ title etype eid]]
   {:ws-send {:payload {:cmd :b/start-round
                        :return {:handler :b/start-round-return}
                        :title title
                        :etype etype
                        :eid eid
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Start"
                      :props {:category "Round"
                              :label etype}}}))

(rf/reg-event-fx
 :b/start-round-return
 (fn [_ [_ {:keys [idstr] :as results}]]
   {:dispatch [:b/nav-round-detail idstr]
    :toast {:type "success"
            :title "New VetdRound created!"
            :message "Please define your requirements."}}))

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

(rf/reg-event-fx
 :b/req-new-prod-cat
 (fn [{:keys [db]} [_ req]]
   {:ws-send {:payload {:cmd :b/req-new-prod-cat
                        :return {:handler :b/req-new-prod-cat-return}
                        :org-id (-> db :memberships first :org-id)
                        :user-id (-> db :user :id)
                        :req req}}}))

(rf/reg-event-fx
 :b/req-new-prod-cat-return
 (constantly
  {:toast {:type "success"
           :title "Thanks for the suggestion!"
           :message "We'll let you know when we add it."}}))

;;;; Subscriptions
(rf/reg-sub
 :search-term
 (fn [{:keys [search-term]}] search-term))

(rf/reg-sub
 :search-filter
 :search-filter)

(rf/reg-sub
 :b/search-result-ids
 (fn [db _]
   (:b/search-result-ids db)))

(rf/reg-sub
 :waiting-for-debounce?
 :<- [:page-params]
 (fn [{:keys [waiting-for-debounce?]}] waiting-for-debounce?))


;;;; Components
(defn c-product-list-item
  [{:keys [id idstr pname short-desc logo rounds
           form-docs forms docs vendor discounts] :as product}]
  (let [requested-preposal? (not-empty forms)
        preposal-responses (-> docs
                               first
                               :response-prompts)
        preposal-v-fn (->> preposal-responses
                           (partial docs/get-value-by-term))
        product-v-fn (->> form-docs
                          first
                          :response-prompts
                          (partial docs/get-value-by-term))]
    [:> ui/Item {:on-click #(rf/dispatch (if preposal-responses
                                           [:b/nav-preposal-detail (-> docs first :idstr)]
                                           [:b/nav-product-detail idstr]))}
     [bc/c-product-logo logo]
     [:> ui/ItemContent
      [:> ui/ItemHeader
       pname " " [:small " by " (:oname vendor)]]
      [:> ui/ItemMeta
       (if preposal-responses
         [bc/c-pricing-estimate preposal-v-fn]
         (if requested-preposal?
           "PrePosal Requested"
           [:<>
            "Pricing Unavailable "
            [:a.teal {:onClick (fn [e]
                                 (.stopPropagation e)
                                 (rf/dispatch [:b/create-preposal-req product vendor]))}
             "Request a PrePosal"]]))]
      [:> ui/ItemDescription (bc/product-description product-v-fn)]
      [:> ui/ItemExtra [bc/c-tags product product-v-fn discounts]]]
     (when (not-empty rounds)
       [bc/c-round-in-progress {:round-idstr (-> rounds first :idstr)
                                :props {:ribbon "right"
                                        :style {:position "absolute"
                                                :marginLeft -14}}}])]))

(defn c-product-search-results
  [products]
  [:> ui/ItemGroup {:class "results"}
   (for [product products]
     ^{:key (:id product)}
     [c-product-list-item product])])

(defn c-category-search-results
  [{:keys [cname id idstr rounds] :as cat}]
  [:div.category-search-result
   (if-let [round-idstr (some-> rounds first :idstr)]
     [:> ui/Label {:color "vetd"
                   :size "large"
                   :as "a"
                   :on-click (fn [e]
                               (.stopPropagation e)
                               (rf/dispatch [:b/nav-round-detail round-idstr]))}
      "VetdRound In Progress for \"" cname "\""]
     [bc/c-start-round-button {:etype :category
                               :eid id
                               :ename cname
                               :popup-props {:position "bottom center"}}])])

(defn c-search-results
  [search-query]
  (let [org-id @(rf/subscribe [:org-id])
        group-ids& (rf/subscribe [:group-ids])
        {:keys [product-ids category-ids] :as ids} @(rf/subscribe [:b/search-result-ids])
        page-size 10
        page-offset& (r/atom 0)
        prods (if (seq product-ids)
                @(rf/subscribe [:gql/sub
                                {:queries
                                 [[:products {:id (->> product-ids
                                                       (drop @page-offset&)
                                                       (take page-size))}
                                   [:id :pname :idstr :logo :score
                                    [:vendor
                                     [:id :oname :idstr :short-desc]] 
                                    [:form-docs {:ftype "product-profile"
                                                 :_order_by {:created :desc}
                                                 :_limit 1
                                                 :doc-deleted nil}
                                     [:id
                                      [:response-prompts {:prompt-term ["product/description"
                                                                        "product/free-trial?"]
                                                          :ref_deleted nil}
                                       [:id :prompt-id :notes :prompt-prompt :prompt-term
                                        [:response-prompt-fields
                                         [:id :prompt-field-fname :idx :sval :nval :dval]]]]]]
                                    [:forms {:ftype "preposal" ; preposal requests
                                             :from-org-id org-id}
                                     [:id]]
                                    [:docs {:dtype "preposal" ; completed preposals
                                            :to-org-id org-id}
                                     [:id :idstr :title
                                      [:from-org [:id :oname]]
                                      [:from-user [:id :uname]]
                                      [:to-org [:id :oname]]
                                      [:to-user [:id :uname]]
                                      [:response-prompts {:ref_deleted nil}
                                       [:id :prompt-id :notes :prompt-prompt :prompt-term
                                        [:response-prompt-fields
                                         [:id :prompt-field-fname :idx :sval :nval :dval]]]]]]
                                    [:rounds {:buyer-id org-id
                                              :deleted nil}
                                     [:id :idstr :created :status]]
                                    [:categories {:ref-deleted nil}
                                     [:id :idstr :cname]]
                                    [:discounts {:id @group-ids&
                                                 :ref-deleted nil}
                                     [:group-discount-descr :gname]]]]]}])
                [])
        categories (if (not-empty category-ids)
                     @(rf/subscribe [:gql/sub
                                     {:queries
                                      [[:categories {:id category-ids}
                                        [:id :idstr :cname
                                         [:rounds {:buyer-id org-id
                                                   :deleted nil}
                                          [:id :idstr :created :status]]]]]}])
                     [])
        loading? (or @(rf/subscribe [:waiting-for-debounce?])
                     (= :loading prods)
                     (= :loading categories))
        prod-cat-suggestion (r/atom "")]
    (if loading?
      [cc/c-loader {:style {:margin-top 20}}]
      (if (not-empty product-ids)
        [:div
         [:div.categories
          (for [c (:categories categories)]
            ^{:key (:id c)}
            [c-category-search-results c])]
         [c-product-search-results (:products prods)]]
        (if (= (count @search-query) 0)
          [:> ui/Segment {:placeholder true
                          :class "how-vetd-works"}
           [:h2 "How Vetd Works . . ."]
           [:> ui/Grid {:columns "equal"
                        :style {:margin-top 4}}
            [:> ui/GridRow
             [:> ui/GridColumn
              [:h3 "Products & Categories"]
              "Search for products or product categories to find products that meet your needs."]
             [:> ui/GridColumn
              [:h3 "PrePosals"]
              "Review PrePosals (personalized pricing estimate and product pitch) you have received from vendors. Don't have any PrePosals yet? Request one by searching above or simply forward vendor emails to forward@vetd.com."]
             [:> ui/GridColumn
              [:h3 "VetdRounds"]
              "Compare similar products side-by-side based on your unique requirements, and make an informed buying decision in a fraction of the time."]]]]
          (when (> (count @search-query) 2)
            [:> ui/Segment {:placeholder true}
             [:> ui/Header {:icon true}
              [:> ui/Icon {:name "search"}]
              "We could not find any matching products or categories."]
             [:> ui/SegmentInline
              [:> ui/Input {:label {:icon "asterisk"}
                            :labelPosition "left corner"
                            :placeholder "Product / Category . . ."
                            :style {:position "relative"
                                    :top 1
                                    :width 240
                                    :margin-right 15}
                            :onChange (fn [_ this]
                                        (reset! prod-cat-suggestion (.-value this)))}]
              #_[:> ui/Select {:compact true
                               :options [{:text "Product"
                                          :value "product"
                                          :key "product"}
                                         {:text "Category"
                                          :value "category"
                                          :key "category"}]
                               :defaultValue "product"}]
              [:> ui/Button {:color "blue"
                             :onClick #(rf/dispatch [:b/req-new-prod-cat @prod-cat-suggestion])}
               "Request It"]]]))))))

(defn c-page []
  (let [search-query& (rf/subscribe [:search-term])
        search-input& (atom nil)
        filter& (rf/subscribe [:search-filter])
        group-ids& (rf/subscribe [:group-ids])]
    (fn []

      [:div.container-with-sidebar
       [:div.sidebar
        [:> ui/Segment
         [:h2 "Filter"]
         [:h4 "Trial"]
         [:> ui/Checkbox {:label "Free Trial"
                          :checked (-> @filter& :features (contains? "free-trial") boolean)
                          :on-change (fn [_ this]
                                       (rf/dispatch [(if (.-checked this)
                                                       :b/search-filter.add
                                                       :b/search-filter.remove)
                                                     :features
                                                     "free-trial"]))}]
         (when (not-empty @group-ids&)
           [:<>
            [:h4 "Discounts"]
            [:> ui/Checkbox {:label "Discounts Available"
                             :checked (-> @filter& :features (contains? "discounts-available") boolean)
                             :on-change (fn [_ this]
                                          (rf/dispatch [(if (.-checked this)
                                                          :b/search-filter.add
                                                          :b/search-filter.remove)
                                                        :features
                                                        "discounts-available"]))}]])

         ;; TODO filter for compelted profile or not
         ]]
       [:div.inner-container
        ;; :> ui/ItemGroup {:class "inner-container results"}


        [ui/input {:class "product-search borderless"
                   :value @search-query&
                   :size "big"
                   :icon (r/as-element
                          [:> ui/Icon
                           {:name (if (not-empty @search-query&) "delete" "search")
                            :link true
                            :on-click #(do (.focus @search-input&)
                                           (rf/dispatch [:b/update-search-term ""]))}])
                   :autoFocus true
                   :spellCheck false
                   :on-change #(rf/dispatch [:b/update-search-term (-> % .-target .-value)])
                   :placeholder "Search products & categories..."
                   :attrs {:ref #(reset! search-input& %)}}]
        [c-search-results search-query&]]]
      )))
