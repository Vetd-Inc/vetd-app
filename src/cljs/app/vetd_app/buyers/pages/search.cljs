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
   (when-not (= search-term (-> db :search :term)) ; only if it really changed (this makes back button behavior better)
     (merge {:db (-> db
                     (assoc-in [:search :term] search-term)
                     (assoc-in [:search :waiting-for-debounce?] true))
             :dispatch-debounce [{:id :b/search
                                  :dispatch [:b/search search-term]
                                  :timeout 250}]}
            (when-not bypass-url-fx
              {:url (url/replace-end url (-> db :search :term) search-term)})))))

(rf/reg-event-fx
 :b/search-filter.add
 (fn [{:keys [db]} [_ group value & [{:keys [event-label]}]]]
   {:db (update-in db [:search :filter group] conj value)
    :analytics/track {:event "Filter"
                      :props {:category "Search"
                              :label event-label}}}))

(rf/reg-event-fx
 :b/search-filter.remove
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:search :filter group] disj value)}))

(rf/reg-event-fx
 :b/search-results-products.empty
 (fn [{:keys [db]}]
   {:db (assoc-in db [:search :results :data :products] {})}))

(rf/reg-event-fx
 :b/search-results-products.set
 (fn [{:keys [db]} [_ value]]
   {:db (assoc-in db [:search :results :data :products] value)}))

(rf/reg-event-fx
 :b/search-page-offset.set
 (fn [{:keys [db]} [_ value]]
   {:db (assoc-in db [:search :page-offset] value)}))

(rf/reg-event-fx
 :b/search-page-offset.add
 (fn [{:keys [db]} [_ value]]
   {:db (update-in db [:search :page-offset] + value)}))

(rf/reg-event-fx
 :b/search-loading?.set
 (fn [{:keys [db]} [_ value]]
   {:db (assoc-in db [:search :loading?] value)}))

(rf/reg-event-fx
 :b/search-fully-loaded?.set
 (fn [{:keys [db]} [_ value]]
   {:db (assoc-in db [:search :fully-loaded?] value)}))

(rf/reg-event-fx
 :b/search
 (fn [{:keys [db ws]} [_ q-str]]
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
     {:db (-> db
              (assoc-in [:search :results :ids] results)
              (assoc-in [:search :waiting-for-debounce?] false))
      :dispatch-n [[:b/search-results-products.empty]
                   [:b/search-page-offset.set 0]
                   [:b/search-fully-loaded?.set false]]}
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
 :search
 (fn [{:keys [search]}] search))

(rf/reg-sub
 :search-term
 :<- [:search]
 (fn [{:keys [term]}] term))

(rf/reg-sub
 :search-filter
 :<- [:search]
 (fn [{:keys [filter]}]
   filter))

(rf/reg-sub
 :search-loading?
 :<- [:search]
 (fn [{:keys [loading?]}]
   loading?))

(rf/reg-sub
 :search-fully-loaded?
 :<- [:search]
 (fn [{:keys [fully-loaded?]}]
   fully-loaded?))

(rf/reg-sub
 :b/search-result-ids
 :<- [:search]
 (fn [{:keys [results]}]
   (-> results :ids)))

(rf/reg-sub
 :waiting-for-debounce?
 :<- [:search]
 (fn [{:keys [waiting-for-debounce?]}]
   waiting-for-debounce?))

(rf/reg-sub
 :search-results-products
 :<- [:search]
 (fn [{:keys [results]}]
   (-> results :data :products)))

(rf/reg-sub
 :search-page-offset
 :<- [:search]
 (fn [{:keys [page-offset]}]
   page-offset))

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
  [ordered-product-ids products]
  [:> ui/ItemGroup {:class "results"}
   (for [id (take (count products) ordered-product-ids)]
     ^{:key id}
     [c-product-list-item (products id)])])

(defn c-category-search-result
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

(defn c-category-search-results
  [categories]
  [:div.categories
   (for [c categories]
     ^{:key (:id c)}
     [c-category-search-result c])])

(defn c-explainer []
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
      "Compare similar products side-by-side based on your unique requirements, and make an informed buying decision in a fraction of the time."]]]])

(defn c-no-results []
  (let [prod-cat-suggestion (r/atom "")]
    (fn []
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
        [:> ui/Button {:color "blue"
                       :onClick #(rf/dispatch [:b/req-new-prod-cat @prod-cat-suggestion])}
         "Request It"]]])))

