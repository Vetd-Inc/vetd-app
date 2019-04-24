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
(defn get-requirements-options []
  [;; {:key "Subscription Billing"
   ;;  :text "Subscription Billing"
   ;;  :value "Subscription Billing"}
   ;; {:key "Free Trial"
   ;;  :text "Free Trial"
   ;;  :value "Free Trial"}
   ])

(defn c-round-initiation-form
  [round-id]
  (let [requirements-options (r/atom (get-requirements-options))
        ;; form data
        goal (r/atom "")
        start-using (r/atom "")
        num-users (r/atom "")
        budget (r/atom "")
        requirements (r/atom [])
        add-products-by-name (r/atom "")]
    (fn []
      [:<>
       [:h3 "VetdRound Initiation Form"]
       [:p "Let us now a little more about who will be using this product and what features you are looking for. Then, we'll gather quotes for you to compare right away."]
       [:> ui/Form {:as "div"
                    :class "round-initiation-form"}
        [:> ui/FormTextArea
         {:label "What are you hoping to accomplish with the product?"
          :on-change (fn [e this]
                       (reset! goal (.-value this)))}]
        [:> ui/FormGroup {:widths "equal"}
         [:> ui/FormField
          [:label "When do you need to decide by?"]
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
         [:> ui/FormField
          [:label "What is your annual budget?"]
          [:> ui/Input {:labelPosition "right"}
           [:> ui/Label {:basic true} "$"]
           [:input {:type "number"
                    :style {:width 0} ; idk why 0 width works, but it does
                    :on-change #(reset! budget (-> % .-target .-value))}]
           [:> ui/Label {:basic true} " per year"]]]
         [:> ui/FormField
          [:label "How many users?"]
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
             {:dtype "round-initiation"
              :round-id round-id
              :return {:handler :b/round.initiation-form-saved
                       :round-id round-id}}
             {:terms
              {:rounds/goal {:value @goal}
               :rounds/start-using {:value @start-using}
               :rounds/num-users {:value @num-users}
               :rounds/budget {:value @budget}
               :rounds/requirements {:value @requirements}
               :rounds/add-products-by-name {:value @add-products-by-name}}}])}
         "Submit"]]])))

(rf/reg-event-fx
 :b/round.initiation-form-saved
 (fn [_ [_ _ {{:keys [round-id]} :return}]]
   {:toast {:type "success"
            :title "Initiation Form Submitted"
            :message "Status updated to \"In Progress\""}
    :analytics/track {:event "Initiation Form Saved"
                      :props {:category "Round"
                              :label round-id}}}))

(defn c-round-initiation
  [{:keys [id status title products doc] :as round}]
  (if doc
    (println doc)
    [c-round-initiation-form id]))

(def dummy-reqs
  ["Pricing Estimate" "Free Trial" "Current Customers" "Integration with GMail"
   "Subscription Billing" "One Time Billing" "Parent / Child Heirarchical Billing"])
(def dummy-products["SendGrid" "Mailchimp" "Mandrill" "iContact"
                    ])
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

(defn c-action-button
  [{:keys [icon on-click popup-text props]}]
  [:> ui/Popup
   {:content popup-text
    :position "bottom center"
    :trigger (r/as-element
              [:> ui/Button (merge {:on-click on-click
                                    :icon icon
                                    :basic true
                                    :size "mini"}
                                   props)])}])

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
                     (reset! modal-showing? true))
        ;; keep a reference to the window-scroll fn (will be created on mount)
        ;; so we can remove the event listener upon unmount
        window-scroll-fn-ref (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [ ;; draggable grid
              node (r/dom-node this)
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
                        (reset! mousedown? false))
              
              ;; make requirements row 'sticky' upon window scroll
              requirements-pickup-y (atom nil) ; nil when not in 'sticky mode'
              all-requirements-nodes #(array-seq (.getElementsByClassName js/document "requirement"))
              ;; the horizontal position of the requirement row needs to
              ;; be manually updated when in 'sticky mode'
              scroll (fn []
                       (.requestAnimationFrame
                        js/window
                        (fn []
                          (when @requirements-pickup-y
                            (doseq [req-node (all-requirements-nodes)]
                              (aset (.-style req-node) "marginLeft" (str (* -1 (.-scrollLeft node)) "px")))))))
              ;; zero out the artificial horizontal scrolling of the requirements row
              ;; this needs to be called when we leave 'sticky mode'
              zero-out-req-scroll (fn []
                                    (doseq [req-node (all-requirements-nodes)]
                                      (aset (.-style req-node) "marginLeft" "0px")))
              ;; turn on and off requirements row 'sticky mode' as needed
              window-scroll (fn []
                              (.requestAnimationFrame
                               js/window
                               (fn []
                                 (if @requirements-pickup-y
                                   (when (< (.-scrollY js/window) @requirements-pickup-y)
                                     (reset! requirements-pickup-y nil)
                                     (.remove (.-classList node) "fixed")
                                     (zero-out-req-scroll))
                                   (when (> (.-scrollY js/window) (.-offsetTop node))
                                     (reset! requirements-pickup-y (.-offsetTop node))
                                     (.add (.-classList node) "fixed")
                                     ;; call 'scroll' to update horiz pos of req row
                                     ;; (only matters if grid was horiz scrolled/dragged)
                                     (scroll))))))
              _ (reset! window-scroll-fn-ref window-scroll)]
          (.addEventListener node "mousedown" mousedown)
          (.addEventListener node "mousemove" mousemove)
          (.addEventListener node "mouseup" mouseup)
          (.addEventListener node "mouseleave" mouseup)
          (.addEventListener node "scroll" scroll)
          (.addEventListener js/window "scroll" window-scroll)))

      :component-will-unmount
      (fn [this]
        (when @window-scroll-fn-ref
          (.removeEventListener js/window "scroll" @window-scroll-fn-ref)))
      
      :reagent-render
      (fn []
        (if (seq products)
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
                   [c-action-button {:on-click #()
                                     :icon "chat outline"
                                     :popup-text "Ask Question"}]
                   [c-action-button {:on-click #()
                                     :icon "thumbs up outline"
                                     :popup-text "Approve"}]
                   [c-action-button {:on-click #()
                                     :icon "thumbs down outline"
                                     :popup-text "Disapprove"}]]])])]
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
              [c-action-button {:on-click #()
                                :icon "thumbs down outline"
                                :popup-text "Disapprove"
                                :props {:style {:float "right"
                                                :margin-right 0}}}]
              [c-action-button {:on-click #()
                                :icon "thumbs up outline"
                                :popup-text "Approve"
                                :props {:style {:float "right"
                                                :margin-right 4}}}]
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
          [:> ui/Segment {:class "detail-container"
                          :style {:margin-left 20}}
           [:p [:em "Your requirements have been submitted."]]
           [:p "We are gathering information for you to review from all relevant vendors. Check back soon for updates."]]))})))

