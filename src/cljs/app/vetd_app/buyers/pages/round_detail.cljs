(ns vetd-app.buyers.pages.round-detail
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [clojure.string :as s]))

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

(rf/reg-event-fx
 :b/round.initiation-form-saved
 (fn [_ [_ _ {{:keys [round-id]} :return}]]
   {:toast {:type "success"
            :title "Initiation Form Submitted"
            :message "Status updated to \"In Progress\""}
    :analytics/track {:event "Initiation Form Saved"
                      :props {:category "Round"
                              :label round-id}}}))

(rf/reg-event-fx
 :b/round.add-requirement
 (fn [{:keys [db]} [_ round-id requirement-text]]
   {:ws-send {:payload {:cmd :b/round.add-requirement
                        :round-id round-id
                        :requirement-text requirement-text
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Add Requirement"
                      :props {:category "Round"
                              :label round-id}}}))

(rf/reg-event-fx
 :b/round.declare-winner
 (fn [{:keys [db]} [_ round-id product-id]]
   {:ws-send {:payload {:cmd :b/round.declare-result
                        :round-id round-id
                        :product-id product-id
                        :result 1
                        :buyer-id (util/db->current-org-id db)}}
    :confetti nil
    :analytics/track {:event "Declare Winner"
                      :props {:category "Round"
                              :label round-id}}}))

(rf/reg-event-fx
 :b/round.disqualify
 (fn [{:keys [db]} [_ round-id product-id reason]]
   {:ws-send {:payload {:cmd :b/round.declare-result
                        :round-id round-id
                        :product-id product-id
                        :result 0
                        :reason reason
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Disqualify Product"
                      :props {:category "Round"
                              :label round-id}}}))

(rf/reg-event-fx
 :b/round.undo-disqualify
 (fn [{:keys [db]} [_ round-id product-id]]
   {:ws-send {:payload {:cmd :b/round.declare-result
                        :round-id round-id
                        :product-id product-id
                        :result nil
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Undo Disqualify Product"
                      :props {:category "Round"
                              :label round-id}}}))

(rf/reg-event-fx
 :b/round.ask-a-question
 (fn [{:keys [db]} [_ product-id product-name message
                    round-id requirement-text]]
   {:ws-send {:payload {:cmd :b/ask-a-question
                        :return {:handler :b/ask-a-question-return}
                        :product-id product-id
                        :message message
                        :round-id round-id
                        :requirement-text requirement-text
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Ask A Question"
                      :props {:category "Round"
                              :label product-name}}}))

(rf/reg-event-fx
 :b/round.rate-response
 (fn [{:keys [db]} [_ response-id rating]]
   {:ws-send {:payload {:cmd :save-response
                        :subject response-id
                        :subject-type "response"
                        :term :round.response/rating
                        :fields {:value rating}
                        :user-id (-> db :user :id)
                        :org-id (util/db->current-org-id db)}}
    :analytics/track {:event "Rate Response"
                      :props {:category "Round"
                              :label rating}}}))

;; Subscriptions
(rf/reg-sub
 :round-idstr
 :<- [:page-params] 
 (fn [{:keys [round-idstr]}] round-idstr))

;; Components
(defn get-requirements-options []
  (util/as-dropdown-options [;; "Subscription Billing" "Free Trial"
                             ]))

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
                           :options (util/as-dropdown-options
                                     ["Within 2 Weeks" "Within 3 Weeks" "Within 1 Month"
                                      "Within 2 Months" "Within 6 Months" "Within 12 Months"])
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

(defn c-round-initiation
  [{:keys [id status title products init-doc] :as round}]
  (if init-doc
    "You have already submitted your requirements." ; this should never show
    [c-round-initiation-form id]))


(defn c-declare-winner-button
  [round product result]
  [:> ui/Popup
   {:content (if (= 1 result) "Declared Winner" "Declare Winner")
    ;; :header "Decision"
    :position "bottom left"
    :trigger (r/as-element
              [:> ui/Button {:icon "checkmark"
                             :color (if (= 1 result)
                                      "white"
                                      "lightblue"
                                      ;; "vetd-gradient"
                                      )
                             :onClick #(rf/dispatch [:b/round.declare-winner (:id round) (:id product)])
                             :size "mini"
                             :disabled (not (nil? result))}])}])

(defn c-setup-call-button
  [round product vendor result]
  [:> ui/Popup
   {:content (str "Set up call with " (:oname vendor))
    ;; :header "Set Up a Call"
    :position "bottom left"
    :trigger (r/as-element
              [:> ui/Button {:icon "call"
                             :color (if (= 1 result)
                                      "white"
                                      "lightteal"
                                      ;; "lightblue"
                                      )
                             :onClick #(rf/dispatch [:b/setup-call (:id product) (:pname product)])
                             :size "mini"
                             :disabled (= 0 result)}])}])

(defn c-disqualify-button
  [round product result]
  (let [popup-open? (r/atom false)]
    (fn [round product result]
      (if-not (= 0 result)
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
                    [:> ui/Popup
                     {:content "Disqualify"
                      :position "bottom center"
                      :trigger (r/as-element
                                [:> ui/Button {:icon "ban"
                                               :basic true
                                               :size "mini"
                                               :disabled (= 1 result)
                                               :on-click #(swap! popup-open? not)}])}])}]
        [:> ui/Popup
         {:content "Undo Disqualify"
          :position "bottom center"
          :trigger (r/as-element
                    [:> ui/Button {:icon "undo"
                                   :basic true
                                   :on-click #(rf/dispatch [:b/round.undo-disqualify (:id round) (:id product)])
                                   :size "mini"}])}]))))

(defn c-action-button
  "Component to display small icon button for grid cell actions."
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

(defn c-no-response []
  [:> ui/Popup
   {:content "Vendor did not respond"
    :position "bottom center"
    :trigger (r/as-element
              [:> ui/Icon {:name "ban"
                           :size "large"
                           :style {:color "#aaa"}}])}])

(defn c-waiting-for-response []
  [:> ui/Popup
   {:content "Waiting for vendor response"
    :position "bottom center"
    :trigger (r/as-element
              [:> ui/Icon {:name "clock outline"
                           :size "large"
                           :style {:color "#aaa"}}])}])

(defn c-cell-modal
  [round-id
   modal-showing?&
   modal-response&]
  (let [modal-message& (r/atom "")]
    (fn [round-id
         modal-showing?&
         modal-response&]
      (let [{:keys [req-prompt-id req-prompt-text product-id pname
                    resp-id resp-text resp-rating]
             :as modal-response} @modal-response&]
        [:> ui/Modal {:open @modal-showing?&
                      :on-close #(reset! modal-showing?& false)
                      :size "tiny"
                      :dimmer "inverted"
                      :closeOnDimmerClick true
                      :closeOnEscape true
                      :closeIcon true} 
         [:> ui/ModalHeader pname]
         [:> ui/ModalContent {:class (str (when (= 1 resp-rating) "response-approved ")
                                          (when (= 0 resp-rating) "response-disapproved "))}
          [:h4 {:style {:padding-bottom 10}}
           [c-action-button {:on-click #(rf/dispatch [:b/round.rate-response resp-id 0])
                             :icon "thumbs down outline"
                             :popup-text (if (= 0 resp-rating) "Disapproved" "Disapprove")
                             :props {:class "disapprove"
                                     :style {:float "right"
                                             :margin-right 0}}}]
           [c-action-button {:on-click #(rf/dispatch [:b/round.rate-response resp-id 1])
                             :icon "thumbs up outline"
                             :popup-text (if (= 1 resp-rating) "Approved" "Approve")
                             :props {:class "approve"
                                     :style {:float "right"
                                             :margin-right 4}}}]
           req-prompt-text]
          (if resp-text
            (util/parse-md resp-text)
            "Waiting for vendor response.")]
         [:> ui/ModalActions
          [:> ui/Form
           [:> ui/FormField
            [:> ui/TextArea {:placeholder "Ask a follow-up question..."
                             :autoFocus true
                             :spellCheck true
                             :onChange (fn [_ this]
                                         (reset! modal-message& (.-value this)))}]]
           [:> ui/Button {:onClick #(reset! modal-showing?& false)
                          :color "grey"}
            "Cancel"]
           [:> ui/Button
            {:onClick #(do (rf/dispatch
                            [:b/round.ask-a-question
                             product-id pname @modal-message& round-id req-prompt-text])
                           (reset! modal-showing?& false))
             :color "blue"}
            "Submit Question"]]]]))))

(def cell-click-disabled? (r/atom false))
(def reverse-scroll-drag? (r/atom false))
;; product id's in sort order
(def products-order& (r/atom []))

(defn c-column*
  [index
   {:keys [id status] :as round}
   {:keys [prompts] :as req-form-template}
   rp
   show-modal-fn]
  (let [{pname :pname
         product-id :id
         product-idstr :idstr
         vendor :vendor
         preposals :docs
         :as product} (:product rp)
        product-disqualified? (= 0 (:result rp))]
    [:div.column {:data-product-id product-id
                  :style {:left (str (* index 234) "px")}}
     [:div {:class (str "round-product"
                        (when (> (count pname) 17) " long")
                        (when (= 1 (:result rp)) " winner")
                        (when (= 0 (:result rp)) " disqualified"))}
      [:div
       [:a.name {:on-click #(rf/dispatch
                             (if (seq preposals)
                               [:b/nav-preposal-detail (-> preposals first :idstr)]
                               [:b/nav-product-detail product-idstr]))}
        pname]
       [c-declare-winner-button round product (:result rp)]
       [c-setup-call-button round product vendor (:result rp)]
       [c-disqualify-button round product (:result rp)]]]
     (for [req prompts
           :let [{req-prompt-id :id
                  req-prompt-text :prompt} req
                 response-prompts (-> rp :vendor-response-form-docs first :response-prompts)
                 response-prompt (docs/get-response-prompt-by-prompt-id
                                  response-prompts
                                  req-prompt-id)
                 resp-rating (some-> response-prompt
                                     :subject-of-response-prompt
                                     first
                                     :response-prompt-fields
                                     first
                                     :nval)
                 resp (docs/get-response-field-by-prompt-id response-prompts req-prompt-id)
                 {id :id
                  resp-id :resp-id
                  resp-text :sval} resp]]
       ^{:key (str req-prompt-id "-" product-id)}
       [:div.cell {:class (str (when product-disqualified? "disqualified" )
                               (when (= 1 resp-rating) "response-approved ")
                               (when (= 0 resp-rating) "response-disapproved "))
                   :on-mouse-down #(reset! cell-click-disabled? false)
                   :on-click #(when-not @cell-click-disabled?
                                (show-modal-fn {:req-prompt-id req-prompt-id
                                                :req-prompt-text req-prompt-text
                                                :product-id product-id
                                                :pname pname
                                                :resp-id resp-id
                                                :resp-text resp-text
                                                :resp-rating resp-rating}))}
        [:div.text (if (not-empty resp-text)
                     (util/truncate-text resp-text 150)
                     (if (= status "complete")
                       [c-no-response]
                       [c-waiting-for-response]))]
        [:div.actions
         [c-action-button {:props {:class "action-button question"}
                           :icon "chat outline" ; on-click just pass through
                           :popup-text "Ask Question"}]
         [c-action-button {:props {:class "action-button approve"}
                           :on-click #(do (.stopPropagation %)
                                          (rf/dispatch [:b/round.rate-response resp-id 1]))
                           :icon "thumbs up outline"
                           :popup-text (if (= 1 resp-rating) "Approved" "Approve")}]
         [c-action-button {:props {:class "action-button disapprove"}
                           :on-click #(do (.stopPropagation %)
                                          (rf/dispatch [:b/round.rate-response resp-id 0]))
                           :icon "thumbs down outline"
                           :popup-text (if (= 0 resp-rating) "Disapproved" "Disapprove")}]]])]))

(def c-column
  (with-meta c-column*
    {:component-did-mount
     (fn [this] ; make columns draggable to reorder
       (let [node (r/dom-node this)
             mousedown? (atom false)
             x-at-mousedown (atom nil)
             col (atom nil)
             col-pos-x-at-mousedown (atom nil)
             last-x-displacement (atom 0)
             part-of-drag-handle? (fn [dom-node]
                                    (or (.contains (.-classList dom-node) "round-product")
                                        (empty? (array-seq (.-classList dom-node)))))
             mousedown (fn [e]
                         (when (part-of-drag-handle? (.-target e))
                           (reset! col node)
                           (reset! mousedown? true)
                           (reset! x-at-mousedown (.-pageX e))
                           (reset! col-pos-x-at-mousedown (js/parseInt (.-left (.-style node))))
                           (reset! reverse-scroll-drag? true)
                           (aset (.-style @col) "transform" (str "scale(1.01)"))
                           (.add (.-classList node) "reordering")))
             mousemove (fn [e]
                         (when @mousedown?
                           (let [x-displacement (- (.-pageX e) @x-at-mousedown)]
                             (.preventDefault e)
                             (aset (.-style @col) "transform" (str "scale(1.01)"))
                             (aset (.-style @col) "left" (str (+ @col-pos-x-at-mousedown
                                                                 (* 1 x-displacement)) "px"))
                             #_(aset (.-style @col) "left" (str (+ @col-pos-x-at-mousedown
                                                                   (* 2 x-displacement)) "px"))
                             (when (> x-displacement 110)
                               (println "do a swap")
                               (let [product-id (js/parseInt (.getAttribute @col "data-product-id"))
                                     old-index (.indexOf @products-order& product-id)
                                     new-index 1] ; zero-based
                                 (swap! products-order&
                                        assoc
                                        old-index (@products-order& new-index)
                                        new-index product-id))))))
             mouseup-or-leave (fn [e]
                                (when @col
                                  (reset! mousedown? false)
                                  (aset (.-style @col) "transform" (str "scale(1)"))
                                  (let [product-id (js/parseInt (.getAttribute @col "data-product-id"))
                                        index (.indexOf @products-order& product-id)]
                                    (aset (.-style @col) "left" (str (* 234 index) "px")))
                                  (.remove (.-classList node) "reordering")))
             ]
         (.addEventListener node "mousedown" mousedown)
         (.addEventListener node "mousemove" mousemove)
         (.addEventListener node "mouseup" mouseup-or-leave)
         (.addEventListener node "mouseleave" mouseup-or-leave)))}))

(defn c-round-grid*
  [round req-form-template round-product]
  (let [ ;; The response currently in the modal.
        ;; E.g., {:req-prompt-id 123
        ;;        :req-prompt-text "Something"
        ;;        :product-id 321
        ;;        :pname "Some Product"
        ;;        :resp-id 456
        ;;        :resp-text "Some answer to prompt"
        ;;        :resp-rating 1}
        modal-response& (r/atom {})
        modal-showing?& (r/atom false)
        show-modal-fn (fn [response]
                        (reset! modal-response& response)
                        (reset! modal-showing?& true))
        _ (when (empty? @products-order&)
            (reset! products-order& (into [] (map (comp :id :product) round-product))))]
    (fn [round req-form-template round-product]
      (if (seq round-product)
        [:<>
         [:div.round-grid {:style {:height (+ 60
                                              (* 201 (-> req-form-template :prompts count)))}}
          (map-indexed
           (fn [index product-id]
             (let [rp (first (filter #(= product-id (-> % :product :id)) round-product))]
               ^{:key (-> rp :product :id)}
               [c-column index round req-form-template rp show-modal-fn]))
           @products-order&)]
         [c-cell-modal (:id round) modal-showing?& modal-response&]]
        ;; no products in round yet
        [:> ui/Segment {:class "detail-container"
                        :style {:margin-left 20}}
         [:p [:em "Your requirements have been submitted."]]
         [:p (str "We are gathering information for you to review "
                  "from all relevant vendors. Check back soon for updates.")]]))))

(def c-round-grid
  (let [;; keep a reference to the window-scroll fn (will be created on mount)
        ;; so we can remove the event listener upon unmount
        window-scroll-fn-ref (atom nil)
        ;; really just affects which cursor displayed
        update-draggability
        (fn [this]
          (let [node (r/dom-node this)]
            (if (> (.-scrollWidth node) (.-clientWidth node))
              (.add (.-classList (r/dom-node this)) "draggable")
              (.remove (.-classList (r/dom-node this)) "draggable"))))]
    (with-meta c-round-grid*
      {:component-did-mount
       (fn [this] ; make grid draggable to scroll horizontally
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
                                                      (* (if @reverse-scroll-drag? 0 3)
                                                         x-displacement)
                                                      #_(* (if @reverse-scroll-drag? -1 3)
                                                           x-displacement))]
                               (.preventDefault e)
                               (aset node "scrollLeft" new-scroll-left)
                               ;; if you drag more than 3px, disable the cell clickability
                               (when (and (> (Math/abs x-displacement) 3)
                                          (not @cell-click-disabled?))
                                 (reset! cell-click-disabled? true)))))
               mouseup (fn [e]
                         (.remove (.-classList node) "dragging")
                         (reset! reverse-scroll-drag? false)
                         (reset! mousedown? false))
               
               ;; make header row 'sticky' upon window scroll
               header-pickup-y (atom nil) ; nil when not in 'sticky mode'
               all-header-nodes #(array-seq (.getElementsByClassName js/document "round-product"))
               ;; the horizontal position of the requirement row needs to
               ;; be manually updated when in 'sticky mode'
               scroll (fn []
                        (.requestAnimationFrame
                         js/window
                         (fn []
                           (when @header-pickup-y
                             (doseq [req-node (all-header-nodes)]
                               (aset (.-style req-node) "marginLeft" (str (* -1 (.-scrollLeft node)) "px")))))))
               ;; zero out the artificial horizontal scrolling of the header row
               ;; this needs to be called when we leave 'sticky mode'
               zero-out-req-scroll (fn []
                                     (doseq [req-node (all-header-nodes)]
                                       (aset (.-style req-node) "marginLeft" "0px")))
               ;; turn on and off header row 'sticky mode' as needed
               window-scroll (fn []
                               (.requestAnimationFrame
                                js/window
                                (fn []
                                  (if @header-pickup-y
                                    (when (< (.-scrollY js/window) @header-pickup-y)
                                      (reset! header-pickup-y nil)
                                      (.remove (.-classList node) "fixed")
                                      (zero-out-req-scroll))
                                    (when (> (.-scrollY js/window) (.-offsetTop node))
                                      (reset! header-pickup-y (.-offsetTop node))
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
           (.addEventListener js/window "scroll" window-scroll)
           (update-draggability this)))

       :component-did-update
       (fn [this]
         (update-draggability this))

       :component-will-unmount
       (fn [this]
         (when @window-scroll-fn-ref
           (.removeEventListener js/window "scroll" @window-scroll-fn-ref)))})))

(defn c-round
  "Component to display Round details."
  [{:keys [id status title products] :as round}
   req-form-template
   round-product]
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
       "complete"} [c-round-grid round req-form-template round-product])])

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
                                      :placeholder "Enter topic..."
                                      :search true
                                      :selection true
                                      :multiple false
                                      :selectOnBlur false
                                      :allowAdditions true
                                      :additionLabel "Hit 'Enter' to Add "
                                      :noResultsMessage "Type to add a new topic..."
                                      :onChange (fn [_ this]
                                                  (reset! popup-open? false)
                                                  (rf/dispatch
                                                   [:b/round.add-requirement id (.-value this)]))}]]))
        :trigger (r/as-element
                  [:> ui/Button {:color "teal"
                                 ;; :basic true
                                 :size "mini"
                                 :icon "plus"
                                 :style {:position "relative"
                                         :top -4
                                         :left 5}
                                 }]
                  #_[:> ui/Button {:color "teal"
                                   :icon true
                                   :fluid true
                                   :labelPosition "left"}
                     "Add a Topic"
                     [:> ui/Icon {:name "plus"}]])}])))

