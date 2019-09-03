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

(def init-db
  {:term ""
   :filter {:features #{} :groups #{}}
   :waiting-for-debounce? false
   :loading? true
   :infinite-scroll {:more-results? false
                     :triggered? false
                     :page-offset 0}
   :results {:ids {:product-ids []
                   :category-ids []}
             :data {:products {}
                    ;; :categories [] ; consider keeping cat results cached as well?
                    }}})

;;;; Subscriptions
(rf/reg-sub
 :b/search
 (fn [{:keys [search]}] search))

(rf/reg-sub
 :b/search.term
 :<- [:b/search]
 (fn [{:keys [term]}] term))

(rf/reg-sub
 :b/search.filter
 :<- [:b/search]
 (fn [{:keys [filter]}]
   filter))

(rf/reg-sub
 :b/search.loading?
 :<- [:b/search]
 (fn [{:keys [loading?]}]
   loading?))

(rf/reg-sub
 :b/search.results.ids
 :<- [:b/search]
 (fn [{:keys [results]}]
   (-> results :ids)))

(rf/reg-sub
 :b/search.some-products?
 :<- [:b/search.results.ids]
 (fn [{:keys [product-ids]}]
   (boolean (seq product-ids))))

(rf/reg-sub
 :b/search.some-categories?
 :<- [:b/search.results.ids]
 (fn [{:keys [category-ids]}]
   (boolean (seq category-ids))))

(rf/reg-sub
 :b/search.results.data.products
 :<- [:b/search]
 (fn [{:keys [results]}]
   (-> results :data :products)))

(rf/reg-sub
 :b/search.waiting-for-debounce?
 :<- [:b/search]
 (fn [{:keys [waiting-for-debounce?]}]
   waiting-for-debounce?))

(rf/reg-sub
 :b/search.infinite-scroll
 :<- [:b/search]
 (fn [{:keys [infinite-scroll]}] infinite-scroll))

(rf/reg-sub
 :b/search.infinite-scroll.more-results?
 :<- [:b/search.infinite-scroll]
 (fn [{:keys [more-results?]}]
   more-results?))

(rf/reg-sub
 :b/search.infinite-scroll.page-offset
 :<- [:b/search.infinite-scroll]
 (fn [{:keys [page-offset]}]
   page-offset))

(rf/reg-sub
 :b/search.infinite-scroll.triggered?
 :<- [:b/search.infinite-scroll]
 (fn [{:keys [triggered?]}]
   triggered?))


;;;; Events
(def last-query-id (atom 0))
(defn get-next-query-id []
  (swap! last-query-id inc))

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
      {:dispatch [:b/search.term.update search-term :bypass-url-fx true]}))))

(rf/reg-event-fx
 :b/search.term.update
 [(rf/inject-cofx :url)]
 (fn [{:keys [db url]} [_ search-term & {:keys [bypass-url-fx force-refresh]}]]
   (when (or (-> db :search :term ; only if it really changed (this makes back button behavior better)
                 (not= search-term))
             force-refresh)
     (merge {:db (-> db
                     (assoc-in [:search :term] search-term)
                     (assoc-in [:search :waiting-for-debounce?] true))
             :dispatch-debounce [{:id :b/search
                                  :dispatch [:b/search]
                                  :timeout 250}]}
            (when-not bypass-url-fx
              {:url (url/replace-end url (-> db :search :term) search-term)})))))

(rf/reg-event-fx
 :b/search.filter.add
 (fn [{:keys [db]} [_ group value & [{:keys [event-label]}]]]
   {:db (update-in db [:search :filter group] conj value)
    :dispatch-debounce [{:id :b/search
                         :dispatch [:b/search]
                         :timeout 0}]
    :analytics/track {:event "Filter"
                      :props {:category "Search"
                              :label event-label}}}))

(rf/reg-event-fx
 :b/search.filter.remove
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:search :filter group] disj value)
    :dispatch-debounce [{:id :b/search
                         :dispatch [:b/search]
                         :timeout 0}]}))

(rf/reg-event-fx
 :b/search.reset
 (fn [{:keys [db]}]
   {:db (assoc-in db [:search :filter] (:filter init-db))
    :dispatch [:b/search.term.update "" :force-refresh true]}))

(rf/reg-event-fx
 :b/search.results.data.products.empty
 (fn [{:keys [db]}]
   {:db (assoc-in db [:search :results :data :products] {})}))

(rf/reg-event-fx
 :b/search.infinite-scroll.add-products
 (fn [{:keys [db]} [_ products]]
   (let [;; _ (println "add products called") ; this is getting called twice every load?!
         total-products (merge (get-in db [:search :results :data :products])
                               (into {} (for [{:keys [id] :as m} products]
                                          [id m])))]
     {:db (-> db
              (assoc-in [:search :results :data :products] total-products)
              (assoc-in [:search :infinite-scroll :triggered?] false)
              (assoc-in [:search :infinite-scroll :more-results?]
                        (< (count total-products)
                           (count (get-in db [:search :results :ids :product-ids])))))})))