(defn c-round
  "Component to display Round details."
  [{:keys [id status title products] :as round}]
  [:<>
   [:> ui/Segment {:id "round-title-container"
                   :class (str "detail-container " (when (> (count title) 50) "long"))}
    [:h1.title title]]
   [:> ui/Segment {:id "round-status-container"
                   :class "detail-container"}
    [bc/c-round-status status]]
   (condp contains? status
     #{"initiation"} [:> ui/Segment {:class "detail-container"
                                     :style {:margin-left 20}}
                      [c-round-initiation round]]
     #{"in-progress"
       "complete"} [c-round-grid round])])

(defn c-add-requirement-button
  [{:keys [id] :as round}]
  (let [popup-open? (r/atom false)]
    (fn []
      [:> ui/Popup
       {:position "top left"
        :on "click"
        :open @popup-open?
        :onOpen #(reset! popup-open? true)
        :onClose #(reset! popup-open? false)
        :content (r/as-element
                  (let [new-requirement (atom "")
                        requirements-options (r/atom (get-requirements-options))]
                    [:> ui/Form {:style {:width 350}}
                     [:> ui/Dropdown {:style {:width "100%"}
                                      :options @requirements-options
                                      :placeholder "Enter requirement..."
                                      :search true
                                      :selection true
                                      :multiple false
                                      :selectOnBlur false
                                      :allowAdditions true
                                      :additionLabel "Hit 'Enter' to Add "
                                      :noResultsMessage "Type to add a new requirement..."
                                      :onChange (fn [_ this]
                                                  (reset! popup-open? false)
                                                  (rf/dispatch
                                                   [:b/round.add-requirement id (.-value this)]))}]]))
        :trigger (r/as-element
                  [:> ui/Button {:color "teal"
                                 :icon true
                                 :fluid true
                                 :labelPosition "left"}
                   "Add Requirement"
                   [:> ui/Icon {:name "plus"}]])}])))