(defn c-products
  "Component to display product boxes with various buttons."
  [round round-product]
  [:<>
   (for [rp round-product
         :let [{product-id :id
                product-idstr :idstr
                pname :pname
                vendor :vendor
                preposals :docs
                :as product} (:product rp)]]
     ^{:key product-id}
     [:> ui/Segment {:class (str "round-product"
                                 (when (> (count pname) 17) " long")
                                 (when (= 1 (:result rp)) " winner")
                                 (when (= 0 (:result rp)) " disqualified"))}
      [:a.name {:on-click #(rf/dispatch
                            (if (seq preposals)
                              [:b/nav-preposal-detail (-> preposals first :idstr)]
                              [:b/nav-product-detail product-idstr]))}
       pname]
      [c-declare-winner-button round product (:result rp)]
      [c-setup-call-button round product vendor (:result rp)]
      [c-disqualify-button round product (:result rp)]])])

(defn c-requirements
  [{:keys [prompts] :as req-form-template}]
  [:div
   (for [req prompts
         :let [{req-prompt-id :id
                req-prompt-text :prompt} req]]
     ^{:key (str req-prompt-id)}
     [:> ui/Segment {:class "requirement"}
      req-prompt-text])])

(defn sort-round-products
  [round-product] ; if result is nil, then sort it in-between winner and disqualified
  (sort-by (juxt #(or (:result %) 0.5) (comp :pname :product)) > round-product))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        round-idstr& (rf/subscribe [:round-idstr])
        rounds& (rf/subscribe [:gql/sub
                               {:queries
                                [[:rounds {:idstr @round-idstr&
                                           :deleted nil}
                                  [:id :idstr :created :status :title
                                   ;; requirements form template
                                   [:req-form-template
                                    [:id
                                     [:prompts {:ref-deleted nil
                                                :_order_by {:sort :asc}}
                                      [:id :idstr :prompt :descr]]]]
                                   ;; round initiation form response
                                   [:init-doc
                                    [:id
                                     [:response-prompts {:ref-deleted nil}
                                      [:id :prompt-id :prompt-prompt :prompt-term
                                       [:response-prompt-fields
                                        [:id :prompt-field-fname :idx
                                         :sval :nval :dval]]]]]]
                                   ;; requirements responses from vendors
                                   [:round-product {:deleted nil}
                                    [:id :result :reason
                                     [:product
                                      [:id :idstr :pname
                                       [:docs {:dtype "preposal" ; completed preposals
                                               :to-org-id @org-id&}
                                        [:id :idstr]]
                                       [:vendor
                                        [:id :oname]]]]
                                     [:vendor-response-form-docs
                                      [:id :title :doc-id :doc-title
                                       :ftype :fsubtype
                                       [:doc-from-org [:id :oname]]
                                       [:doc-to-org [:id :oname]]
                                       [:response-prompts {:ref-deleted nil}
                                        [:id :prompt-id :prompt-prompt :prompt-term
                                         [:response-prompt-fields
                                          [:id :prompt-field-fname :idx :resp-id
                                           :sval :nval :dval]]
                                         [:subject-of-response-prompt
                                          {:deleted nil
                                           :prompt-term "round.response/rating"}
                                          [[:response-prompt-fields
                                            {:deleted nil}
                                            [:nval]]]]]]]]]]]]]}])]
    (fn []
      [:div.container-with-sidebar.round-details
       (if (= :loading @rounds&)
         [cc/c-loader]
         (let [{:keys [status req-form-template round-product] :as round} (-> @rounds& :rounds first)
               sorted-round-products (sort-round-products round-product)]
           [:<> ; sidebar margins (and detail container margins) are customized on this page
            [:div.sidebar {:style {:margin-right 0}}
             [:div {:style {:padding "0 15px"}}
              [bc/c-back-button {:on-click #(rf/dispatch [:b/nav-rounds])}
               "All VetdRounds"]]
             [:div {:style {:height 186}}] ; spacer
             (when (and (#{"in-progress" "complete"} status)
                        (seq sorted-round-products))
               [:<>
                [:div {:style {:padding "0 15px"}}
                 (if (some (comp (partial = 1) :result) sorted-round-products)
                   [:div {:style {:height 36}}]
                   [:div {:class "requirements-heading"}
                    "Topics "
                    [c-add-requirement-button round]])]
                [c-requirements req-form-template]])]
            [:div.inner-container [c-round round req-form-template sorted-round-products]]]))])))