(rf/reg-event-fx
 :b/search.infinite-scroll.page-offset.set
 (fn [{:keys [db]} [_ value]]
   {:db (assoc-in db [:search :infinite-scroll :page-offset] value)}))

(rf/reg-event-fx
 :b/search.infinite-scroll.load-more
 (fn [{:keys [db]} [_ value]]
   {:db (-> db
            (assoc-in [:search :infinite-scroll :triggered?] true)
            (update-in [:search :infinite-scroll :page-offset] + value))}))

(rf/reg-event-fx
 :b/search.loading?.set
 (fn [{:keys [db]} [_ value]]
   {:db (assoc-in db [:search :loading?] value)}))

(rf/reg-event-fx
 :b/search.infinite-scroll.triggered?.set
 (fn [{:keys [db]} [_ value]]
   {:db (assoc-in db [:search :infinite-scroll :triggered?] value)}))

(rf/reg-event-fx
 :b/search.infinite-scroll.more-results?.set
 (fn [{:keys [db]} [_ value]]
   {:db (assoc-in db [:search :infinite-scroll :more-results?] value)}))

(rf/reg-event-fx
 :b/search
 (fn [{:keys [db ws]}]
   (let [qid (get-next-query-id)]
     {:db (assoc db :buyer-qid qid)
      :ws-send {:payload {:cmd :b/search
                          :return {:handler :b/search.return
                                   :qid qid}
                          :query (-> db :search :term)
                          :filter-map
                          (let [filter-map (-> db :search :filter)]
                            (cond-> filter-map
                              ((:features filter-map) "discounts-available") (assoc :discounts-available-to-groups
                                                                                    @(rf/subscribe [:group-ids]))))
                          :buyer-id (util/db->current-org-id db)
                          :qid qid}}
      :dispatch-n [[:b/search.results.data.products.empty]
                   [:b/search.infinite-scroll.page-offset.set 0]
                   [:b/search.infinite-scroll.more-results?.set true]]})))

(rf/reg-event-fx
 :b/search.return
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   (if (= (:buyer-qid db) (:qid return))
     {:db (-> db
              (assoc-in [:search :results :ids] results)
              (assoc-in [:search :waiting-for-debounce?] false))}
     {})))

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


;;;; Components
(defn c-product-list-item
  [{:keys [id idstr pname short-desc logo rounds
           form-docs forms vendor discounts
           agg-group-prod-rating agg-group-prod-price] :as product}]
  (let [product-v-fn (->> form-docs
                          first
                          :response-prompts
                          (partial docs/get-value-by-term))
        orgs-using-count (reduce (fn [acc {:keys [count-stack-items]}]
                                   (+ acc count-stack-items))
                                 0
                                 agg-group-prod-rating)]
    [:> ui/Item {:on-click #(rf/dispatch [:b/nav-product-detail idstr])}
     [bc/c-product-logo logo]
     (when (not-empty rounds)
       [bc/c-round-in-progress {:round-idstr (-> rounds first :idstr)
                                :props {:ribbon "left"
                                        :style {:position "absolute"
                                                :left -14}}}])
     [:> ui/ItemContent
      [:> ui/ItemHeader
       pname " " [:small " by " (:oname vendor)]]
      [:> ui/ItemDescription (bc/product-description product-v-fn)]
      [:> ui/ItemExtra [bc/c-tags product product-v-fn discounts]]]
     
     (when (pos? orgs-using-count)
       [:div.community
        [:div.metric
         [:h5
          "Your Community"]
         "Used by " orgs-using-count " "
         [:> ui/Icon {:name "group"}]]
        [:div.metric
         [:h5
          "Median Price"]
         (let [median-prices (map :median-price agg-group-prod-price)]
           (if (seq median-prices)
             (str "$" ;; get the mean from all the member'd groups' medians
                  (util/decimal-format (/ (apply + median-prices) (count median-prices)))
                  " / year")
             "No pricing data."))]
        (when-let [mean (:mean (util/rating-avg-map agg-group-prod-rating))]
          [:> ui/Rating {:rating mean
                         :maxRating 5
                         :size "huge"
                         :disabled true}])])]))

(defn c-product-search-results
  "Given a list product IDs in order, and an indexed map of products by id,
  return products as list items."
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
   [cc/c-grid {:columns "equal"
               :style {:margin-top 4}}
    [[[:<>
       [:h3 "Products & Categories"]
       "Search for products or product categories to find products that meet your needs."]]
     [[:<>
       [:h3 "PrePosals"]
       "Review PrePosals (personalized pricing estimate and product pitch) you have received from vendors. Don't have any PrePosals yet? Request one by searching above or simply forward vendor emails to forward@vetd.com."
       ]]
     [[:<>
       [:h3 "VetdRounds"]
       "Compare similar products side-by-side based on your unique requirements, and make an informed buying decision in a fraction of the time."]]]]])

