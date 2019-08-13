(ns vetd-app.buyers.pages.stack
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(def init-db
  {:filter {:status #{}}
   :loading? true
   ;; ID's of stack items that are in edit mode
   :items-editing #{}})

;;;; Subscriptions
(rf/reg-sub
 :b/stack
 (fn [{:keys [stack]}] stack))

(rf/reg-sub
 :b/stack.filter
 :<- [:b/stack]
 (fn [{:keys [filter]}]
   filter))

(rf/reg-sub
 :b/stack.items-editing
 :<- [:b/stack]
 (fn [{:keys [items-editing]}]
   items-editing))

;;;; Events
(rf/reg-event-fx
 :b/nav-stack
 (constantly
  {:nav {:path "/b/stack"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Buyers Stack"}}}))

(rf/reg-event-fx
 :b/route-stack
 (fn [{:keys [db]}]
   {:db (assoc db :page :b/stack)
    :analytics/page {:name "Buyers Stack"}}))

(rf/reg-event-fx
 :b/stack.filter.add
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:stack :filter group] conj value)}))

(rf/reg-event-fx
 :b/stack.filter.remove
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:stack :filter group] disj value)}))

(rf/reg-event-fx
 :b/stack.add-items
 (fn [{:keys [db]} [_ product-ids]]
   (let [buyer-id (util/db->current-org-id db)]
     {:ws-send {:payload {:cmd :b/stack.add-items
                          :buyer-id buyer-id
                          :product-ids product-ids}}
      :analytics/track {:event "Products Added"
                        :props {:category "Stack"
                                :label buyer-id}}})))


;;;; Components
(defn c-add-product-form
  [stack popup-open?&]
  (let [value& (r/atom [])
        options& (r/atom []) ; options from search results + current values
        search-query& (r/atom "")
        products->options (fn [products]
                            (for [{:keys [id pname]} products]
                              {:key id
                               :text pname
                               :value id}))]
    (fn [stack popup-open?&]
      (let [products& (rf/subscribe
                       [:gql/q
                        {:queries
                         [[:products {:_where {:_and [{:pname {:_ilike (str "%" @search-query& "%")}}
                                                      {:deleted {:_is_null true}}]}
                                      :_limit 100
                                      :_order_by {:pname :asc}}
                           [:id :pname]]]}])
            _ (when-not (= :loading @products&)
                (let [options (->> @products&
                                   :products
                                   products->options ; now we have options from gql sub
                                   ;; (this dumbly actually keeps everything, but that seems fine)
                                   (concat @options&) ; keep options for the current values
                                   distinct)]
                  (when-not (= @options& options)
                    (reset! options& options))))]
        [:> ui/Form {:as "div"
                     :class "popup-dropdown-form"}
         [:> ui/Dropdown {:loading (= :loading @products&)
                          :options @options&
                          :placeholder "Search products..."
                          :search true
                          :selection true
                          :multiple true
                          :selectOnBlur false
                          :selectOnNavigation true
                          :closeOnChange true
                          ;; :allowAdditions true
                          ;; :additionLabel "Hit 'Enter' to Add "
                          ;; :onAddItem (fn [_ this]
                          ;;              (->> this
                          ;;                   .-value
                          ;;                   vector
                          ;;                   ui/as-dropdown-options
                          ;;                   (swap! options& concat)))
                          :onSearchChange (fn [_ this] (reset! search-query& (aget this "searchQuery")))
                          :onChange (fn [_ this] (reset! value& (.-value this)))}]
         [:> ui/Button
          {:color "teal"
           :disabled (empty? @value&)
           :on-click #(do (reset! popup-open?& false)
                          (rf/dispatch [:b/stack.add-items (js->clj @value&)]))}
          "Add"]]))))

(defn c-add-product-button
  [stack]
  (let [popup-open? (r/atom false)]
    (fn [stack]
      [:> ui/Popup
       {:position "bottom left"
        :on "click"
        :open @popup-open?
        :onOpen #(reset! popup-open? true)
        :onClose #(reset! popup-open? false)
        :hideOnScroll false
        :flowing true
        :content (r/as-element [c-add-product-form stack popup-open?])
        :trigger (r/as-element
                  [:> ui/Button {:color "teal"
                                 :icon true
                                 :labelPosition "left"
                                 :fluid true}
                   "Add Product"
                   [:> ui/Icon {:name "plus"}]])}])))

