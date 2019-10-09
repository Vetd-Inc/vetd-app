(ns vetd-app.buyers.pages.stack
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.common.fx :as cfx]
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
  {:nav {:path "/b/stack"} ;; see also core.cljs :nav-home
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Buyers Stack"}}}))

(rf/reg-event-fx
 :b/route-stack
 (fn [{:keys [db]} [_ param]]
   (let [buyer-id (util/db->current-org-id db)]
     (merge {:db (assoc db :page :b/stack)}
            (case param 
              "qb-return" {:toast {:type "success"
                                   :title "Connected to QuickBooks"
                                   :message "We will notify you after your data has been processed and added to your stack."}
                           :analytics/track {:event "QuickBooks Connected"
                                             :props {:category "Stack"
                                                     :label buyer-id}}}
              "qb-return-access-denied" {:toast {:type "error"
                                                 :title "QuickBooks Not Connected"
                                                 :message "We were not able to connect to your QuickBooks account."}
                                         :analytics/track {:event "QuickBooks Failed to Connect"
                                                           :props {:category "Stack"
                                                                   :label buyer-id}}}
              nil)))))

(rf/reg-event-fx
 :b/stack.add-items
 (fn [{:keys [db]} [_ product-ids]]
   (let [buyer-id (util/db->current-org-id db)]
     {:ws-send {:payload {:cmd :b/stack.add-items
                          :return {:handler :b/stack.add-items.return}
                          :buyer-id buyer-id
                          :product-ids product-ids}}
      :scroll-to (-> db :scroll-to-refs :current-stack)
      :analytics/track {:event "Products Added"
                        :props {:category "Stack"
                                :label buyer-id}}})))

(rf/reg-event-fx
 :b/stack.add-items.return
 (fn [{:keys [db]} [_ {:keys [stack-item-ids]}]]
   {:db (assoc-in db
                  [:stack :items-editing]
                  (set (concat (get-in db [:stack :items-editing]) stack-item-ids)))}))

(rf/reg-event-fx
 :b/stack.edit-item
 (fn [{:keys [db]} [_ id]]
   {:db (update-in db [:stack :items-editing] conj id)}))

(rf/reg-event-fx
 :b/stack.stop-editing-item
 (fn [{:keys [db]} [_ id]]
   {:db (-> db
            (update-in [:stack :items-editing] disj id)
            (assoc-in [:page-params :bad-input] nil))}))

(rf/reg-event-fx
 :b/stack.save-item.submit
 (fn [{:keys [db]} [_ {:keys [id price-amount price-period renewal-date renewal-day-of-month]
                       :as input}]]
   (cfx/validated-dispatch-fx db
                              [:b/stack.save-item input]
                              #(cond
                                 (and renewal-date
                                      (not (s/blank? renewal-date))
                                      (not (re-matches #"^\d{4}\-(0?[1-9]|1[012])\-(0?[1-9]|[12][0-9]|3[01])$" renewal-date)))
                                 [:uname "Renewal Date must be in YYYY-MM-DD format, but it is not a required field."]
                                 
                                 (and renewal-day-of-month
                                      (not (s/blank? renewal-day-of-month))
                                      (not (re-matches #"^(0?[1-9]|[12][0-9]|3[01])$" renewal-day-of-month)))
                                 [:uname "Renewal Day of Month must be a number between 1 to 31, but it is not a required field."]
                                 
                                 :else nil))))

(rf/reg-event-fx
 :b/stack.save-item
 (fn [{:keys [db]} [_ {:keys [id price-amount price-period renewal-date renewal-day-of-month]}]]
   {:ws-send {:payload {:cmd :b/stack.update-item
                        :return {:handler :b/stack.save-item.return
                                 :id id}
                        :stack-item-id id
                        :price-amount price-amount
                        :price-period price-period
                        :renewal-date renewal-date
                        :renewal-day-of-month renewal-day-of-month}}}))

(rf/reg-event-fx
 :b/stack.save-item.return
 (fn [{:keys [db]} [_ _ {{:keys [id]} :return}]]
   {:dispatch [:b/stack.stop-editing-item id]}))

(rf/reg-event-fx
 :b/stack.move-item
 (fn [{:keys [db]} [_ id status]]
   {:ws-send {:payload {:cmd :b/stack.update-item
                        :stack-item-id id
                        :status status}}}))

(rf/reg-event-fx
 :b/stack.delete-item
 (fn [{:keys [db]} [_ id]]
   {:ws-send {:payload {:cmd :b/stack.delete-item
                        :stack-item-id id}}}))

(rf/reg-event-fx
 :b/stack.rate-item
 (fn [{:keys [db]} [_ id rating]]
   {:ws-send {:payload {:cmd :b/stack.update-item
                        :stack-item-id id
                        :rating rating}}}))

(rf/reg-event-fx
 :b/stack.set-item-renewal-reminder
 (fn [{:keys [db]} [_ id renewal-reminder]]
   {:ws-send {:payload {:cmd :b/stack.update-item
                        :stack-item-id id
                        :renewal-reminder renewal-reminder}}}))

(rf/reg-event-fx
 :b/stack.store-plaid-token
 (fn [{:keys [db]} [_ public-token]]
   (let [buyer-id (util/db->current-org-id db)]
     {:ws-send {:payload {:cmd :b/stack.store-plaid-token
                          :return {:handler :b/stack.store-plaid-token.return}
                          :buyer-id buyer-id
                          :public-token public-token}}
      :analytics/track {:event "Bank Account Connected"
                        :props {:category "Stack"
                                :label buyer-id}}})))

(rf/reg-event-fx
 :b/stack.store-plaid-token.return
 (fn []
   {:toast {:type "success"
            :title "Bank Account Connected"
            :message "We will notify you after your transaction data has been processed, and products have been added to your stack."}}))

(rf/reg-event-fx
 :b/stack.upload-csv
 (fn [{:keys [db]} [_ file-contents]]
   (let [buyer-id (util/db->current-org-id db)]
     {:ws-send {:payload {:cmd :b/stack.upload-csv
                          :return {:handler :b/stack.upload-csv.return}
                          :buyer-id buyer-id
                          :file-contents file-contents}}
      :analytics/track {:event "CSV Uploaded"
                        :props {:category "Stack"
                                :label buyer-id}}})))

(rf/reg-event-fx
 :b/stack.upload-csv.return
 (fn []
   {:toast {:type "success"
            :title "CSV File Uploaded"
            :message "We will notify you after your data has been processed and added to your stack."}}))

;;;; Components
(defn c-add-product-form
  [stack-items popup-open?&]
  (let [value& (r/atom [])
        options& (r/atom []) ; options from search results + current values
        search-query& (r/atom "")
        products->options (fn [products]
                            (for [{:keys [id pname]} products]
                              {:key id
                               :text pname
                               :value id}))]
    (fn [stack-items popup-open?&]
      (let [products& (rf/subscribe
                       [:gql/q
                        {:queries
                         [[:products {:_where
                                      {:_and ;; while this trims search-query, the Dropdown's search filter doesn't...
                                       [{:pname {:_ilike (str "%" (s/trim @search-query&) "%")}}
                                        {:deleted {:_is_null true}}]}
                                      :_limit 100
                                      :_order_by {:pname :asc}}
                           [:id :pname]]]}])
            product-ids-already-in-stack (set (map (comp :id :product) stack-items))
            _ (when-not (= :loading @products&)
                (let [options (->> @products&
                                   :products
                                   products->options ; now we have options from gql sub
                                   ;; (this dumbly actually keeps everything, but that seems fine)
                                   (concat @options&) ; keep options for the current values
                                   distinct
                                   (remove (comp (partial contains? product-ids-already-in-stack) :value)))]
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
  [stack-items]
  (let [popup-open? (r/atom false)]
    (fn [stack-items]
      [:> ui/Popup
       {:position "right center"
        :on "click"
        :open @popup-open?
        :onOpen #(reset! popup-open? true)
        :onClose #(reset! popup-open? false)
        :hideOnScroll false
        :flowing true
        :content (r/as-element [c-add-product-form stack-items popup-open?])
        :trigger (r/as-element
                  [:> ui/Button {:color "teal"
                                 :icon true
                                 :labelPosition "left"
                                 :fluid true}
                   "Add Products"
                   [:> ui/Icon {:name "plus"}]])}])))

(defn c-qb-import-button
  []
  [:> ui/Form {:method "post"
               :action "https://quickbooks.vetd.com/"
               :style {:margin-top 14}}
   [:> ui/Popup
    {:position "right center"
     :header "Import From QuickBooks"
     :content "Connect to your QuickBooks account and Vetd will add your vendor stack for you."
     :trigger (r/as-element
               [:> ui/Button {:color "lightteal"
                              :icon true
                              :labelPosition "left"
                              :fluid true}
                "QuickBooks"
                [:> ui/Icon {:name "quickbooks"}]])}]])

(defonce plaid-link
  (.create js/Plaid
           (clj->js {"clientName" "Vetd"
                     "countryCodes" ["US"]
                     ;; TODO use production envirionment when available
                     "env" "development"
                     "key" "9c83e7b98a9c97e81d417e4ee7f6ce"
                     "product" ["transactions"]
                     "language" "en"
                     "onSuccess" (fn [public-token metadata]
                                   (rf/dispatch [:b/stack.store-plaid-token public-token]))})))

(defn c-bank-import-button
  []
  [:> ui/Popup
   {:position "right center"
    :header "Import From Bank Account"
    :content "Connect to your bank account and Vetd will add your vendor stack for you, using historical transactions data."
    :trigger (r/as-element
              [:> ui/Button {:color "lightteal"
                             :icon true
                             :labelPosition "left"
                             :fluid true
                             :on-click #(.open plaid-link)}
               "Bank Account"
               [:> ui/Icon {:name "dollar"}]])}])

(defn c-credit-card-import-button
  []
  [:> ui/Popup
   {:position "right center"
    :header "Import From Credit Card"
    :content "Connect to your credit card and Vetd will add your vendor stack for you, using historical transactions data."
    :trigger (r/as-element
              [:> ui/Button {:color "lightteal"
                             :icon true
                             :labelPosition "left"
                             :fluid true
                             :on-click #(.open plaid-link)}
               "Credit Card"
               [:> ui/Icon {:name "credit card outline"}]])}])

(defn c-csv-upload-button
  []
  (let [modal-showing?& (r/atom false)
        file-contents& (r/atom nil)]
    (fn []
      [:<>
       [:> ui/Popup
        {:position "right center"
         :header "Upload A CSV File of Transaction Data"
         :content "Upload a .csv file, and Vetd will add your vendor stack for you."
         :trigger (r/as-element
                   [:> ui/Button {:on-click #(reset! modal-showing?& true)
                                  :color "lightteal"
                                  :icon true
                                  :labelPosition "left"
                                  :fluid true}
                    "CSV File"
                    [:> ui/Icon {:name "file alternate"}]])}]
       [:> ui/Modal {:open @modal-showing?&
                     :on-close #(reset! modal-showing?& false)
                     :size "tiny"
                     :dimmer "inverted"
                     :closeOnDimmerClick false
                     :closeOnEscape true
                     :closeIcon true}
        [:> ui/ModalHeader "Upload a CSV File of Transaction Data"]
        [:> ui/ModalContent
         [:p "Upload a .csv file, and Vetd will add your vendor stack for you."]
         [:p "For best results, include the last 12 months of transaction data."]
         [:h4 "Suggested format:"]
         [:> ui/Table
          [:> ui/TableHeader
           [:> ui/TableRow
            [:> ui/TableHeaderCell "Transaction Date"]
            [:> ui/TableHeaderCell "Vendor/Product Name"]
            [:> ui/TableHeaderCell "Amount"]]]
          [:> ui/TableBody
           [:> ui/TableRow
            [:> ui/TableCell "2019-08-05"]
            [:> ui/TableCell "Slack"]
            [:> ui/TableCell "$25.00"]]
           [:> ui/TableRow
            [:> ui/TableCell "2019-09-05"]
            [:> ui/TableCell "Slack"]
            [:> ui/TableCell "$25.00"]]
           [:> ui/TableRow
            [:> ui/TableCell "2019-09-08"]
            [:> ui/TableCell "Mailchimp"]
            [:> ui/TableCell "$75.00"]]
           [:> ui/TableRow
            [:> ui/TableCell "2019-09-13"]
            [:> ui/TableCell "Carta"]
            [:> ui/TableCell "$2000.00"]]]]
         ]
        [:> ui/ModalActions
         [:> ui/Form {:method "post"
                      :enc-type "multipart/form-data"
                      :style {:margin "5px auto 15px auto"}}
          [:input {:type "file"
                   :accept "text/csv" ;; not sure how much this does...
                   :on-change (fn [e]
                                (let [file (aget e "target" "files" 0)]
                                  (if (or (= (aget file "type") "text/csv")
                                          (= "csv" (s/lower-case (last (s/split (aget file "name") #"\.")))))
                                    (let [onloadend #(reset! file-contents& (aget % "target" "result"))
                                          reader (doto (js/FileReader.)
                                                   (aset "onloadend" onloadend))]
                                      (.readAsBinaryString reader file))
                                    (do (rf/dispatch [:toast {:type "error"
                                                              :title "Only CSV files are accepted."}])
                                        (aset (aget e "target") "value" "")))))}]]
         [:div {:style {:clear "both"}}]
         [:> ui/Button {:onClick #(do (reset! file-contents& nil)
                                      (reset! modal-showing?& false))}
          "Cancel"]
         [:> ui/Button
          {:disabled (nil? @file-contents&)
           :color "blue"
           :on-click #(do (rf/dispatch [:b/stack.upload-csv @file-contents&])
                          (reset! file-contents& nil)
                          (reset! modal-showing?& false))}
          "Upload"]]]])))

(defn c-subscription-type-checkbox
  [s-type subscription-type&]
  [:> ui/Checkbox {:radio true
                   :name "subscriptionType"
                   :label (s/capitalize s-type)
                   :value s-type
                   :checked (= @subscription-type& s-type)
                   :on-change (fn [_ this] (reset! subscription-type& (.-value this)))}])

(defn c-stack-item
  [{:keys [price-amount price-period renewal-date renewal-day-of-month] :as stack-item}]
  (let [stack-items-editing?& (rf/subscribe [:b/stack.items-editing])
        bad-input& (rf/subscribe [:bad-input])
        subscription-type& (r/atom price-period)
        price& (atom price-amount)
        ;; TODO fragile (expects particular string format of date from server)
        renewal-date& (atom (when renewal-date (subs renewal-date 0 10)))
        renewal-day-of-month& (atom renewal-day-of-month)]
    (fn [{:keys [id rating price-amount price-period
                 renewal-date renewal-day-of-month renewal-reminder status
                 product] :as stack-item}]
      (let [{product-id :id
             product-idstr :idstr
             :keys [pname short-desc logo vendor]} product]
        [:> ui/Item {:class (when (@stack-items-editing?& id) "editing")
                     :on-click #(when-not (@stack-items-editing?& id)
                                  (rf/dispatch [:b/nav-product-detail product-idstr]))}
         [bc/c-product-logo logo]
         [:> ui/ItemContent
          [:> ui/ItemHeader
           (if (@stack-items-editing?& id)
             ;; Save Changes button
             [:<>
              [:> ui/Label {:on-click #(rf/dispatch [:b/stack.save-item.submit
                                                     {:id id
                                                      :price-amount (str @price&)
                                                      :price-period (str @subscription-type&)
                                                      :renewal-date (str @renewal-date&)
                                                      :renewal-day-of-month (str @renewal-day-of-month&)}])
                            :color "blue"
                            :as "a"
                            :style {:float "right"}}
               [:> ui/Icon {:name "check"}]
               "Save Changes"]
              [:> ui/Label {:on-click (fn [e]
                                        (.stopPropagation e)
                                        (rf/dispatch [:b/stack.stop-editing-item id]))
                            :as "a"
                            :style {:float "right"
                                    :margin-right 7}}
               "Cancel"]]
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
                [:> ui/Popup
                 {:position "bottom right"
                  :on "click"
                  :content (r/as-element
                            [:div.account-actions
                             [:> ui/Button {:on-click (fn [e]
                                                        (.stopPropagation e)
                                                        (rf/dispatch [:b/stack.move-item id dest-status]))
                                            :color "white"
                                            :fluid true
                                            :icon true
                                            :labelPosition "left"}
                              (str "To " (s/capitalize dest-status))
                              [:> ui/Icon {:name (str "angle double " (if (= status "current") "down" "up"))}]]
                             [:> ui/Button {:on-click (fn [e]
                                                        (.stopPropagation e)
                                                        (rf/dispatch [:b/stack.delete-item id]))
                                            :color "red"
                                            :fluid true
                                            :icon true
                                            :labelPosition "left"}
                              "Delete"
                              [:> ui/Icon {:name "x"}]]])
                  :trigger (r/as-element
                            [:> ui/Label {:on-click (fn [e] (.stopPropagation e))
                                          :as "a"
                                          :style {:float "right"
                                                  :margin-right 7}}
                             [:> ui/Icon {:name "caret down"}]
                             "Move"])}]
                )])
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
                 [:> ui/FormField {:width 6
                                   :style {:margin-top 10}}
                  [:label "Renewal Date"]
                  [:> ui/Input {:placeholder "YYYY-MM-DD"
                                :style {:width 130}
                                :defaultValue (when renewal-date (subs renewal-date 0 10)) ; TODO fragile (expects particular string format of date from server)
                                :on-change #(reset! renewal-date& (-> % .-target .-value))}]])
               (when (= @subscription-type& "monthly")
                 [:> ui/FormField {:width 6
                                   :style {:margin-top 10}}
                  [:label "Renewal Day of Month"]
                  [:> ui/Input {:placeholder "DD"
                                :style {:width 60}
                                :defaultValue renewal-day-of-month
                                :on-change #(reset! renewal-day-of-month& (-> % .-target .-value))}]])]])]
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
                   "Annual Renewal")
                 (when (and (= price-period "monthly")
                            renewal-day-of-month)
                   "Monthly Renewal Day")]
                [:> ui/GridColumn {:width 5
                                   :style {:text-align "right"}}
                 "Your Rating"]]
               [:> ui/GridRow {:style {:margin-top 6}}
                [:> ui/GridColumn {:width 3}
                 (if (= price-period "free")
                   "Free"
                   (when price-amount
                     [:<>
                      "$" (util/decimal-format price-amount)
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
                                                 :defaultChecked renewal-reminder
                                                 :on-click (fn [e]
                                                             (.stopPropagation e))
                                                 :on-change (fn [_ this]
                                                              (rf/dispatch [:b/stack.set-item-renewal-reminder id (.-checked this)])
                                                              ;; return 'this' to keep it as an uncontrolled component
                                                              this)
                                                 :label "Remind?"}])}]])
                 (when (and (= price-period "monthly")
                            renewal-day-of-month)
                   (str renewal-day-of-month
                        (case renewal-day-of-month
                          1 "st"
                          2 "nd"
                          3 "rd"
                          21 "st"
                          22 "nd"
                          23 "rd"
                          31 "st"
                          "th")))]
                [:> ui/GridColumn {:width 5
                                   :style {:text-align "right"}}
                 [:> ui/Rating {:class (when-not rating "not-rated")
                                :maxRating 5
                                :size "large"
                                :defaultRating rating
                                :clearable false
                                :on-click (fn [e]
                                            (.stopPropagation e))
                                :onRate (fn [_ this]
                                          (rf/dispatch [:b/stack.rate-item id (aget this "rating")]))}]]]]]])]]))))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        org-name& (rf/subscribe [:org-name])
        group-ids& (rf/subscribe [:group-ids])]
    (when @org-id&
      (let [stack& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:stack-items {:buyer-id @org-id&
                                                   :_order_by {:created :desc}
                                                   :deleted nil}
                                     [:id :idstr :status
                                      :price-amount :price-period :rating
                                      :renewal-date :renewal-day-of-month
                                      :renewal-reminder
                                      [:product
                                       [:id :pname :idstr :logo
                                        [:vendor
                                         [:id :oname :idstr :short-desc]]]]]]]}])]
        (fn []
          (if (= :loading @stack&)
            [cc/c-loader]
            (let [unfiltered-stack (:stack-items @stack&)]
              [:div.container-with-sidebar
               [:div.sidebar
                [:> ui/Segment
                 [c-add-product-button unfiltered-stack]
                 [:h4 "Import Transactions"]
                 [c-qb-import-button]
                 [c-bank-import-button]
                 [c-credit-card-import-button]
                 [c-csv-upload-button]]
                [:> ui/Segment {:class "top-categories"}
                 [:h4 "Jump To"]
                 [:div
                  [:a.blue {:on-click (fn [e]
                                        (.stopPropagation e)
                                        (rf/dispatch [:scroll-to :current-stack]))}
                   "Current Stack"]]
                 [:div
                  [:a.blue {:on-click (fn [e]
                                        (.stopPropagation e)
                                        (rf/dispatch [:scroll-to :previous-stack]))}
                   "Previous Stack"]]]]
               [:div.inner-container
                [:> ui/Segment {:class "detail-container"}
                 [:h1 {:style {:padding-bottom 0}}
                  @org-name& "'" (when (not= (last @org-name&) "s") "s") " Stack"]
                 [:p
                  "Add products to your stack to keep track of renewals, get recommendations, and share with "
                  (if (not-empty @group-ids&)
                    "your community"
                    "others")
                  "."]
                 [:p
                  "You can add products by name, or use one of the "
                  [:strong "Import Transactions"]
                  " options, and Vetd will build out your stack for you."]]
                [:div.stack
                 [:h2 "Current"]
                 [:span.scroll-anchor {:ref (fn [this] (rf/dispatch [:reg-scroll-to-ref :current-stack this]))}]
                 [:> ui/ItemGroup {:class "results"}
                  (let [stack (filter (comp (partial = "current") :status) unfiltered-stack)]
                    (if (seq stack)
                      (for [stack-item stack]
                        ^{:key (:id stack-item)}
                        [c-stack-item stack-item])
                      [:div {:style {:margin-left 14
                                     :margin-right 14}}
                       "You don't have any products in your current stack."]))]]
                [:div.stack
                 [:h2 "Previous"]
                 [:span.scroll-anchor {:ref (fn [this] (rf/dispatch [:reg-scroll-to-ref :previous-stack this]))}]
                 [:> ui/ItemGroup {:class "results"}
                  (let [stack (filter (comp (partial = "previous") :status) unfiltered-stack)]
                    (if (seq stack)
                      (for [stack-item stack]
                        ^{:key (:id stack-item)}
                        [c-stack-item stack-item])
                      [:div {:style {:margin-left 14
                                     :margin-right 14}}
                       "You haven't listed any previously used products."]))]]]])))))))