(defn potentially-emphasize-filters []
  (let [filter& (rf/subscribe [:b/search.filter])
        has-filter? (some seq (vals @filter&))
        search-filter-node (util/node-by-id "search-filter")]
    (when (and has-filter?
               search-filter-node)
      (util/add-class search-filter-node "shake")
      (js/setTimeout (fn []
                       (when search-filter-node
                         (util/remove-class search-filter-node "shake")))
                     2000))))

(defn c-no-results []
  (let [prod-cat-suggestion (r/atom "")
        ;; side effect everytime this component is mounted:
        ;; if there are no results, and there are filters selected,
        ;; emphasize to user that the filters may be causing the no results.
        _ (potentially-emphasize-filters)]
    (fn []
      [:> ui/Segment {:placeholder true}
       [:> ui/Header {:icon true}
        [:> ui/Icon {:name "search"}] ;; TODO different message if they have filters on (let them know filters may be limiting results)
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
  (let [search-result-ids& (rf/subscribe [:b/search.results.ids])
        some-products?& (rf/subscribe [:b/search.some-products?])
        some-categories?& (rf/subscribe [:b/search.some-categories?])
        waiting-for-debounce?& (rf/subscribe [:b/search.waiting-for-debounce?])
        search-term& (rf/subscribe [:b/search.term])
        products& (rf/subscribe [:b/search.results.data.products])
        page-offset& (rf/subscribe [:b/search.infinite-scroll.page-offset])
        loading?& (rf/subscribe [:b/search.loading?])
        org-id& (rf/subscribe [:org-id])
        group-ids& (rf/subscribe [:group-ids])]
    (fn [{:keys [page-size]}]
      (let [products-data (when @some-products?&
                            @(rf/subscribe
                              [:gql/sub
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
                                   [:rounds {:buyer-id @org-id&
                                             :deleted nil}
                                    [:id :idstr :created :status]]
                                   [:categories {:ref-deleted nil}
                                    [:id :idstr :cname]]
                                   [:agg-group-prod-rating {:group-id @group-ids&}
                                    [:group-id :product-id
                                     :count-stack-items :rating]]
                                   [:agg-group-prod-price {:group-id @group-ids&}
                                    [:group-id :median-price]]
                                   [:discounts {:id @group-ids&
                                                :ref-deleted nil}
                                    [:group-discount-descr :gname]]]]]}]))
            categories-data (when @some-categories?&
                              @(rf/subscribe
                                [:gql/q
                                 {:queries
                                  [[:categories {:id (->> @search-result-ids&
                                                          :category-ids
                                                          (take 3))}
                                    [:id :idstr :cname
                                     [:rounds {:buyer-id @org-id&
                                               :deleted nil}
                                      [:id :idstr :created :status]]]]]}]))
            ;; this is suspect...
            _ (rf/dispatch-sync [:b/search.loading?.set (or @waiting-for-debounce?&
                                                            (= :loading products-data)
                                                            (= :loading categories-data))])]
        (if (or @waiting-for-debounce?&
                (= :loading categories-data)
                (and (zero? @page-offset&)
                     (= :loading products-data)))
          [cc/c-loader {:style {:margin-top 20}}]
          (do (when-not (= :loading products-data) ;; @loading?& ;; sketchy
                (rf/dispatch-sync [:b/search.infinite-scroll.add-products (:products products-data)]))
              (if @some-products?&
                [:div.search-results-container
                 (when @some-categories?&
                   [c-category-search-results (:categories categories-data)])
                 [c-product-search-results (:product-ids @search-result-ids&) @products&]]
                (if (= (count @search-term&) 0)
                  [c-explainer]
                  [c-no-results]))))))))

(def c-search-results
  (let [;; keep a reference to the window-scroll fn (will be created on mount)
        ;; so we can remove the event listener upon unmount
        window-scroll-fn-ref (atom nil)]
    (with-meta c-search-results*
      {:component-did-mount
       (fn [this]
         (let [page-size (-> this r/props :page-size)
               loading?& (rf/subscribe [:b/search.loading?])
               more-results?& (rf/subscribe [:b/search.infinite-scroll.more-results?])
               page-offset& (rf/subscribe [:b/search.infinite-scroll.page-offset])
               triggered?& (rf/subscribe [:b/search.infinite-scroll.triggered?])
               load-more-trigger-height 400 ; enough to make the load more SEAMLESS
               window-scroll (fn []
                               (when (and (not @loading?&)
                                          @more-results?&
                                          (not @triggered?&)
                                          (> (.-scrollY js/window)
                                             (- (.-scrollHeight (.-documentElement js/document))
                                                (.-innerHeight js/window)
                                                load-more-trigger-height)))
                                 ;; should be sync or no?
                                 (rf/dispatch-sync [:b/search.infinite-scroll.load-more page-size])))
               _ (reset! window-scroll-fn-ref window-scroll)]
           (.addEventListener js/window "scroll" window-scroll)))

       :component-will-unmount
       (fn [this]
         (when @window-scroll-fn-ref
           (.removeEventListener js/window "scroll" @window-scroll-fn-ref)))})))

