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
 (fn [{:keys [db]} [_ etype eid]]
   (let [qid (get-next-query-id)]
     {:ws-send {:payload {:cmd :b/start-round
                          :return {:handler :b/start-round-return}
                          :etype etype
                          :eid eid
                          :buyer-id (util/db->current-org-id db)}}
      :analytics/track {:event "Start"
                        :props {:category "Round"
                                :label etype}}})))

(rf/reg-event-fx
 :b/start-round-return
 (constantly
  {:toast {:type "success"
           :title "Your VetdRound has begun!"
           :message "We'll be in touch with next steps."}}))

(rf/reg-event-fx
 :b/create-preposal-req
 (fn [{:keys [db]} [_ product vendor]]
   (let [qid (get-next-query-id)]
     {:ws-send {:payload {:cmd :b/create-preposal-req

                          :return {:handler :b/create-preposal-req-return
                                   :product product
                                   :vendor vendor}
                          :prep-req {:from-org-id (->> (:active-memb-id db)
                                                       (get (group-by :id (:memberships db)))
                                                       first
                                                       :org-id)
                                     :from-user-id (-> db :user :id)
                                     :prod-id (:id product)}}}})))

(rf/reg-event-fx
 :b/create-preposal-req-return
 (fn [_ [_ _ {{:keys [product vendor]} :return}]]
   {:toast {:type "success"
            :title "Preposal Requested"
            :message "We'll be in touch with next steps."}
    :analytics/track {:event "Request"
                      :props {:category "Preposals"
                              :label (str (:pname product) " by " (:oname vendor))}}}))

(rf/reg-event-fx
 :b/req-new-prod-cat
 (fn [{:keys [db]} [_ req]]
   (let [qid (get-next-query-id)]
     {:ws-send {:payload {:cmd :b/req-new-prod-cat
                          :return {:handler :b/req-new-prod-cat-return}
                          :org-id (-> db :memberships first :org-id)
                          :user-id (-> db :user :id)
                          :req req}}})))

(rf/reg-event-fx
 :b/req-new-prod-cat-return
 (constantly
  {:toast {:type "success"
           :title "Thanks for the suggestion!"
           :message "We'll let you know when we add it."}}))

;; Subscriptions
(rf/reg-sub
 :b/search-result-ids
 (fn [db _]
   (:b/search-result-ids db)))

(rf/reg-sub
 :search-term
 (fn [{:keys [search-term]}] search-term))

(rf/reg-sub
 :waiting-for-debounce?
 :<- [:page-params]
 (fn [{:keys [waiting-for-debounce?]}] waiting-for-debounce?))


;;;; Components
(defn c-product-search-result
  [{:keys [id idstr pname short-desc logo rounds categories forms docs] :as product} vendor]
  (let [preposal-responses (-> docs first :responses)
        requested-preposal? (not-empty forms)]
    [:> ui/Item {:onClick #(rf/dispatch (if preposal-responses
                                          [:b/nav-preposal-detail (-> docs first :idstr)]
                                          [:b/nav-product-detail idstr]))}
     [:div.product-logo {:style {:background-image (str "url('https://s3.amazonaws.com/vetd-logos/" logo "')")}}]
     [:> ui/ItemContent
      [:> ui/ItemHeader
       pname " " [:small " by " (:oname vendor)]]
      [:> ui/ItemMeta
       (if preposal-responses
         (let [p-e-details (docs/get-field-value preposal-responses
                                                 "Pricing Estimate"
                                                 "details"
                                                 :sval)]
           (if-let [p-e-value (docs/get-field-value preposal-responses
                                                    "Pricing Estimate"
                                                    "value"
                                                    :nval)]
             [:span
              (util/currency-format p-e-value)
              " / "
              (docs/get-field-value preposal-responses "Pricing Estimate" "unit" :sval)
              " "
              [:small "(estimate) " p-e-details]]
             (when p-e-details
               (str "Price Estimate: " p-e-details))))
         (if requested-preposal?
           "Preposal Requested"
           [:<>
            "Pricing Unavailable "
            [:a.teal {:onClick #(do (.stopPropagation %)
                                    (rf/dispatch [:b/create-preposal-req product vendor]))}
             "Request a Preposal"]]))]
      [:> ui/ItemDescription short-desc]
      [:> ui/ItemExtra
       [bc/c-categories product]
       (when (and preposal-responses
                  (= "yes" (docs/get-field-value preposal-responses "Do you offer a free trial?" "value" :sval)))
         [:> ui/Label {:class "free-trial-tag"
                       :color "gray"
                       :size "small"
                       :tag true}
          "Free Trial"])]]
     (when (not-empty rounds)
       [bc/c-round-in-progress {:props {:ribbon "right"
                                        :style {:position "absolute"
                                                :marginLeft -14}}}])]))

