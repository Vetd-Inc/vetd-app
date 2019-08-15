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
  {:loading? true
   ;; ID's of stack items that are in edit mode
   :items-editing #{}})

;;;; Subscriptions
(rf/reg-sub
 :b/stack
 (fn [{:keys [stack]}] stack))

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
 :b/stack.add-items
 (fn [{:keys [db]} [_ product-ids]]
   (let [buyer-id (util/db->current-org-id db)]
     {:ws-send {:payload {:cmd :b/stack.add-items
                          :return {:handler :b/stack.add-items.return}
                          :buyer-id buyer-id
                          :product-ids product-ids}}
      :analytics/track {:event "Products Added"
                        :props {:category "Stack"
                                :label buyer-id}}})))

(rf/reg-event-fx
 :b/stack.add-items.return
 (fn [{:keys [db]} [_ stack-item-ids]]
   {:db (assoc-in db
                  [:stack :items-editing]
                  (set (concat (get-in db [:stack :items-editing]) stack-item-ids)))}))

(rf/reg-event-fx
 :b/stack.edit-item
 (fn [{:keys [db]} [_ id]]
   {:db (update-in db [:stack :items-editing] conj id)}))

(rf/reg-event-fx
 :b/stack.save-item
 (fn [{:keys [db]} [_ {:keys [id status rating
                              price-amount price-period
                              renewal-date renewal-reminder]}]]
   {:ws-send {:payload {:cmd :b/stack.update-item
                        :return {:handler :b/stack.save-item.return
                                 :id id}
                        :stack-item-id id
                        ;; :status status
                        :price-amount price-amount
                        :price-period price-period
                        :renewal-date renewal-date
                        ;; :renewal-reminder renewal-reminder
                        ;; :rating rating
                        }}}))

(rf/reg-event-fx
 :b/stack.save-item.return
 (fn [{:keys [db]} [_ _ {{:keys [id]} :return}]]
   {:db (update-in db [:stack :items-editing] disj id)}))

(rf/reg-event-fx
 :b/stack.move-item
 (fn [{:keys [db]} [_ id status]]
   {:ws-send {:payload {:cmd :b/stack.update-item
                        :stack-item-id id
                        :status status}}}))

(rf/reg-event-fx
 :b/stack.rate-item
 (fn [{:keys [db]} [_ id rating]]
   {:ws-send {:payload {:cmd :b/stack.update-item
                        :stack-item-id id
                        :rating rating}}}))

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

(defn c-subscription-type-checkbox
  [s-type subscription-type&]
  [:> ui/Checkbox {:radio true
                   :name "subscriptionType"
                   :label (s/capitalize s-type)
                   :value s-type
                   :checked (= @subscription-type& s-type)
                   :on-change (fn [_ this] (reset! subscription-type& (.-value this)))}])