(defn c-page []
  (let [search-term& (rf/subscribe [:b/search.term])
        search-input-ref& (atom nil)
        filter& (rf/subscribe [:b/search.filter])
        group-ids& (rf/subscribe [:group-ids])
        active-org& (rf/subscribe [:active-org])]
    (fn []
      [:div.container-with-sidebar
       [:div.sidebar
        [:> ui/Segment {:id "search-filter"}
         [:h2 "Filter"]
         [:> ui/Popup
          {:position "bottom left"
           :content "Products that have been through our vetting process and have in-depth information available."
           :trigger (r/as-element
                     [:> ui/Checkbox {:label (r/as-element
                                              [:label
                                               [:> ui/Icon {:name "vetd"
                                                            :size "small"
                                                            :color "vetd-colors"}]
                                               [:span {:style {:position "relative"
                                                               :top -5}}
                                                "Vetd Products"]])
                                      :checked (-> @filter& :features (contains? "product-profile-completed") boolean)
                                      ;; on-click works better than on-change here
                                      :on-click (fn [_ this]
                                                  (rf/dispatch [(if (.-checked this)
                                                                  :b/search.filter.add
                                                                  :b/search.filter.remove)
                                                                :features
                                                                "product-profile-completed"]))}])}]
         [:h4 "Trial"]
         [:> ui/Checkbox {:label "Free Trial"
                          :checked (-> @filter& :features (contains? "free-trial") boolean)
                          :on-change (fn [_ this]
                                       (rf/dispatch [(if (.-checked this)
                                                       :b/search.filter.add
                                                       :b/search.filter.remove)
                                                     :features
                                                     "free-trial"]))}]
         (when (not-empty @group-ids&)
           [:<>
            [:h4 "Discounts"]
            [:> ui/Checkbox {:label "Discounts Available"
                             :checked (-> @filter& :features (contains? "discounts-available") boolean)
                             :on-change (fn [_ this]
                                          (rf/dispatch [(if (.-checked this)
                                                          :b/search.filter.add
                                                          :b/search.filter.remove)
                                                        :features
                                                        "discounts-available"]))}]])
         (when (not-empty @group-ids&)
           [:<>
            [:h4 "Community Usage"]
            (doall
             (for [{group-id :id
                    group-name :gname} (:groups @active-org&)]
               ^{:key group-id}
               [:> ui/Checkbox {:label group-name
                                :checked (-> @filter& :groups (contains? group-id) boolean)
                                :on-change (fn [_ this]
                                             (rf/dispatch [(if (.-checked this)
                                                             :b/search.filter.add
                                                             :b/search.filter.remove)
                                                           :groups
                                                           group-id]))}]))])]
        [:> ui/Segment {:class "top-categories"}
         [:h4 "Top Categories"]
         (let [top-categories ["CRM"
                               "Applicant Tracking"
                               "Live Chat"
                               "Human Resource"
                               "Board Management"
                               "Email Marketing"
                               "Project Management"
                               "Data Visualization"]]
           (doall
            (for [category top-categories]
              ^{:key category}
              [:div
               [:a.blue {:on-click (fn [e]
                                     (.stopPropagation e)
                                     (rf/dispatch [:b/nav-search category]))}
                category]])))]]
       [:div.inner-container
        [ui/input {:class "product-search borderless"
                   :value @search-term&
                   :size "big"
                   :icon (r/as-element
                          [:> ui/Icon
                           {:name (if (not-empty @search-term&) "delete" "search")
                            :link true
                            :on-click #(do (.focus @search-input-ref&)
                                           (rf/dispatch [:b/search.term.update ""]))}])
                   :autoFocus true
                   :spellCheck false
                   :on-change #(rf/dispatch [:b/search.term.update (-> % .-target .-value)])
                   :placeholder "Search products & categories..."
                   :attrs {:ref #(reset! search-input-ref& %)}}]
        ;; you probably want page-size big enough to make the page initially
        ;; scrollable (assuming there are more results). otherwise you have
        ;; a situation where they can't cause a scroll event to trigger a load more
        [c-search-results {:page-size 15}]]])))