(defn c-search-results*
  [props]
  (let [search-result-ids& (rf/subscribe [:b/search-result-ids])
        waiting-for-debounce?& (rf/subscribe [:waiting-for-debounce?])
        org-id& (rf/subscribe [:org-id])
        group-ids& (rf/subscribe [:group-ids])]
    (fn [{:keys [search-query& products& page-offset& page-size loading?& fully-loaded?&]}]
      (let [some-products? (seq (:product-ids @search-result-ids&))
            some-categories? (seq (:category-ids @search-result-ids&))
            products-data (when (seq (:product-ids @search-result-ids&))
                            @(rf/subscribe [:gql/sub
                                            {:queries
                                             [[:products {:id (->> @search-result-ids&
                                                                   :product-ids
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
                                                         :from-org-id @org-id&}
                                                 [:id]]
                                                [:docs {:dtype "preposal" ; completed preposals
                                                        :to-org-id @org-id&}
                                                 [:id :idstr :title
                                                  [:from-org [:id :oname]]
                                                  [:from-user [:id :uname]]
                                                  [:to-org [:id :oname]]
                                                  [:to-user [:id :uname]]
                                                  [:response-prompts {:ref_deleted nil}
                                                   [:id :prompt-id :notes :prompt-prompt :prompt-term
                                                    [:response-prompt-fields
                                                     [:id :prompt-field-fname :idx :sval :nval :dval]]]]]]
                                                [:rounds {:buyer-id @org-id&
                                                          :deleted nil}
                                                 [:id :idstr :created :status]]
                                                [:categories {:ref-deleted nil}
                                                 [:id :idstr :cname]]
                                                [:discounts {:id @group-ids&
                                                             :ref-deleted nil}
                                                 [:group-discount-descr :gname]]]]]}]))
            categories-data (when some-categories?
                              @(rf/subscribe [:gql/q
                                              {:queries
                                               [[:categories {:id (:category-ids @search-result-ids&)}
                                                 [:id :idstr :cname
                                                  [:rounds {:buyer-id @org-id&
                                                            :deleted nil}
                                                   [:id :idstr :created :status]]]]]}]))
            _ (rf/dispatch [:b/search-loading?.set (or @waiting-for-debounce?&
                                                       (= :loading products-data)
                                                       (= :loading categories-data))])]
        (if (or @waiting-for-debounce?& ; "(= :loading products-data)" is purposefully missing
                (= :loading categories-data)
                (and (zero? @page-offset&)
                     (= :loading products-data)))
          [cc/c-loader {:style {:margin-top 20}}]
          (do (when-not @loading?&
                (let [new-products (into {}
                                         (for [{:keys [id] :as m} (:products products-data)]
                                           [id m]))
                      total-products (merge @products& new-products)]
                  (rf/dispatch [:b/search-results-products.set total-products])
                  (when (= (count total-products)
                           (count (:product-ids @search-result-ids&)))
                    (rf/dispatch [:b/search-fully-loaded?.set true]))))
              (if some-products?
                [:div.search-results-container
                 (when some-categories?
                   [c-category-search-results (:categories categories-data)])
                 [c-product-search-results (:product-ids @search-result-ids&) @products&]]
                (if (= (count @search-query&) 0)
                  [c-explainer]
                  (when (> (count @search-query&) 2)
                    [c-no-results])))))))))

(def c-search-results
  (let [;; keep a reference to the window-scroll fn (will be created on mount)
        ;; so we can remove the event listener upon unmount
        window-scroll-fn-ref (atom nil)]
    (with-meta c-search-results*
      {:component-did-mount
       (fn [this]
         (let [page-offset& (-> this r/props :page-offset&) ; use subscribe for these instead of passing refs as props? or will that cause unwanted refreshes?
               page-size (-> this r/props :page-size)
               loading?& (-> this r/props :loading?&)
               fully-loaded?& (-> this r/props :fully-loaded?&)
               
               ;; TODO would there be any issue if screen size was less than this??
               load-more-trigger-height 400 ; enough to make the load more ***seamless***
               window-scroll (fn []
                               (when (and (not @loading?&)
                                          (not @fully-loaded?&)
                                          (> (.-scrollY js/window)
                                             (- (.-scrollHeight (.-documentElement js/document))
                                                (.-innerHeight js/window)
                                                load-more-trigger-height)))
                                 ;; should be sync or no?
                                 (rf/dispatch-sync [:b/search-page-offset.add page-size])))
               _ (reset! window-scroll-fn-ref window-scroll)]
           (.addEventListener js/window "scroll" window-scroll)))

       :component-will-unmount
       (fn [this]
         (when @window-scroll-fn-ref
           (.removeEventListener js/window "scroll" @window-scroll-fn-ref)))})))

(defn c-page []
  (let [search-query& (rf/subscribe [:search-term])
        products& (rf/subscribe [:search-results-products])
        page-offset& (rf/subscribe [:search-page-offset])
        loading?& (rf/subscribe [:search-loading?])
        fully-loaded?& (rf/subscribe [:search-fully-loaded?])
        search-input-ref& (atom nil)
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
        [ui/input {:class "product-search borderless"
                   :value @search-query&
                   :size "big"
                   :icon (r/as-element
                          [:> ui/Icon
                           {:name (if (not-empty @search-query&) "delete" "search")
                            :link true
                            :on-click #(do (.focus @search-input-ref&)
                                           (rf/dispatch [:b/update-search-term ""]))}])
                   :autoFocus true
                   :spellCheck false
                   :on-change #(rf/dispatch [:b/update-search-term (-> % .-target .-value)])
                   :placeholder "Search products & categories..."
                   :attrs {:ref #(reset! search-input-ref& %)}}]
        [c-search-results {:search-query& search-query&
                           :products& products&
                           :page-offset& page-offset&
                           ;; you probably want page-size big enough to make the page initially
                           ;; scrollable (assuming there are more results). otherwise you have
                           ;; a situation where they can't cause a scroll event to trigger a load more
                           :page-size 15
                           :loading?& loading?&
                           :fully-loaded?& fully-loaded?&}]]])))
