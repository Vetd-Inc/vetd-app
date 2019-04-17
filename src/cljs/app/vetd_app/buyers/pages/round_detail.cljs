(ns vetd-app.buyers.pages.round-detail
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [markdown-to-hiccup.core :as md]
            [clojure.string :as s]))

(def last-query-id (atom 0))

(defn get-next-query-id []
  (swap! last-query-id inc))

;; Events
(rf/reg-event-fx
 :b/nav-round-detail
 (fn [_ [_ round-idstr]]
   {:nav {:path (str "/b/rounds/" round-idstr)}}))

(rf/reg-event-fx
 :b/route-round-detail
 (fn [{:keys [db]} [_ round-idstr]]
   {:db (assoc db
               :page :b/round-detail
               :page-params {:round-idstr round-idstr})
    :analytics/page {:name "Buyers Round Detail"
                     :props {:round-idstr round-idstr}}}))

;; Subscriptions
(rf/reg-sub
 :round-idstr
 :<- [:page-params] 
 (fn [{:keys [round-idstr]}] round-idstr))

;; Components
(defn c-round-initiation-form
  [round-id]
  (let [requirements-options (r/atom [;; {:key "Subscription Billing"
                                      ;;  :text "Subscription Billing"
                                      ;;  :value "Subscription Billing"}
                                      ;; {:key "Free Trial"
                                      ;;  :text "Free Trial"
                                      ;;  :value "Free Trial"}
                                      ])
        ;; form data
        goal (r/atom "")
        start-using (r/atom "")
        num-users (r/atom "")
        budget (r/atom "")
        requirements (r/atom [])
        add-products-by-name (r/atom "")]
    (fn []
      [:<>
       [:h3 "Round Initiation Form"]
       [:p "Let us now a little more about who will be using this product and what features you are looking for. Then, we'll gather quotes for you to compare right away."]
       [:> ui/Form {:class "round-initiation-form"}
        [:> ui/FormTextArea
         {:label "What are you hoping to accomplish with the product?"
          :on-change (fn [_ this] (reset! goal (.-value this)))}]
        [:> ui/FormField
         [:label "When would you like to start using the product?"]
         [:> ui/Dropdown {:selection true
                          :options [{:key "Within 2 Weeks"
                                     :text "Within 2 Weeks"
                                     :value "Within 2 Weeks"}
                                    {:key "Within 3 Weeks"
                                     :text "Within 3 Weeks"
                                     :value "Within 3 Weeks"}
                                    {:key "Within 1 Month"
                                     :text "Within 1 Month"
                                     :value "Within 1 Month"}
                                    {:key "Within 2 Months"
                                     :text "Within 2 Months"
                                     :value "Within 2 Months"}
                                    {:key "Within 6 Months"
                                     :text "Within 6 Months"
                                     :value "Within 6 Months"}
                                    {:key "Within 12 Months"
                                     :text "Within 12 Months"
                                     :value "Within 12 Months"}]
                          :on-change (fn [_ this]
                                       (reset! start-using (.-value this)))}]]
        [:> ui/FormGroup {:widths "equal"}
         [:> ui/FormField
          [:label "What is your annual budget?"]
          [:> ui/Input {:labelPosition "right"}
           [:> ui/Label {:basic true} "$"]
           [:input {:type "number"
                    :on-change #(reset! budget (-> % .-target .-value))}]
           [:> ui/Label {:basic true} " per year"]]]
         [:> ui/FormField
          [:label "How many people will be using the product?"]
          [:> ui/Input {:labelPosition "right"}
           [:input {:type "number"
                    :on-change #(reset! num-users (-> % .-target .-value))}]
           [:> ui/Label {:basic true} "users"]]]]
        [:> ui/FormField
         [:label "What are your product requirements?"]
         [:> ui/Dropdown {:value @requirements
                          :options @requirements-options
                          :placeholder "Add your requirements..."
                          :search true
                          :selection true
                          :multiple true
                          :allowAdditions true
                          :additionLabel "Hit 'Enter' to Add "
                          :noResultsMessage "Type to add a new requirement..."
                          :onAddItem (fn [_ this]
                                       (let [value (.-value this)]
                                         (swap! requirements-options
                                                conj
                                                {:key value
                                                 :text value
                                                 :value value})))
                          :onChange (fn [_ this]
                                      (reset! requirements (.-value this)))}]]
        #_[:> ui/FormField
           [:label "Are there specific products you want to include?"]
           [:> ui/Dropdown {:multiple true
                            :search true
                            :selection true
                            ;; :on-change (fn [_ this]
                            ;;              (reset! add-products-by-name (.-value this)))
                            :on-change #(.log js/console %1 %2)}]]
        [:> ui/FormButton
         {:color "blue"
          :on-click
          #(rf/dispatch
            [:save-doc
             {:ftype "round-initiation"
              :round-id round-id}
             {:rounds/goal {:value @goal}
              :rounds/start-using {:value @start-using}
              :rounds/num-users {:value @num-users}
              :rounds/budget {:value @budget}
              :rounds/requirements {:value @requirements}
              :rounds/add-products-by-name {:value @add-products-by-name}}])}
         "Submit"]]])))

(defn c-round-initiation
  [{:keys [id status title products doc] :as round}]
  (if doc
    (println doc)
    [c-round-initiation-form id]))

(def dummy-reqs
  ["Pricing Estimate" "Free Trial" "Current Customers" "Integration with GMail"
   "Subscription Billing" "One Time Billing" "Parent / Child Heirarchical Billing"])
(def dummy-products["SendGrid" "Mailchimp" "Mandrill" "iContact"])
(def dummy-resps
  {"Pricing Estimate" ["$45 / mo."
                       "$200 / mo."
                       "If you are in the $0-2M pricing tier, the base fee is $4,000."
                       "Unavailable"]
   "Free Trial" ["First 30 days." "Yes, with limited features." "Yes" "Yes"]
   "Current Customers" ["Google, Patreon, YouTube, Vetd, Make Offices"
                        "Apple, Cisco Enterprise, Symantec, Tommy's Coffee"
                        "Heinz, Philadelphia Business Group, Wizards of the Coast"
                        "None currently."]
   "Integration with GMail" ["Yes" "Yes" "Yes, with PRO account." "No"]
   "Subscription Billing" ["Yes" "Yes" "Yes" "No"]
   "One Time Billing" ["Yes" "No" "Yes" "Yes"]
   "Parent / Child Heirarchical Billing" ["Yes" "Yes" "Yes" "No"]})

(defn c-round-grid
  [{:keys [id status title products] :as round}]
  (let [modal-showing? (r/atom false)
        modal-message (r/atom "")
        cell-click-disabled? (r/atom false)
        ;; the response currently in the modal
        modal-response (r/atom {:requirement {:title nil}
                                :product {:pname nil}
                                :response nil})
        show-modal (fn [requirement product response]
                     (swap! modal-response assoc
                            :requirement requirement
                            :product product
                            :response response)
                     (reset! modal-showing? true))]
    (r/create-class
     {:component-did-mount
      (fn [this]
        ;; set up draggable grid
        (let [node (r/dom-node this)
              mousedown? (atom false)
              x-at-mousedown (atom nil)
              scroll-left-at-mousedown (atom nil)
              mousedown (fn [e]
                          (.add (.-classList node) "dragging")
                          (reset! mousedown? true)
                          (reset! x-at-mousedown (- (.-pageX e) (.-offsetLeft node)))
                          (reset! scroll-left-at-mousedown (.-scrollLeft node)))
              mousemove (fn [e]
                          (when @mousedown?
                            (let [x-displacement (- (- (.-pageX e) (.-offsetLeft node))
                                                    @x-at-mousedown)
                                  new-scroll-left (- @scroll-left-at-mousedown
                                                     (* 3 x-displacement))]
                              (.preventDefault e)
                              (aset node "scrollLeft" new-scroll-left)
                              ;; if you drag more than 3px, disable the cell clickability
                              (when (and (> (Math/abs x-displacement) 3)
                                         (not @cell-click-disabled?))
                                (reset! cell-click-disabled? true)))))
              mouseup (fn [e]
                        (.remove (.-classList node) "dragging")
                        (reset! mousedown? false))]
          (.addEventListener node "mousedown" mousedown)
          (.addEventListener node "mousemove" mousemove)
          (.addEventListener node "mouseup" mouseup)
          (.addEventListener node "mouseleave" mouseup)))
      
      :reagent-render
      (fn []
        (if (or true (seq products))
          [:<>
           [:div.round-grid
            (for [dummy dummy-reqs]
              ^{:key dummy}
              [:div.column 
               [:h4.requirement dummy]
               (for [dummy-product dummy-products
                     :let [resps (get dummy-resps dummy)
                           response (get resps (.indexOf dummy-products dummy-product))]]
                 ^{:key dummy-product}
                 [:div.cell {:on-mouse-down #(reset! cell-click-disabled? false)
                             :on-mouse-up #(when-not @cell-click-disabled?
                                             (show-modal {:title dummy} {:pname dummy-product} response))}
                  [:div.text (util/truncate-text response 150)]
                  [:div.actions
                   [:> ui/Button {:icon "chat" :basic true
                                  :size "mini"}]
                   [:> ui/Button {:icon "thumbs up outline"
                                  :basic true
                                  :size "mini"}]
                   [:> ui/Button {:icon "thumbs down outline"
                                  :basic true
                                  :size "mini"}]]])])]
           [:> ui/Modal {:open @modal-showing?
                         :on-close #(reset! modal-showing? false)
                         :size "tiny"
                         :dimmer "inverted"
                         :closeOnDimmerClick true
                         :closeOnEscape true
                         :closeIcon true}
            [:> ui/ModalHeader (-> @modal-response :product :pname)]
            [:> ui/ModalContent
             [:h4 {:style {:padding-bottom 10}}
              [:> ui/Button {:icon "thumbs down outline"
                             :basic true
                             :size "mini"
                             :style {:float "right"
                                     :margin-right 0}}]
              [:> ui/Button {:icon "thumbs up outline"
                             :basic true
                             :size "mini"
                             :style {:float "right"
                                     :margin-right 4}}]
              (-> @modal-response :requirement :title)]
             (-> @modal-response :response)]
            [:> ui/ModalActions
             [:> ui/Form
              [:> ui/FormField
               [:> ui/TextArea {:placeholder "Ask a follow-up question..."
                                :autoFocus true
                                :spellCheck true
                                :onChange (fn [_ this]
                                            (reset! modal-message (.-value this)))}]]
              [:> ui/Button {:onClick #(reset! modal-showing? false)
                             :color "grey"}
               "Cancel"]
              [:> ui/Button {:onClick #(reset! modal-showing? false)
                             :color "blue"}
               "Submit Question"]]]]]
          [:<>
           [:p [:em "Your requirements have been submitted."]]
           [:p "We are gathering information for you to review from all relevant vendors. Check back soon for updates."]]))})))

(defn c-round
  "Component to display Round details."
  [{:keys [id status title products] :as round}]
  (let [status "in-progress"] ; DEV ONLY, REMOVE
    [:<>
     [:> ui/Segment {:class "detail-container"
                     :style {:margin-left 20}}
      [:h1 {:style {:margin-top 0}}
       title]]
     [:> ui/Segment {:class "detail-container"
                     :style {:margin-left 20}}
      [bc/c-round-status status]]
     (case status
       "initiation" [:> ui/Segment {:class "detail-container"}
                     [c-round-initiation round]]
       "in-progress" [c-round-grid round]
       "complete" [c-round-grid round])]))

(defn c-page []
  (let [round-idstr& (rf/subscribe [:round-idstr])
        org-id& (rf/subscribe [:org-id])
        rounds& (rf/subscribe [:gql/sub
                               {:queries
                                [[:rounds {:idstr @round-idstr&}
                                  [:id :idstr :created :status :title
                                   [:products [:pname]]
                                   [:doc [:id
                                          [:response-prompts {:ref-deleted nil}
                                           [:id :prompt-id :prompt-prompt :prompt-term
                                            [:response-prompt-fields
                                             [:id :prompt-field-fname :idx
                                              :sval :nval :dval]]]]]]]]]}])]
    (fn []
      [:<>
       [:div.container-with-sidebar.round-details
        [:div.sidebar {:style {:margin-right 0}}
         [:div {:style {:padding "0 15px"}}
          [bc/c-back-button {:on-click #(rf/dispatch [:b/nav-rounds])}
           "All VetdRounds"]]
         [:div {:style {:height 154}}] ; spacer
         [:div {:style {:padding "0 15px"}}
          [:> ui/Button {:color "teal"
                         :icon true
                         :fluid true
                         :labelPosition "left"}
           "Add Requirement"
           [:> ui/Icon {:name "plus"}]]]
         (when-not (= :loading @rounds&)
           [:<>
            [:> ui/Segment
             [:h3 "SendGrid"]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "vetd-gradient"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Declare Winner"
              [:> ui/Icon {:name "checkmark"}]]
             
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "grey"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Setup a Call"
              [:> ui/Icon {:name "left call"}]]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "grey"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Disqualify"
              [:> ui/Icon {:name "close"}]]]
            [:> ui/Segment
             [:h3 "Mailchimp"]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "vetd-gradient"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Declare Winner"
              [:> ui/Icon {:name "checkmark"}]]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "grey"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Setup a Call"
              [:> ui/Icon {:name "left call"}]]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "grey"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Disqualify"
              [:> ui/Icon {:name "close"}]]]
            [:> ui/Segment
             [:h3 "Mandrill"]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "vetd-gradient"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Declare Winner"
              [:> ui/Icon {:name "checkmark"}]]
             
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "grey"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Setup a Call"
              [:> ui/Icon {:name "left call"}]]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "grey"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Disqualify"
              [:> ui/Icon {:name "close"}]]]
            [:> ui/Segment
             [:h3 "iContact"]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "vetd-gradient"
                            :fluid true
                            :disabled true
                            :icon true
                            :labelPosition "left"}
              "Declare Winner"
              [:> ui/Icon {:name "checkmark"}]]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "grey"
                            :fluid true
                            :disabled true
                            :icon true
                            :labelPosition "left"}
              "Setup a Call"
              [:> ui/Icon {:name "left call"}]]
             [:> ui/Button {:onClick #(do (.stopPropagation %)
                                          #_(rf/dispatch [:b/do-something]))
                            :color "grey"
                            :fluid true
                            :icon true
                            :labelPosition "left"}
              "Undo Disqualify"
              [:> ui/Icon {:name "undo"}]]]
            ]
           #_(let [{:keys [vendor rounds] :as product} (-> @products& :products first)]
               (when (empty? (:rounds product))
                 [:> ui/Segment
                  [bc/c-start-round-button {:etype :product
                                            :eid (:id product)
                                            :ename (:pname product)
                                            :props {:fluid true}}]
                  [c-preposal-request-button product]
                  [bc/c-setup-call-button product vendor]
                  [bc/c-ask-a-question-button product vendor]])))]
        [:div.inner-container
         (if (= :loading @rounds&)
           [cc/c-loader]
           (let [round (-> @rounds& :rounds first)]
             [c-round round]))]]

       ])))