(defn c-stack-item
  [stack-item]
  (let [stack-items-editing?& (rf/subscribe [:b/stack.items-editing])
        bad-input& (rf/subscribe [:bad-input])]
    (fn [{:keys [id product] :as stack-item}]
      (let [{product-id :id
             product-idstr :idstr
             :keys [pname short-desc logo 
                    form-docs vendor]} product
            product-v-fn (->> form-docs
                              first
                              :response-prompts
                              (partial docs/get-value-by-term))]
        [:> ui/Item {:on-click #(rf/dispatch [:b/nav-product-detail product-idstr])}
         [bc/c-product-logo logo]
         [:> ui/ItemContent
          [:> ui/ItemHeader
           pname " " [:small " by " (:oname vendor)]]
          (if (@stack-items-editing?& product-id)
            [:> ui/Form
             [:> ui/FormField {:error (= @bad-input& (keyword (str "edit-stack-item-" id ".price")))}
              [:> ui/Input
               {:placeholder "Price"
                :fluid true
                ;; :on-change #(reset! details& (-> % .-target .-value))
                :action (r/as-element
                         [:> ui/Button { ;; :on-click #(rf/dispatch [:g/add-discount-to-group.submit
                                        ;;                          (:id group)
                                        ;;                          (js->clj @product&)
                                        ;;                          @details&])
                                        :color "blue"}
                          "Save"])}]]]
            [:<>
             
             ;; [:> ui/ItemMeta
             ;;  ]
             ;; [:> ui/ItemDescription (bc/product-description product-v-fn)]
             [:> ui/ItemExtra {:style {:color "rgba(0, 0, 0, 0.85)"
                                       :font-size 14
                                       :line-height "14px"}}
              [:> ui/Grid {:class "stack-item-grid"}
               [:> ui/GridRow {:style {:font-weight 700
                                       :margin-top 7}}
                [:> ui/GridColumn {:width 3}
                 "Price"]
                [:> ui/GridColumn {:width 5}
                 "Annual Renewal"]
                [:> ui/GridColumn {:width 4}
                 "Your Rating"]
                [:> ui/GridColumn {:width 4}
                 "Currently Using?"]]
               [:> ui/GridRow {:style {:margin-top 6}}
                [:> ui/GridColumn {:width 3}
                 "$90 / year"]
                [:> ui/GridColumn {:width 5}
                 "2020-03-24"
                 [:> ui/Checkbox {:style {:margin-left 15
                                          :font-size 12}
                                  :label "Remind?"}]]
                [:> ui/GridColumn {:width 4}
                 [:> ui/Rating {:maxRating 5
                                :size "huge"
                                ;; :icon "star"
                                :on-click (fn [e] (.stopPropagation e))
                                :onRate (fn [_ this]
                                          (println (.-rating this)))}]]
                [:> ui/GridColumn {:width 4}
                 [:> ui/Checkbox
                  {:toggle true
                   ;; :checked (boolean (selected-statuses status))
                   :on-click (fn [e] (.stopPropagation e))
                   :on-change (fn [_ this]
                                
                                #_(if (.-checked this)
                                    (rf/dispatch [:b/stack.filter.add "status" status])
                                    (rf/dispatch [:b/stack.filter.remove "status" status])))
                   }]]]
               ]]])]]))))

