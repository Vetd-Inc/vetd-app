(ns vetd-app.buyers.pages.round-detail.grid
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.pages.round-detail.initiation :as initiation]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(defonce cell-click-disabled? (r/atom false))
;; the id of the product currently being reordered
(defonce reordering-product (r/atom nil))
;; the current x position of a column being reordered
(defonce curr-reordering-pos-x (r/atom nil))
;; includes any spacing to the right of each col
(defonce col-width 234) 

;;;; Subscriptions
(rf/reg-sub
 :b/round
 (fn [{:keys [round]}] round))

(rf/reg-sub
 :b/products-order
 :<- [:b/round]
 (fn [{:keys [products-order]}] products-order))

;;;; Events
(rf/reg-event-fx
 :b/round.add-requirements
 (fn [{:keys [db]} [_ round-id requirements]]
   {:ws-send {:payload {:cmd :b/round.add-requirements
                        :return {:handler :b/round.add-requirements-return
                                 :requirements requirements}
                        :round-id round-id
                        :requirements requirements
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Add Requirements"
                      :props {:category "Round"
                              :label round-id}}}))

(rf/reg-event-fx
 :b/round.add-requirements-return
 (fn [{:keys [db]} [_ _ {{:keys [requirements]} :return}]]
   (let [multiple-requirements? (> (count requirements) 1)]
     {:toast {:type "success"
              :title (str "Topic" (when multiple-requirements? "s")
                          " Added to VetdRound")
              :message "We will request responses from every vendor in the VetdRound."}})))

(rf/reg-event-fx
 :b/round.add-products
 ;; "products" is a mixed coll of product id's[number] (for adding products that exist),
 ;; and product names[strings] (for adding products that don't exist in our DB yet)
 (fn [{:keys [db]} [_ round-id products]]
   (let [product-ids (filter number? products)
         product-names (filter string? products)]
     {:ws-send {:payload {:cmd :b/round.add-products
                          :return {:handler :b/round.add-products-return
                                   :product-ids product-ids
                                   :product-names product-names}
                          :round-id round-id
                          :product-ids product-ids
                          :product-names product-names
                          :buyer-id (util/db->current-org-id db)}}
      :analytics/track {:event "Add Products"
                        :props {:category "Round"
                                :label round-id}}})))

(rf/reg-event-fx
 :b/round.add-products-return
 (fn [{:keys [db]} [_ _ {{:keys [product-ids product-names]} :return}]]
   (let [multiple-products? (> (+ (count product-ids) (count product-names)) 1)
         all-products-known? (empty? product-names)]
     {:toast (if all-products-known?
               {:type "success"
                :title (str "Product" (when multiple-products? "s") " Added to VetdRound")}
               {:type "warning"
                :title (if multiple-products? "Some Products Not Addded Yet" "Product Not Added Yet")
                :message (str "We don't recognize the following products but will look into adding them soon: "
                              (s/join ", " product-names))})})))

(rf/reg-event-fx
 :b/round.declare-winner
 (fn [{:keys [db]} [_ round-id product-id reason]]
   {:ws-send {:payload {:cmd :b/round.declare-result
                        :round-id round-id
                        :product-id product-id
                        :result 1
                        :reason reason
                        :buyer-id (util/db->current-org-id db)}}
    :confetti nil
    :analytics/track {:event "Declare Winner"
                      :props {:category "Round"
                              :label round-id}}}))

(rf/reg-event-fx
 :b/round.disqualify
 (fn [{:keys [db]} [_ round-id product-id reason]]
   {:ws-send {:payload {:cmd :b/round.declare-result
                        :return {:handler :b/round.declare-result-return
                                 :round-id round-id
                                 :product-id product-id}
                        :round-id round-id
                        :product-id product-id
                        :result 0
                        :reason reason
                        :buyer-id (util/db->current-org-id db)}}}))

(rf/reg-event-fx
 :b/round.declare-result-return
 (fn [{:keys [db]} [_ _ {{:keys [round-id product-id]} :return}]]
   {:toast {:type "success"
            :title "Product Disqualified from VetdRound"}
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
                        :reason nil
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Undo Disqualify Product"
                      :props {:category "Round"
                              :label round-id}}}))

(rf/reg-event-fx
 :b/round.ask-a-question
 (fn [{:keys [db]} [_ product-id product-name message round-id requirement-text]]
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

;; sets round products sort order locally (app-db)
(rf/reg-event-fx
 :b/set-products-order
 (fn [{:keys [db]} [_ new-products-order]]
   {:db (assoc-in db [:round :products-order] new-products-order)}))

;; persist any changes to round products sort order to backend
(rf/reg-event-fx
 :b/store-products-order
 (fn [{:keys [db]} [_ round-id]]
   {:ws-send {:payload {:cmd :b/set-round-products-order
                        :product-ids (-> db :round :products-order)
                        :round-id round-id                        
                        :user-id (-> db :user :id)
                        :org-id (util/db->current-org-id db)}}}))

(rf/reg-event-fx
 :b/round.move-topic
 (fn [{:keys [db]} [_ direction round-id prompt-id prompts]]
   (let [curr-order (->> prompts (sort-by :sort) (mapv :id))
         swap-fn (if (= direction "up") dec inc)
         swap-idx (swap-fn (.indexOf curr-order prompt-id))]
     (when (< -1 swap-idx (count curr-order))
       {:dispatch [:b/round.store-topic-order
                   round-id
                   (replace {prompt-id (curr-order swap-idx)
                             (curr-order swap-idx) prompt-id} curr-order)]}))))

(rf/reg-event-fx
 :b/round.store-topic-order
 ;; topics-order is a vector of prompt-ids in correct order
 (fn [{:keys [db]} [_ round-id topics-order]]
   {:ws-send {:payload {:cmd :b/round.set-topic-order
                        :prompt-ids topics-order
                        :round-id round-id                        
                        :user-id (-> db :user :id)
                        :org-id (util/db->current-org-id db)}}}))

;;;; Components
(defn c-declare-winner-form
  [round product popup-open?]
  (let [new-reason (atom "")]
    (fn [round product popup-open?]
      [:> ui/Form {:style {:width 325}}
       [:h4 {:style {:margin-bottom 7}}
        "Declare Winner of VetdRound"]
       [:p {:style {:margin-top 0}}
        "We will put you in touch with " (:oname (:vendor product)) " as soon as possible."]
       [:> ui/FormField
        [:label "Why did you pick " (:pname product) "?"]
        [:> ui/Input
         {:placeholder "Enter a reason for your future reference..."
          :on-change (fn [_ this] (reset! new-reason (.-value this)))}]
        [:> ui/Button
         {:color "vetd-gradient"
          :fluid true
          :on-click (fn []
                      (reset! popup-open? false)
                      (rf/dispatch [:b/round.declare-winner
                                    (:id round) (:id product) @new-reason]))
          :style {:margin-top 7}}
         "Declare Winner"]]])))

(defn c-declare-winner-button
  [round product won? reason]
  (let [popup-open? (r/atom false)
        context-ref (r/atom nil)]
    (fn [round product won? reason]
      (if-not won?
        [:<>
         [:> ui/Popup
          {:position "top right"
           :on "click"
           :open @popup-open?
           :on-close #(reset! popup-open? false)
           :context @context-ref
           :content (r/as-element [c-declare-winner-form round product popup-open?])}]
         [:> ui/Popup
          {:content "Declare Winner"
           :position "bottom center"
           :context @context-ref
           :trigger (r/as-element
                     [:> ui/Button
                      {:icon "checkmark"
                       :color "lightblue"
                       :ref (fn [this] (reset! context-ref (r/dom-node this)))
                       :on-click #(swap! popup-open? not)
                       :size "mini"}])}]]
        ;; winner already declared
        [:> ui/Popup
         {:header "Declared Winner"
          :content reason
          :position "bottom center"
          :trigger (r/as-element
                    [:> ui/Button
                     {:icon true
                      :color "white"
                      :size "mini"
                      :style {:cursor "default"}}
                     [:<> [:> ui/Icon {:name "checkmark"}] " Winner"]])}]))))

(defn c-setup-call-button
  [product vendor won?]
  [:> ui/Popup
   {:content (str "Set up call with " (:oname vendor))
    :position "bottom center"
    :trigger (r/as-element
              [:> ui/Button
               {:icon "call"
                :color (if won? "white" "lightteal")
                :on-click #(rf/dispatch [:b/setup-call (:id product) (:pname product)])
                :size "mini"}])}])

(defn c-disqualify-form
  [round product popup-open?]
  (let [new-reason (atom "")]
    (fn [round product popup-open?]
      [:> ui/Form {:style {:width 400}}
       [:> ui/FormField
        [:> ui/Input
         {:placeholder "Enter reason..."
          :on-change (fn [_ this] (reset! new-reason (.-value this)))
          :action (r/as-element
                   [:> ui/Button
                    {:color "red"
                     :on-click (fn []
                                 (reset! popup-open? false)
                                 (rf/dispatch [:b/round.disqualify
                                               (:id round)
                                               (:id product)
                                               @new-reason]))}
                    "Disqualify"])}]]])))

(defn c-disqualify-button
  [round product disqualified? reason]
  (let [popup-open? (r/atom false)
        context-ref (r/atom nil)]
    (fn [round product disqualified? reason]
      (if-not disqualified?
        [:<>
         [:> ui/Popup
          {:position "top right"
           :on "click"
           :open @popup-open?
           :on-close #(reset! popup-open? false)
           :context @context-ref
           :content (r/as-element [c-disqualify-form round product popup-open?])}]
         [:> ui/Popup
          {:content "Disqualify"
           :position "bottom center"
           :context @context-ref
           :trigger (r/as-element
                     [:> ui/Button {:icon "ban"
                                    :basic true
                                    :ref (fn [this] (reset! context-ref (r/dom-node this)))
                                    :size "mini"
                                    :on-click #(swap! popup-open? not)}])}]]
        [:> ui/Popup
         {:header "Product Disqualified"
          :content (r/as-element
                    [:div (when (bc/has-data? reason)
                            [:<> reason [:br] [:br]])
                     "Click to undo disqualification."])
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

(defn c-response-status-icon
  [{:keys [popup-text icon-name]}]
  [:> ui/Popup
   {:content popup-text
    :position "bottom center"
    :trigger (r/as-element
              [:> ui/Icon {:name icon-name
                           :size "large"
                           :style {:color "#aaa"}}])}])

(defn c-no-response []
  [c-response-status-icon
   {:popup-text "Vendor did not respond"
    :icon-name "ban"}])

(defn c-waiting-for-response []
  [c-response-status-icon
   {:popup-text "Waiting for vendor response"
    :icon-name "clock outline"}])

(defn c-cell-modal
  [round-id modal-showing?& modal-response&]
  (let [modal-message& (r/atom "")]
    (fn [round-id modal-showing?& modal-response&]
      (let [{:keys [req-prompt-id req-prompt-text
                    product-id pname
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
          [:> ui/Form {:as "div"
                       :style {:padding-bottom "1rem"}}
           [:> ui/FormField
            [:> ui/TextArea {:placeholder "Ask a follow-up question..."
                             :autoFocus true
                             :spellCheck true
                             :onChange (fn [_ this]
                                         (reset! modal-message& (.-value this)))}]]]
          [:> ui/Button {:onClick #(reset! modal-showing?& false)}
           "Cancel"]
          [:> ui/Button
           {:onClick #(do (rf/dispatch
                           [:b/round.ask-a-question
                            product-id pname @modal-message& round-id req-prompt-text])
                          (reset! modal-showing?& false))
            :color "blue"}
           "Submit Question"]]]))))

(defn c-cell-text
  [resp-text round-status]
  [:div.text
   (if (not-empty resp-text)
     (util/truncate-text resp-text 120)
     (if (= round-status "complete")
       [c-no-response]
       [c-waiting-for-response]))])

(defn c-cell-actions
  [resp-id resp-rating round-id prompt-id prompts]
  (let [admin?& (rf/subscribe [:admin?]) 
        rating-approved 1
        rating-disapproved 0]
    (fn [resp-id resp-rating round-id prompt-id prompts]
      [:div.actions
       [c-action-button {:props {:class "action-button"}
                         :icon "chat outline" ; no on-click, let it pass through to underlying cell click
                         :popup-text "Ask Question"}]
       [c-action-button {:props {:class "action-button approve"}
                         :on-click (fn [e]
                                     (.stopPropagation e)
                                     (rf/dispatch [:b/round.rate-response resp-id rating-approved]))
                         :icon "thumbs up outline"
                         :popup-text (if (= rating-approved resp-rating) "Approved" "Approve")}]
       [c-action-button {:props {:class "action-button disapprove"}
                         :on-click (fn [e]
                                     (.stopPropagation e)
                                     (rf/dispatch [:b/round.rate-response resp-id rating-disapproved]))
                         :icon "thumbs down outline"
                         :popup-text (if (= rating-disapproved resp-rating) "Disapproved" "Disapprove")}]
       (when @admin?&
         [:<>
          [c-action-button {:props {:class "action-button"}
                            :on-click (fn [e]
                                        (.stopPropagation e)
                                        (rf/dispatch [:b/round.move-topic "up" round-id prompt-id prompts]))
                            :icon "arrow up"
                            :popup-text "Move topic row up"}]
          [c-action-button {:props {:class "action-button"}
                            :on-click (fn [e]
                                        (.stopPropagation e)
                                        (rf/dispatch [:b/round.move-topic "down" round-id prompt-id prompts]))
                            :icon "arrow down"
                            :popup-text "Move topic row down"}]])])))

(defn c-column
  [round req-form-template rp show-modal-fn]
  (let [products-order& (rf/subscribe [:b/products-order])]
    (fn [{:keys [id status] :as round}
         {:keys [prompts] :as req-form-template}
         rp
         show-modal-fn]
      (let [{pname :pname
             product-id :id
             product-idstr :idstr
             vendor :vendor
             :as product} (:product rp)
            rp-result (:result rp)
            [won? disqualified?] ((juxt (partial = 1) zero?) rp-result)
            reason (:reason rp)]
        [:div.column {:class (str (when won? " winner")
                                  (when disqualified? " disqualified"))
                      :data-product-id product-id
                      :style {:transform
                              (str "translateX("
                                   (if (and @products-order& ; to trigger re-render
                                            (= @reordering-product product-id))
                                     @curr-reordering-pos-x
                                     (* col-width (.indexOf @products-order& product-id)))
                                   "px)")}}
         [:div.round-product {:class (when (> (count pname) 17) " long")}
          [:div
           [:a.name {:on-mouse-down #(reset! cell-click-disabled? false)
                     :on-click #(when-not @cell-click-disabled?
                                  (rf/dispatch [:b/nav-product-detail product-idstr]))}
            pname]
           (when-not disqualified?
             [c-declare-winner-button round product won? reason])
           (when-not disqualified?
             [c-setup-call-button product vendor won?])
           (when-not won?
             [c-disqualify-button round product disqualified? reason])]]
         (if (seq prompts)
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
                       
                       resps (docs/get-response-fields-by-prompt-id response-prompts req-prompt-id)
                       resp-id (-> resps first :resp-id)
                       resp-text (->> resps
                                      (map #(or (:sval %) (:nval %) (:dval %)))
                                      (apply str))]]
             ^{:key (str req-prompt-id "-" product-id)}
             [:div.cell {:class (str (when (= 1 resp-rating) "response-approved ")
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
              [:div.topic req-prompt-text]
              [c-cell-text resp-text status]
              [c-cell-actions resp-id resp-rating id req-prompt-id prompts]])
           [:div.cell [c-cell-text "Click \"Add Topics\" to learn more about this product." status]])]))))

(defn c-round-grid*
  [round req-form-template round-product show-top-scrollbar?]
  (let [;; The response currently in the modal.
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
        last-default-products-order& (atom [])]
    (fn [round req-form-template round-product show-top-scrollbar?]
      (let [default-products-order (vec (map (comp :id :product) round-product))]
        (when (not= @last-default-products-order& default-products-order)
          (reset! last-default-products-order& default-products-order)
          (rf/dispatch [:b/set-products-order default-products-order])))
      [:div.round-grid-container ; c-round-grid expects a certain order of children
       [:div.round-grid-top-scrollbar {:style {:display (if show-top-scrollbar? "block" "none")}}
        [:div {:style {:width (* col-width (count round-product))
                       :height 1}}]]
       [:div.round-grid {:style {:min-height (-> req-form-template
                                                 :prompts
                                                 count
                                                 (* 203)
                                                 (+ 122)
                                                 (max (+ 122 (* 1 203))))}}
        [:div {:style {:min-width (- (* col-width (count round-product))
                                     14)}}
         (for [rp round-product]
           ^{:key (-> rp :product :id)}
           [c-column round req-form-template rp show-modal-fn])]]
       [c-cell-modal (:id round) modal-showing?& modal-response&]])))

(defn cmp->child-nodes [cmp] (-> cmp r/dom-node .-children array-seq))
(def round-grid-cmp->round-grid-node (comp second cmp->child-nodes))
(def round-grid-cmp->top-scrollbar-node (comp first cmp->child-nodes))

(defn update-draggability
  [{{:keys [grid]} :nodes
    :keys [scroll] :as state}]
  (let [scroll-width (aget @grid "scrollWidth")
        client-width (aget @grid "clientWidth")
        modify-class-fn (if (> scroll-width client-width)
                          util/add-class
                          util/remove-class)]
    (reset! (:max-x scroll) (- scroll-width client-width))
    (modify-class-fn @grid "draggable")))

(defn all-header-nodes
  "Get all of the DOM nodes that are a header element of a column."
  []
  (util/nodes-by-class "round-product"))

;; this needs to be called when we leave 'sticky mode'
(defn zero-out-header-scroll []
  (doseq [header-node (all-header-nodes)]
    (aset (.-style header-node) "transform" "translateY(0px)")))

(defn part-of-drag-handle?
  "Is a given DOM node considered a handle (that can initiate
  a column drag) (for reordering columns)?"
  [node]
  (or (util/contains-class? node "round-product") ; top portion of column
      (util/contains-class? node "name") ; the product name
      (empty? (array-seq (.-classList node))))) ; the column node itself

(defn part-of-scrollbar?
  "Is a certain y position part of the scrollbar?"
  [node y]
  (> (- y (aget node "offsetTop"))
     (aget node "clientHeight")))

(defn col-node->product-id
  [node]
  (js/parseInt (.getAttribute node "data-product-id")))

(defn mousedown
  [{:keys [nodes mouse scroll drag products-order&] :as state} e]
  (let [page-x (.-pageX e)
        page-y (.-pageY e)
        target (.-target e)
        {:keys [grid reordering-col]} nodes]
    (reset! (:down? mouse) true)
    (reset! (:x mouse) page-x)
    (reset! (:x-pos-at-down mouse) page-x)
    (when (and (part-of-drag-handle? target)
               (> (count @products-order&) 1)) ; only able to reorder if more than one product
      ;; Reordering columns
      (when-let [col (.closest target ".column")] ; is the mousedown even on/in a column?
        (let [col-left (js/parseInt (re-find #"\d+" (.-transform (.-style col))))]
          (reset! reordering-col col)
          (reset! curr-reordering-pos-x col-left)
          (reset! (:direction-intention drag) nil)
          (reset! (:handle-offset drag) (- (+ (- page-x
                                                 (.-offsetLeft @grid))
                                              (.-scrollLeft @grid))
                                           col-left))
          ;; remember the id of the product we are currently reordering
          (reset! reordering-product (col-node->product-id col))
          (util/add-class col "reordering"))))
    ;; Scrolling by grabbing/dragging the grid
    (when-not (part-of-scrollbar? @grid page-y)
      (do (reset! (:grabbing? scroll) true)
          (util/add-class @grid "dragging")))
    (reset! (:last-x mouse) page-x)))

(defn window-scroll
  "Turn on and off header row 'sticky mode' as necessary."
  [{{:keys [grid]} :nodes
    {:keys [header-pickup-y]} :scroll
    :as state}]
  (.requestAnimationFrame
   js/window
   (fn []
     (let [window-scroll-y (.-scrollY js/window)
           node-offset-top (.-offsetTop @grid)]
       (if @header-pickup-y
         (if (< window-scroll-y @header-pickup-y)
           (do (reset! header-pickup-y nil)
               (util/remove-class @grid "fixed")
               (zero-out-header-scroll))
           (doseq [header-node (all-header-nodes)]
             (aset (.-style header-node) "transform"
                   (str "translateY(" (- window-scroll-y node-offset-top) "px)"))))
         (when (> window-scroll-y node-offset-top)
           (reset! header-pickup-y node-offset-top)
           (util/add-class @grid "fixed")))))))

(defn update-sort-pos
  "Based on the current x-axis position of the column being dragged,
  update the sort pos (if needed)."
  [{:keys [products-order&] :as state}]
  (let [old-index (.indexOf @products-order& @reordering-product)
        new-index (-> @curr-reordering-pos-x
                      (/ col-width) ; width of column
                      (+ 0.5) ; to cause swap to occur when middle of a col is passed
                      Math/floor
                      (max 0) ; clamp between 0 and max index
                      (min (dec (count @products-order&))))]
    (when (not= old-index new-index)
      (rf/dispatch [:b/set-products-order
                    (assoc @products-order&
                           old-index (@products-order& new-index)
                           new-index @reordering-product)]))))

(defn mousemove
  [{:keys [mouse scroll drag] :as state} e]
  (let [page-x (.-pageX e)]
    (reset! (:x mouse) page-x)
    (when @(:down? mouse)
      ;; this seems to prevent cell text selection when scrolling
      (.preventDefault e)
      ;; if you drag more than 3px, disable the cell & product name clickability
      (when (and (not @cell-click-disabled?)
                 (> (Math/abs (- @(:x mouse) @(:x-pos-at-down mouse))) 3))
        (reset! cell-click-disabled? true))
      ;; useful for determining if right or left edge scrolling is needed
      (reset! (:last-delta-x mouse) (- @(:x mouse) @(:last-x mouse)))
      (when-not (zero? @(:last-delta-x mouse))
        (reset! (:direction-intention drag) (if (pos? @(:last-delta-x mouse)) "right" "left")))
      ;; scrolling
      (when-not @reordering-product
        (let [neg-disp (* -1 (- page-x @(:last-x mouse)))]
          (swap! (:x scroll) + neg-disp)
          (reset! (:velocity-x scroll) (* neg-disp (:acceleration-factor scroll)))))
      (reset! (:last-x mouse) @(:x mouse)))))

(defn mouseup
  [{:keys [round-id nodes mouse scroll] :as state} e]
  (reset! (:x mouse) (.-pageX e))
  (when @(:down? mouse)
    (reset! (:down? mouse) false)
    (reset! (:last-delta-x mouse) 0)
    (when @reordering-product
      (do (util/remove-class @(:reordering-col nodes) "reordering")
          (reset! reordering-product nil)
          (rf/dispatch [:b/store-products-order @round-id])))
    (reset! (:grabbing? scroll) false)
    (util/remove-class @(:grid nodes) "dragging")))

(defn scroll
  [{:keys [nodes scroll] :as state}]
  (when-not @(:grabbing? scroll)
    (let [scroll-left (.-scrollLeft @(:grid nodes))]
      (when (> (Math/abs (- scroll-left @(:x scroll))) 0.99999)
        (reset! (:velocity-x scroll) 0)
        (reset! (:x scroll) scroll-left))))
  (aset @(:top-scrollbar nodes) "scrollLeft" (Math/floor @(:x scroll))))

(defn scroll-top-scrollbar
  [{:keys [nodes scroll] :as state}]
  (when @(:hovering-top-scrollbar? scroll)
    (when-not @(:grabbing? scroll)
      (let [scroll-left (.-scrollLeft @(:top-scrollbar nodes))]
        (when (> (Math/abs (- scroll-left @(:x scroll))) 0.99999)
          (reset! (:velocity-x scroll) 0)
          (reset! (:x scroll) scroll-left)
          (aset @(:grid nodes) "scrollLeft" (Math/floor @(:x scroll))))))))

(defn mouseover-top-scrollbar
  [{{:keys [hovering-top-scrollbar?]} :scroll
    :as state}]
  (reset! hovering-top-scrollbar? true))

(defn mouseleave-top-scrollbar
  [{{:keys [hovering-top-scrollbar?]} :scroll
    :as state}]
  (reset! hovering-top-scrollbar? false))

(defn animation-loop
  [{:keys [mounted? nodes mouse scroll drag] :as state} timestamp]
  (do
    ;; override scroll velocity if reordering
    (when (and @reordering-product
               (= @(:direction-intention drag) "right")
               (not (neg? @(:last-delta-x mouse)))
               (> (- @(:x mouse)
                     (.-offsetLeft @(:grid nodes)))
                  (- (.-clientWidth @(:grid nodes))
                     col-width)))
      (reset! (:velocity-x scroll) (:speed-reordering scroll)))
    (when (and @reordering-product
               (= @(:direction-intention drag) "left")
               (not (pos? @(:last-delta-x mouse)))
               (< (- @(:x mouse)
                     (.-offsetLeft @(:grid nodes)))
                  col-width))
      (reset! (:velocity-x scroll) (* -1 (:speed-reordering scroll))))    
    ;; apply scroll velocity to scroll position
    (swap! (:x scroll) + @(:velocity-x scroll))
    ;; right-side boundary
    (when (> @(:x scroll) @(:max-x scroll))
      (reset! (:velocity-x scroll) 0)
      (reset! (:x scroll) @(:max-x scroll)))
    ;; left-side boundary
    (when (< @(:x scroll) 0)
      (reset! (:velocity-x scroll) 0)
      (reset! (:x scroll) 0))
    ;; apply position updates
    (when @(:grabbing? scroll) ; drag scrolling is not the same as reordering scrolling
      (aset @(:grid nodes) "scrollLeft" (Math/floor @(:x scroll))))
    (when @reordering-product
      (reset! curr-reordering-pos-x (- (+ (- @(:x mouse)
                                             (.-offsetLeft @(:grid nodes)))
                                          (.-scrollLeft @(:grid nodes)))
                                       @(:handle-offset drag)))
      (update-sort-pos state))
    ;; apply friction
    (if @reordering-product
      (swap! (:velocity-x scroll) * 0)
      (swap! (:velocity-x scroll) * @(:friction scroll)))
    ;; zero out weak velocity
    (if (< (Math/abs @(:velocity-x scroll)) 0.000001)
      (reset! (:velocity-x scroll) 0))
    (when @mounted?
      (js/requestAnimationFrame (partial animation-loop state)))))

(def c-round-grid
  (let [state {:mounted? (atom false) ;; is this component mounted?
               :round-id (atom nil)
               :nodes {:grid (atom nil) ;; round grid DOM node
                       :top-scrollbar (atom nil) ;; top scrollbar DOM node
                       :reordering-col (atom nil)} ;; the DOM node of the column we are reordering (if any)
               :mouse {:x (atom nil) ;; current mouse x position
                       :y (atom nil) ;; current mouse y position
                       :last-x (atom nil) ;; last mouse x position (from the previous animation loop step)
                       :last-y (atom nil) ;; last mouse y position (from the previous animation loop step)
                       ;; the delta from mouse position two steps ago and the previous step
                       :last-delta-x (atom 0) ;; last mouse x delta (from the previous animation loop step)
                       :last-delta-y (atom 0)
                       :down? (atom false) ;; is the mouse button currently down?
                       :x-pos-at-down (atom nil) ;; what was the x position of the most recent mousedown
                       :y-pos-at-down (atom nil)} ;; what was the y position of the most recent mousedown
               :scroll {:x (atom 0) ;; the grid's scrollLeft
                        :y (atom 0) ;; the grid's scrollTop
                        :last-x (atom 0) ;; last scroll x position (from the previous animation loop step)
                        :last-y (atom 0) ;; last scroll y position (from the previous animation loop step)
                        :max-x (atom 0) ;; right-side scroll boundary
                        :velocity-x (atom 0) ;; scroll velocity (on x axis)
                        :velocity-y (atom 0) ;; scroll velocity (on x axis)
                        :friction (atom 0.85) ;; coefficient of friction
                        :acceleration-factor 1 ;; amount of acceleration that gets applied per 1px mouse drag
                        :speed-reordering 7 ;; speed of scroll when reordering near left or right edge of grid
                        :grabbing? (atom false) ;; currently scrolling by grabbing/dragging the grid?
                        ;; where (y position) did we pickup the column headers when we entered sticky mode?
                        :header-pickup-y (atom nil) ;; nil when not in sticky mode
                        :hovering-top-scrollbar? (atom false)} ;; is the mouse over the top scrollbar?
               :drag {:direction-intention (atom nil) ;; (left/right) direction user is intending to drag a column
                      :handle-offset (atom nil)} ;; distance that user mousedown'd from left side of column being dragged
               :products-order& (rf/subscribe [:b/products-order])}
        window-scroll-ref (partial window-scroll state)]
    (with-meta c-round-grid*
      {:component-did-mount
       (fn [cmp] ;; make grid draggable (for scrolling & reordering columns)
         (let [{:keys [mounted? round-id nodes]} state
               {:keys [grid top-scrollbar]} nodes]
           (do (reset! round-id (-> cmp r/props :id))
               (reset! grid (round-grid-cmp->round-grid-node cmp))
               (reset! top-scrollbar (round-grid-cmp->top-scrollbar-node cmp))
               (reset! mounted? true)
               (js/requestAnimationFrame (partial animation-loop state))
               (update-draggability state)
               (.addEventListener @grid "mousedown" (partial mousedown state))
               (.addEventListener @grid "mousemove" (partial mousemove state))
               (.addEventListener @grid "mouseup" (partial mouseup state))
               (.addEventListener @grid "mouseleave" (partial mouseup state))
               (.addEventListener @grid "scroll" (partial scroll state))
               (.addEventListener @top-scrollbar "mouseover" (partial mouseover-top-scrollbar state))
               (.addEventListener @top-scrollbar "mouseleave" (partial mouseleave-top-scrollbar state))
               (.addEventListener @top-scrollbar "scroll" (partial scroll-top-scrollbar state))
               (.addEventListener js/window "scroll" window-scroll-ref))))
       :component-did-update (fn [] (update-draggability state))
       :component-will-unmount (fn []
                                 (reset! (:mounted? state) false)
                                 (.removeEventListener js/window "scroll" window-scroll-ref))})))

(defn c-add-requirement-form
  [round-id popup-open?&]
  (let [value& (r/atom [])
        ;; TODO remove requirements that have already been added to the round.
        ;; they are already ignored on the backend, but shouldn't even be shown to user.
        topic-options (rf/subscribe [:b/topics.data-as-dropdown-options])
        new-topic-options (r/atom [])]
    (fn [round-id popup-open?&]
      [:> ui/Form {:as "div"
                   :class "popup-dropdown-form"}
       [:> ui/Dropdown {:style {:width "100%"}
                        :options (concat @topic-options @new-topic-options)
                        :placeholder "Enter topic..."
                        :search true
                        :selection true
                        :multiple true
                        :selectOnBlur false
                        :selectOnNavigation true
                        :closeOnChange false
                        :header "Enter a custom topic..."
                        :allowAdditions true
                        :additionLabel "Hit 'Enter' to Add "
                        :noResultsMessage "Type to add a new topic..."
                        :onAddItem (fn [_ this]
                                     (let [value (.-value this)]
                                       (swap! new-topic-options
                                              conj
                                              {:key (str "new-topic-" value)
                                               :text value
                                               :value value})))
                        :onChange (fn [_ this]
                                    (reset! value& (.-value this)))}]
       [:> ui/Button
        {:color "teal"
         :disabled (empty? @value&)
         :on-click #(do (reset! popup-open?& false)
                        (rf/dispatch [:b/round.add-requirements round-id (js->clj @value&)]))}
        "Add"]])))

(defn c-add-requirement-button
  [round]
  (let [popup-open? (r/atom false)
        get-round-grid-node #(util/first-node-by-class "round-grid")]
    (fn [{:keys [id] :as round}]
      [:> ui/Popup
       {:position "top left"
        :on "click"
        :open @popup-open?
        :onOpen #(reset! popup-open? true)
        :onClose #(reset! popup-open? false)
        :hideOnScroll false
        :flowing true
        :content (r/as-element [c-add-requirement-form id popup-open?])
        :trigger (r/as-element
                  [:> ui/Button {:color "teal"
                                 :fluid true
                                 :icon true
                                 :labelPosition "left"
                                 :on-mouse-over
                                 #(when-not @popup-open?
                                    (when-let [grid (get-round-grid-node)]
                                      (util/add-class grid "highlight-topics")))
                                 :on-mouse-leave
                                 #(when-let [grid (get-round-grid-node)]
                                    (util/remove-class grid "highlight-topics"))}
                   "Add Topics"
                   [:> ui/Icon {:name "plus"}]])}])))

(defn c-add-product-form
  [round-id round-product popup-open?&]
  (let [value& (r/atom [])
        options& (r/atom []) ; options from search results + current values
        search-query& (r/atom "")
        products->options (fn [products]
                            (for [{:keys [id pname]} products]
                              {:key id
                               :text pname
                               :value id}))]
    (fn [round-id round-product popup-open?&]
      (let [products& (rf/subscribe
                       [:gql/q
                        {:queries
                         [[:products {:_where {:_and [{:pname {:_ilike (str "%" @search-query& "%")}}
                                                      {:deleted {:_is_null true}}]}
                                      :_limit 100
                                      :_order_by {:pname :asc}}
                           [:id :pname]]]}])
            product-ids-already-in-round (set (map (comp :id :product) round-product))
            _ (when-not (= :loading @products&)
                (let [options (->> @products&
                                   :products
                                   products->options ; now we have options from gql sub
                                   ;; (this dumbly actually keeps everything, but that seems fine)
                                   (concat @options&) ; keep options for the current values
                                   distinct
                                   (remove (comp (partial contains? product-ids-already-in-round) :value)))]
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
                          :allowAdditions true
                          :additionLabel "Hit 'Enter' to Add "
                          :onAddItem (fn [_ this]
                                       (->> this
                                            .-value
                                            vector
                                            ui/as-dropdown-options
                                            (swap! options& concat)))
                          :onSearchChange (fn [_ this] (reset! search-query& (aget this "searchQuery")))
                          :onChange (fn [_ this] (reset! value& (.-value this)))}]
         [:> ui/Button
          {:color "blue"
           :disabled (empty? @value&)
           :on-click #(do (reset! popup-open?& false)
                          (rf/dispatch [:b/round.add-products round-id (js->clj @value&)]))}
          "Add"]]))))

(defn c-add-product-button
  [round]
  (let [popup-open? (r/atom false)
        get-round-grid-node #(util/first-node-by-class "round-grid")]
    (fn [{:keys [id round-product] :as round}]
      [:> ui/Popup
       {:position "bottom left"
        :on "click"
        :open @popup-open?
        :onOpen #(reset! popup-open? true)
        :onClose #(reset! popup-open? false)
        :hideOnScroll false
        :flowing true
        :content (r/as-element [c-add-product-form id round-product popup-open?])
        :trigger (r/as-element
                  [:> ui/Button {:color "blue"
                                 :fluid true
                                 :icon true
                                 :labelPosition "left"
                                 :on-mouse-over
                                 #(when-not @popup-open?
                                    (when-let [grid (get-round-grid-node)]
                                      (util/add-class grid "highlight-products")))
                                 :on-mouse-leave
                                 #(when-let [grid (get-round-grid-node)]
                                    (util/remove-class grid "highlight-products"))}
                   "Add Products"
                   [:> ui/Icon {:name "plus"}]])}])))