(defn c-product-search-results
  [v]
  [:> ui/ItemGroup {:class "results"}
   (for [p (:products v)]
     ^{:key (:id p)}
     [c-product-search-result p v])])

(defn c-category-search-results
  [{:keys [cname id idstr rounds] :as cat}]
  [:div.category-search-result
   (if (empty? rounds)
     [:> ui/Button {:on-click #(rf/dispatch [:b/start-round :category id])
                    :color "blue"
                    :icon true
                    :labelPosition "right"}
      (str "Start VetdRound for \"" cname "\"")
      [:> ui/Icon {:name "right arrow"}]]
     [:> ui/Label {:color "teal"
                   :size "large"}
      "VetdRound In Progress for \"" cname "\""])])

(defn c-search-results
  [search-query]
  (let [org-id @(rf/subscribe [:org-id])
        {:keys [product-ids vendor-ids category-ids] :as ids} @(rf/subscribe [:b/search-result-ids])
        prods (if (not-empty (concat product-ids vendor-ids))
                @(rf/subscribe [:gql/sub
                                {:queries
                                 [[:orgs {:id vendor-ids}
                                   [:id :oname :idstr :short-desc
                                    [:products {:id product-ids}
                                     [:id :pname :idstr :short-desc :logo
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
                                        [:responses
                                         [:id :prompt-id :notes
                                          [:prompt
                                           [:id :prompt]]
                                          [:fields
                                           [:id :pf-id :idx :sval :nval :dval
                                            [:prompt-field [:id :fname]]]]]]]]
                                      [:rounds {:buyer-id org-id
                                                :status "active"}
                                       [:id :created :status]]
                                      [:categories [:id :idstr :cname]]]]]]]}])
                [])
        categories (if (not-empty category-ids)
                     @(rf/subscribe [:gql/sub
                                     {:queries
                                      [[:categories {:id category-ids}
                                        [:id :idstr :cname
                                         [:rounds {:buyer-id org-id
                                                   :status "active"}
                                          [:id :created :status]]]]]}])
                     [])
        loading? (or @(rf/subscribe [:waiting-for-debounce?])
                     (= :loading prods)
                     (= :loading categories))
        prod-cat-suggestion (r/atom "")]
    (if loading?
      [cc/c-loader {:style {:margin-top 20}}]
      (if (not-empty (concat product-ids vendor-ids))
        [:div
         [:div.categories
          (for [c (:categories categories)]
            ^{:key (:id c)}
            [c-category-search-results c])]
         (for [v (:orgs prods)]
           ^{:key (:id v)}
           [c-product-search-results v])]
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
             "Request It"]]])))))

(defn c-page []
  (let [search-query& (rf/subscribe [:search-term])]
    (fn []
      [:> ui/Grid
       [:> ui/GridRow
        [:> ui/GridColumn {:computer 4 :mobile 0}]
        [:> ui/GridColumn {:computer 8 :mobile 16}
         [:> ui/Input {:class "product-search"
                       :value @search-query&
                       :size "big"
                       :icon "search"
                       :autoFocus true
                       :spellCheck false
                       :onChange (fn [_ this]
                                   (rf/dispatch [:b/update-search-term (.-value this)]))
                       :placeholder "Search products & categories..."}]]
        [:> ui/GridColumn {:computer 4 :mobile 0}]]
       [:> ui/GridRow
        [:> ui/GridColumn {:computer 2 :mobile 0}]
        [:> ui/GridColumn {:computer 12 :mobile 16}
         [c-search-results search-query&]]
        [:> ui/GridColumn {:computer 2 :mobile 0}]]])))