(defn c-stack-item
  [{:keys [price-amount price-period renewal-date] :as stack-item}]
  (let [stack-items-editing?& (rf/subscribe [:b/stack.items-editing])
        bad-input& (rf/subscribe [:bad-input])
        subscription-type& (r/atom price-period)
        price& (atom price-amount)
        renewal-date& (atom renewal-date)]
    (fn [{:keys [id rating price-amount price-period
                 renewal-date renewal-reminder status
                 product] :as stack-item}]
      (let [{product-id :id
             product-idstr :idstr
             :keys [pname short-desc logo 
                    form-docs vendor]} product
            product-v-fn (->> form-docs
                              first
                              :response-prompts
                              (partial docs/get-value-by-term))]
        [:> ui/Item {:class (when (@stack-items-editing?& id) "editing")
                     :on-click #(when-not (@stack-items-editing?& id)
                                  (rf/dispatch [:b/nav-product-detail product-idstr]))}
         [bc/c-product-logo logo]
         [:> ui/ItemContent
          [:> ui/ItemHeader
           (if (@stack-items-editing?& id)
             ;; Save Changes button
             [:> ui/Label {:on-click #(rf/dispatch [:b/stack.save-item {:id id
                                                                        :price-amount @price&
                                                                        :price-period @subscription-type&
                                                                        :renewal-date @renewal-date&}])
                           :color "blue"
                           :as "a"
                           :style {:float "right"}}
              [:> ui/Icon {:name "check"}]
              "Save Changes"]
             [:<>
              ;; Edit button
              [:> ui/Label {:on-click (fn [e]
                                        (.stopPropagation e)
                                        (rf/dispatch [:b/stack.edit-item id]))
                            :as "a"
                            :style {:float "right"}}
               [:> ui/Icon {:name "edit outline"}]
               "Edit"]
              ;; Move to (Previous/Current) button
              (let [dest-status (if (= status "current") "previous" "current")]
                [:> ui/Label {:on-click (fn [e]
                                          (.stopPropagation e)
                                          (rf/dispatch [:b/stack.move-item id dest-status]))
                              :color "white"
                              :as "a"
                              :style {:float "right"
                                      :margin-right 7}}
                 [:> ui/Icon {:name (str "caret " (if (= dest-status "previous") "down" "up"))}]
                 "Move to " (s/capitalize dest-status)])])
           ;; Product by Vendor heading
           pname " " [:small " by " (:oname vendor)]]
          [:> ui/Transition {:animation "fade"
                             :duration {:show 1000
                                        :hide 0}
                             :visible (@stack-items-editing?& id)}
           (if-not (@stack-items-editing?& id)
             [:span] ; HACK to avoid flash of Form upon Save Changes
             [:> ui/Form
              [:> ui/FormGroup
               [:> ui/FormField {:class "subscription-types"
                                 :width 4
                                 :style {:margin-top 10}}
                [:label "Subscription Type"]
                (for [s-type ["annual" "monthly" "free" "other"]]
                  ^{:key s-type}
                  [c-subscription-type-checkbox s-type subscription-type&])]
               (when (and @subscription-type&
                          (not= @subscription-type& "free"))
                 [:> ui/FormField {:width 5
                                   :style {:margin-top 10}}
                  [:label (when (= @subscription-type& "other") "Estimated ") "Price"]
                  [:> ui/Input {:labelPosition "right"}
                   [:> ui/Label {:basic true} "$"]
                   [:input {:style {:width 0} ; idk why 0 width works, but it does
                            :defaultValue @price&
                            :on-change #(reset! price& (-> % .-target .-value))}]
                   [:> ui/Label {:basic true}
                    " per " (if (#{"annual" "other"} @subscription-type&) "year" "month")]]])
               [:> ui/FormField {:width 1}]
               (when (= @subscription-type& "annual")
                 [:> ui/FormField {:width 3
                                   :style {:margin-top 10}}
                  [:label "Renewal Date"]
                  [:> ui/Input {:placeholder "YYYY-MM-DD"
                                :defaultValue (when renewal-date (subs renewal-date 0 10)) ; TODO fragile (expects particular string format of date from server)
                                :on-change #(reset! renewal-date& (-> % .-target .-value))}
                   ]])]])]
          (when-not (@stack-items-editing?& id)
            [:<>
             [:> ui/ItemExtra {:style {:color "rgba(0, 0, 0, 0.85)"
                                       :font-size 14
                                       :line-height "14px"}}
              [:> ui/Grid {:class "stack-item-grid"}
               [:> ui/GridRow {:class "field-row"}
                [:> ui/GridColumn {:width 3}
                 (when (or price-amount
                           (= price-period "free"))
                   (str (when (= price-period "other") "Estimated ")
                        "Price"))]
                [:> ui/GridColumn {:width 8}
                 (when (and (= price-period "annual")
                            renewal-date)
                   "Annual Renewal")]
                [:> ui/GridColumn {:width 5
                                   :style {:text-align "right"}}
                 "Your Rating"]]
               [:> ui/GridRow {:style {:margin-top 6}}
                [:> ui/GridColumn {:width 3}
                 (if (= price-period "free")
                   "$0"
                   (when price-amount
                     [:<>
                      "$" price-amount
                      (when price-period
                        [:<>
                         " / "
                         (case price-period
                           "annual" "year"
                           "other" "year"
                           "monthly" "month")])]))]
                [:> ui/GridColumn {:width 8}
                 (when (and (= price-period "annual")
                            renewal-date)
                   [:<>
                    (subs renewal-date 0 10) ; TODO fragile (expects particular string format of date from server)
                    [:> ui/Popup
                     {:position "bottom center"
                      :content "Check this box to have a renewal reminder sent to you 2 months before your Annual Renewal date."
                      :trigger (r/as-element
                                [:> ui/Checkbox {:style {:margin-left 10
                                                         :font-size 12}
                                                 :on-click (fn [e]
                                                             (.stopPropagation e))
                                                 :on-change (fn [_ this]
                                                              (println (.-checked this)))
                                                 :label "Remind?"}])}]])]
                [:> ui/GridColumn {:width 5
                                   :style {:text-align "right"}}
                 [:> ui/Rating {:class (when-not rating "not-rated")
                                :maxRating 5
                                :size "large"
                                :defaultRating rating
                                :on-click (fn [e]
                                            (.stopPropagation e))
                                :onRate (fn [_ this]
                                          (rf/dispatch [:b/stack.rate-item id (.-rating this)]))}]]]]]])]]))))

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
        org-name& (rf/subscribe [:org-name])
        group-ids& (rf/subscribe [:group-ids])
        jump-link-refs (atom {})]
    (when @org-id&
      (let [stack& (rf/subscribe [:gql/sub
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
            (let [unfiltered-stack (:stack-items @stack&)]
              [:div.container-with-sidebar
               [:div.sidebar
                [:> ui/Segment
                 [c-add-product-button]]
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
                 [:h1 {:style {:padding-bottom 7}}
                  @org-name& "'" (when (not= (last @org-name&) "s") "s") " Stack"]
                 "Add products to your stack to keep track of renewals, get recommendations, and share with "
                 (if (not-empty @group-ids&)
                   "your community"
                   "others")
                 "."]
                [:div.department
                 [:h2 "Current"]
                 [:span.scroll-anchor {:ref (fn [this] (swap! jump-link-refs assoc "current" this))}] ; anchor
                 [:> ui/ItemGroup {:class "results"}
                  (let [stack (filter (comp (partial = "current") :status) unfiltered-stack)]
                    (if (seq stack)
                      (for [stack-item stack]
                        ^{:key (:id stack-item)}
                        [c-stack-item stack-item])
                      [:div {:style {:margin-left 14
                                     :margin-right 14}}
                       "You don't have any products in your current stack."]))]]
                [:div.department
                 [:h2 "Previous"]
                 [:span.scroll-anchor {:ref (fn [this] (swap! jump-link-refs assoc "previous" this))}] ; anchor
                 [:> ui/ItemGroup {:class "results"}
                  (let [stack (filter (comp (partial = "previous") :status) unfiltered-stack)]
                    (if (seq stack)
                      (for [stack-item stack]
                        ^{:key (:id stack-item)}
                        [c-stack-item stack-item])
                      [:div {:style {:margin-left 14
                                     :margin-right 14}}
                       "You haven't listed any previously used products."]))]]]])))))))
