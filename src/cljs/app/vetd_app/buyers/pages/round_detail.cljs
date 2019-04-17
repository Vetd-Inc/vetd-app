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

(defn c-round-grid
  [{:keys [id status title products] :as round}]
  (let [modal-showing? (r/atom false)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        ;; draggable grid
        (let [node (r/dom-node this)
              mousedown? (atom false)
              x-at-mousedown (atom nil)
              scrollleft-at-mousedown (atom nil)
              handle-mousedown
              (fn [e]
                (.add (.-classList node) "dragging")
                (reset! x-at-mousedown (- (.-pageX e) (.-offsetLeft node)))
                (reset! scrollleft-at-mousedown (.-scrollLeft node))
                (reset! mousedown? true))
              handle-mouseup
              (fn [e]
                (.remove (.-classList node) "dragging")
                (reset! mousedown? false))
              handle-mousemove
              (fn [e]
                (when @mousedown?
                  (do (.preventDefault e)
                      (aset node
                            "scrollLeft"
                            (- @scrollleft-at-mousedown
                               (* 3 (- (- (.-pageX e) (.-offsetLeft node))
                                       @x-at-mousedown)))))))]
          (.addEventListener node "mousedown" handle-mousedown)
          (.addEventListener node "mouseup" handle-mouseup)
          (.addEventListener node "mouseleave" handle-mouseup)
          (.addEventListener node "mousemove" handle-mousemove)))
      ;; TODO will the above garbage collect fine?
      ;; or need to removeEventListener on component-will-unmount?
      
      :reagent-render
      (fn []
        (if (or true (seq products))
          [:<>
           [:div.round-grid
            (for [i (range 8)]
              ^{:key i}
              [:div.column 
               [:h4.requirement
                (nth ["Subscription Billing" "Parent / Child Hierarchial" "Prepay Option"  "Integration with GMail" "Example Of A Longer Requirement Title Is Here" "Coffee Flavors"]
                     (mod i 6))]
               (for [j (range 4)]
                 ^{:key (str "j" j)}
                 [:div.cell {:on-click #(reset! modal-showing? true)}
                  [:div.text
                   (util/truncate-text
                    (nth ["Lorem ipsum lorem ispum lorem ispum lorem ispum loremum lorem ispum lorem m ispum."
                          "Lorem ipsum lorem ispum lorem ispum lorem ispum loremum lorem ispum lorem m ispum. Lorem ipsum lorem ispum lorem ispum lorem ispum loremum lorem ispum lorem m ispum."
                          "Ipsum lorem ispum lorem ispum lorem ispum lorem m ispum. Lorem ipsum lorem ispum lorem ispum lorem ispum loremum lorem ispum lorem m ispum. Ipsum lorem ispum lorem ispum lorem ipsum ipsum ipsum ipsum ispum loremum lorem ispum lorem m ispum. Lorem ipsum lorem ispum lorem ispum lorem ispum loremum lorem ispum lorem m ispum."
                          "Yes"
                          "Lorem ipsum lorem ispum."]
                         (mod (+ i j) 5))
                    150)]
                  [:div.actions
                   [:> ui/Button {:icon "chat"
                                  :basic true
                                  :size "mini"
                                  }]
                   [:> ui/Button {:icon "check"
                                  :basic true
                                  :size "mini"
                                  }]
                   [:> ui/Button {:icon "close"
                                  :basic true
                                  :size "mini"
                                  }]]])])]
           [:> ui/Modal {:open @modal-showing?
                         :size "tiny"
                         :dimmer "inverted"
                         :closeOnDimmerClick false
                         :closeOnEscape false}
            [:> ui/ModalHeader "Ask a Question About"]
            [:> ui/ModalContent
             [:> ui/Form
              [:> ui/FormField
               [:> ui/TextArea {:placeholder ""
                                :autoFocus true
                                :spellCheck true
                                ;; :onChange (fn [_ this]
                                ;;             (reset! message (.-value this)))
                                }]]]]
            [:> ui/ModalActions
             [:> ui/Button {:onClick #(reset! modal-showing? false)}
              "Cancel"]
             [:> ui/Button {:onClick #(do ;; (rf/dispatch [:b/ask-a-question
                                        ;;               id
                                        ;;               pname
                                        ;;               @message])
                                        (reset! modal-showing? false))
                            :color "blue"}
              "Submit"]]]]
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
             [:h3 "Stripe Billing"]
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
             [:h3 "Capital One"]
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
             [:h3 "PayPal Developers"]
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
            [:> ui/Segment
             [:h3 "Cryptocurrency"]
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