(defn c-declare-winner-button
  [round product product-disqualified?]
  [bc/c-sidebar-button
   {:text "Declare Winner"
    :dispatch [:b/round.declare-winner (:id round) (:id product)]
    :icon "checkmark"
    :props {:color "vetd-gradient"
            :disabled product-disqualified?}}])

(defn c-disqualify-button
  [round product product-disqualified?]
  (let [popup-open? (r/atom false)]
    (fn []
      (if-not product-disqualified?
        [:> ui/Popup
         {:position "top left"
          :on "click"
          :open @popup-open?
          :onOpen #(reset! popup-open? true)
          :onClose #(reset! popup-open? false)
          :content (r/as-element
                    (let [reason (atom "")]
                      [:> ui/Form {:style {:width 450}}
                       [:> ui/FormField
                        [:> ui/Input
                         {:placeholder "Enter reason..."
                          :on-change (fn [_ this]
                                       (reset! reason (.-value this)))
                          :action (r/as-element
                                   [:> ui/Button
                                    {:color "teal"
                                     :on-click #(do (reset! popup-open? false)
                                                    (rf/dispatch [:b/round.disqualify
                                                                  (:id round)
                                                                  (:id product)
                                                                  @reason]))}
                                    "Disqualify"])}]]]))
          :trigger (r/as-element
                    [:> ui/Button {:color "grey"
                                   :icon true
                                   :fluid true
                                   :labelPosition "left"}
                     "Disqualify Product"
                     [:> ui/Icon {:name "ban"}]])}]
        [bc/c-sidebar-button
         {:text "Undo Disqualify"
          :dispatch [:b/round.undo-disqualify (:id round) (:id product)]
          :icon "undo"}]))))

(defn c-products
  "Component to display product boxes with various buttons."
  [round products]
  [:<>
   (for [{product-id :id
          pname :pname
          vendor :vendor
          :as product} products       ; TODO actual disqualified value
         :let [product-disqualified? false]]
     ^{:key product-id}
     [:> ui/Segment {:class (str "round-product " (when (> (count pname) 17) "long"))}
      [:h3.name pname]
      [c-declare-winner-button round product product-disqualified?]
      [bc/c-setup-call-button product vendor]
      [c-disqualify-button round product product-disqualified?]])])

(defn c-page []
  (let [round-idstr& (rf/subscribe [:round-idstr])
        rounds& (rf/subscribe [:gql/sub
                               {:queries
                                [[:rounds {:idstr @round-idstr&
                                           :deleted nil}
                                  [:id :idstr :created :status :title
                                   [:products {:deleted nil
                                               :ref-deleted nil}
                                    [:id :pname
                                     [:vendor
                                      [:id :oname]]]]
                                   [:doc
                                    [:id
                                     [:response-prompts {:ref-deleted nil}
                                      [:id :prompt-id :prompt-prompt :prompt-term
                                       [:response-prompt-fields
                                        [:id :prompt-field-fname :idx
                                         :sval :nval :dval]]]]]]]]]}])]
    (fn []
      [:div.container-with-sidebar.round-details
       (if (= :loading @rounds&)
         [cc/c-loader]
         (let [{:keys [status products] :as round} (-> @rounds& :rounds first)]
           [:<> ; sidebar margins (and detail container margins) are customized on this page
            [:div.sidebar {:style {:margin-right 0}}
             [:div {:style {:padding "0 15px"}}
              [bc/c-back-button {:on-click #(rf/dispatch [:b/nav-rounds])}
               "All VetdRounds"]]
             [:div {:style {:height 154}}] ; spacer
             (when (and (#{"in-progress" "complete"} status)
                        (seq products))
               [:<>
                [:div {:style {:padding "0 15px"}}
                 [c-add-requirement-button round]]
                [c-products round products]])]
            [:div.inner-container [c-round round]]]))])))