(defn c-status-filter-checkboxes
  [stack selected-statuses]
  (let [all-possible-statuses ["current" "previous"]
        statuses (->> stack
                      (group-by :status)
                      (merge (zipmap all-possible-statuses (repeatedly vec))))]
    [:<>
     (for [[status rs] statuses]
       ^{:key status} 
       [:> ui/Checkbox
        {:label (str (-> status
                         (s/replace  #"-" " ")
                         util/capitalize-words) 
                     " (" (count rs) ")")
         :checked (boolean (selected-statuses status))
         :on-change (fn [_ this]
                      (if (.-checked this)
                        (rf/dispatch [:b/stack.filter.add "status" status])
                        (rf/dispatch [:b/stack.filter.remove "status" status])))}])]))

(defn filter-stack
  [stack selected-statuses]
  (filter #(selected-statuses (:status %)) stack))

(defn c-no-stack-items []
  (let [group-ids& (rf/subscribe [:group-ids])]
    (fn []
      [:> ui/Segment {:placeholder true}
       [:> ui/Header {:icon true}
        [:> ui/Icon {:name "grid layout"}]
        "You don't have any products in your stack."]
       [:> ui/SegmentInline
        "Add products to your stack to keep track of renewals, get recommendations, and share with "
        (if (not-empty @group-ids&)
          "your community"
          "others")
        "."]])))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        group-ids& (rf/subscribe [:group-ids])
        jump-link-refs (atom {})]
    (when @org-id&
      (let [filter& (rf/subscribe [:b/stack.filter])
            stack& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:stack-items {:buyer-id @org-id&
                                                   :_order_by {:created :desc}
                                                   :deleted nil}
                                     [:id :idstr :status
                                      :price-amount :price-period :rating
                                      :renewal-date :renewal-reminder
                                      [:product
                                       [:id :pname :idstr :logo
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
                                             [:id :prompt-field-fname :idx :sval :nval :dval]]]]]]]]]]]}])]
        (fn []
          (if (= :loading @stack&)
            [cc/c-loader]
            (let [unfiltered-stack (:stack-items @stack&)
                  selected-statuses (:status @filter&)]
              
              [:div.container-with-sidebar
               [:div.sidebar
                [:> ui/Segment
                 [c-add-product-button]]
                ;; [:> ui/Segment
                ;;  [:h2 "Filter"]
                ;;  [:h4 "Status"]
                ;;  [c-status-filter-checkboxes unfiltered-stack selected-statuses]]
                [:> ui/Segment {:class "top-categories"}
                 [:h4 "Jump To"]
                 [:div
                  [:a.blue {:on-click (fn [e]
                                        (.stopPropagation e)
                                        (.scrollIntoView (get @jump-link-refs "current")
                                                         (clj->js {:behavior "smooth"
                                                                   :block "start"})))}
                   "Current Stack"]]
                 [:div
                  [:a.blue {:on-click (fn [e]
                                        (.stopPropagation e)
                                        (.scrollIntoView (get @jump-link-refs "previous")
                                                         (clj->js {:behavior "smooth"
                                                                   :block "start"})))}
                   "Previous Stack"]]]]
               [:div.inner-container
                [:> ui/Segment {:class "detail-container"}
                 [:h1 "Chooser's Stack"]
                 "Add products to your stack to keep track of renewals, get recommendations, and share with "
                 (if (not-empty @group-ids&)
                   "your community"
                   "others")
                 "."]
                [:div.department
                 [:h2 "Current Stack"]
                 [:span.scroll-anchor {:ref (fn [this] (swap! jump-link-refs assoc "current" this))}] ; anchor
                 [:> ui/ItemGroup {:class "results"}
                  (let [ ;; stack (cond-> unfiltered-stack
                        ;;         (seq selected-statuses) (filter-stack selected-statuses))
                        stack (take 9 unfiltered-stack)
                        ]
                    (for [stack-item stack]
                      ^{:key (:id stack-item)}
                      [c-stack-item stack-item]))]]
                [:div.department
                 [:h2 "Previous Stack"]
                 [:span.scroll-anchor {:ref (fn [this] (swap! jump-link-refs assoc "previous" this))}] ; anchor
                 [:> ui/ItemGroup {:class "results"}
                  (let [ ;; stack (cond-> unfiltered-stack
                        ;;         (seq selected-statuses) (filter-stack selected-statuses))
                        stack (take 9 unfiltered-stack)
                        ]
                    (for [stack-item stack]
                      ^{:key (:id stack-item)}
                      [c-stack-item stack-item]))]]
                ]
               ])))))))
