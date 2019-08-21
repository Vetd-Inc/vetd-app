(ns vetd-app.buyers.components
  (:require [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(defn c-external-link
  [url text]
  [:a {:href (str (when-not (.startsWith url "http") "http://") url)
       :target "_blank"}
   [:> ui/Icon {:name "external square"
                :color "blue"}]
   (or text url)])

(defn c-back-button
  ([] (c-back-button {} "Back"))
  ([text] (c-back-button {} text))
  ([props text] [:> ui/Button (merge {:class "back-button"
                                      :on-click #(.go js/history -1)
                                      :basic true
                                      :icon true
                                      :size "small"
                                      :fluid true
                                      :labelPosition "left"}
                                     props)
                 text
                 [:> ui/Icon {:name "left arrow"}]]))

(defn c-sidebar-button
  [{:keys [text dispatch icon props]}]
  [:> ui/Button (merge {:onClick #(rf/dispatch dispatch)
                        :color "white"
                        :fluid true
                        :icon true
                        :labelPosition "left"}
                       props)
   text
   [:> ui/Icon {:name icon}]])

(defn c-start-round-button
  [{:keys [etype eid ename props popup-props]}]
  (let [popup-open?& (r/atom false)
        context-ref& (r/atom nil)
        default-title (case etype
                        :product (str "Products Similar to " ename)
                        :category (str (util/capitalize-words ename) " Products")
                        :none "")
        title& (atom default-title)
        bad-title?& (r/atom false)
        start-round-fn #(rf/dispatch [:b/start-round
                                      @title&
                                      (case etype
                                        :none :category
                                        etype)
                                      eid])]
    (fn []
      [:<>
       [:> ui/Popup
        (merge {:position "bottom left"
                :on "click"
                :open @popup-open?&
                :on-close #(reset! popup-open?& false)
                :on-click #(.stopPropagation %)
                :context @context-ref&
                :header "Create a New VetdRound"
                :content (r/as-element
                          [:div
                           [:p {:style {:margin-top 7
                                        :margin-bottom 7}}
                            "Enter a name for your VetdRound:"]
                           [:> ui/Form {:style {:width 500}}
                            [:> ui/FormField {:error @bad-title?&}
                             [:> ui/Input
                              {:placeholder "E.g., Marketing Analytics Products"
                               :default-value @title&
                               :on-change (fn [_ this]
                                            (reset! title& (.-value this)))
                               :action (r/as-element
                                        [:> ui/Button
                                         {:color "blue"
                                          :on-click (fn []
                                                      (if (s/blank? @title&)
                                                        (reset! bad-title?& true)
                                                        (do (reset! bad-title?& false)
                                                            (reset! popup-open?& false)
                                                            (start-round-fn))))}
                                         "Create"])}]]]])}
               popup-props)]
       [:> ui/Popup
        (merge
         {:content (r/as-element
                    [:div.content
                     (case etype
                       :product [:<>
                                 "Find and compare similar products to " [:strong ename]
                                 " that meet your needs."]
                       :category (str "Find and compare " (util/capitalize-words ename)
                                      " products that meet your needs.")
                       :none "Find and compare similar products that meet your needs.")])
          :header "What is a VetdRound?"
          :position "bottom left"
          :context @context-ref&
          :trigger (r/as-element
                    [:> ui/Button (merge {:on-click (fn [e]
                                                      (.stopPropagation e)
                                                      (swap! popup-open?& not))
                                          :color "blue"
                                          :icon true
                                          :labelPosition "left"
                                          :ref (fn [this] (reset! context-ref& (r/dom-node this)))}
                                         props)
                     (case etype
                       :product "Create VetdRound"
                       :category (str "Create VetdRound for \"" ename "\"")
                       :none "Create VetdRound")
                     [:> ui/Icon {:name "vetd-icon"}]])}
         popup-props)]])))

(defn c-round-in-progress [{:keys [round-idstr props]}]
  [:> ui/Label (merge {:color "vetd"
                       :size "medium"
                       :as "a"
                       :on-click (fn [e]
                                   (.stopPropagation e)
                                   (rf/dispatch [:b/nav-round-detail round-idstr]))}
                      props)
   "Product In VetdRound"])

(defn c-rounds
  "Given a product map, display the Round data."
  [product]
  (if (not-empty (:rounds product))
    [c-round-in-progress {:props {:ribbon "left"}}]
    [c-start-round-button {:etype :product
                           :eid (:id product)
                           :ename (:pname product)}]))

(defn c-round-status
  [status]
  "Display a round's status with a Step Group."
  [:> ui/StepGroup {:class "round-status"
                    :size "small"
                    :widths 3
                    :style {:user-select "none"}}
   [:> ui/Step {:style {:cursor "inherit"}
                :disabled (not= status "initiation")}
    [:> ui/Icon {:name "wpforms"}]
    [:> ui/StepContent
     [:> ui/StepTitle "Initiation"]
     [:> ui/StepDescription "Define your requirements"]]]
   [:> ui/Step {:style {:cursor "inherit"}
                :disabled (not= status "in-progress")}
    [:> ui/Icon {:name "chart bar"}]
    [:> ui/StepContent
     [:> ui/StepTitle "In Progress"]
     [:> ui/StepDescription "Comparison and dialogue"]]]
   [:> ui/Step {:style {:cursor "inherit"}
                :disabled (not= status "complete")}
    [:> ui/Icon {:name "check"}]
    [:> ui/StepContent
     [:> ui/StepTitle "Complete"]
     [:> ui/StepDescription "Final decision"]]]])

(defn c-reject-preposal-button
  [id rejected? & [{:keys [icon?]}]]
  (let [popup-open?& (r/atom false)
        popup-position (if icon? "bottom right" "bottom left")
        context-ref& (r/atom nil)
        reason& (atom "")
        options& (r/atom (ui/as-dropdown-options ["Outside our budget"
                                                  "Not relevant to our business"
                                                  "We already use a similar tool"]))
        submit #(do (reset! popup-open?& false)
                    (rf/dispatch [:b/preposals.reject id @reason&]))]
    (fn [id rejected?]
      (if-not rejected?
        [:<>
         [:> ui/Popup
          {:position popup-position
           :on "click"
           :open @popup-open?&
           :on-close #(reset! popup-open?& false)
           :on-click #(.stopPropagation %)
           :context @context-ref&
           :header "Reject PrePosal"
           :content (r/as-element
                     [:div
                      [:p {:style {:margin-top 7}}
                       "Vendor will be notified, but will not be permitted to reach out."]
                      [:> ui/Form {:as "div"
                                   :class "popup-dropdown-form"
                                   :style {:width 450}}
                       [:> ui/Dropdown {:options @options&
                                        :placeholder "Enter reason..."
                                        :search true
                                        :selection true
                                        :multiple false
                                        :selectOnBlur false
                                        :selectOnNavigation true
                                        :closeOnChange true
                                        :header "Enter a custom reason..."
                                        :allowAdditions true
                                        :additionLabel "Hit 'Enter' to Reject as "
                                        :onAddItem (fn [_ this]
                                                     (->> this
                                                          .-value
                                                          vector
                                                          ui/as-dropdown-options
                                                          (swap! options& concat))
                                                     (submit))
                                        :onChange (fn [_ this] (reset! reason& (.-value this)))}]
                       [:> ui/Button
                        {:color "red"
                         :on-click submit}
                        "Reject"]]])}]
         [:> ui/Popup
          {:header "Reject PrePosal"
           :content "Reject if you aren't interested"
           :position popup-position
           :context @context-ref&
           :trigger (r/as-element
                     (if icon?
                       [:> ui/Icon {:on-click (fn [e]
                                                (.stopPropagation e)
                                                (swap! popup-open?& not))
                                    :color "black"
                                    :link true
                                    :name "close"
                                    :size "large"
                                    :style {:position "absolute"
                                            :right 7}
                                    :ref (fn [this] (reset! context-ref& (r/dom-node this)))}]
                       [:> ui/Button {:on-click #(swap! popup-open?& not)
                                      :color "white"
                                      :fluid true
                                      :icon true
                                      :labelPosition "left"
                                      :ref (fn [this] (reset! context-ref& (r/dom-node this)))}
                        "Reject Preposal"
                        [:> ui/Icon {:name "close"}]]))}]]
        [:> ui/Popup
         {:content "Undo PrePosal Rejection"
          :position popup-position
          :trigger (r/as-element
                    (if icon?
                      [:> ui/Icon {:on-click (fn [e]
                                               (.stopPropagation e)
                                               (rf/dispatch [:b/preposals.undo-reject id]))
                                   :link true
                                   :color "red"
                                   :name "undo"
                                   :size "large"
                                   :style {:position "absolute"
                                           :right 7}}]
                      [:> ui/Button {:on-click #(rf/dispatch [:b/preposals.undo-reject id])
                                     :color "white"
                                     :fluid true
                                     :icon true
                                     :labelPosition "left"}
                       "Undo Reject"
                       [:> ui/Icon {:name "undo"}]]))}]))))

(defn c-setup-call-button
  [{:keys [id pname] :as product}
   {:keys [oname] :as vendor}
   props]
  [:> ui/Popup
   {:content (r/as-element
              [:div.content
               "Let us set up a call for you with " oname " to discuss " [:strong pname] "."])
    :header "Set Up a Call"
    :position "bottom left"
    :trigger (r/as-element
              [:> ui/Button (merge {:onClick #(rf/dispatch [:b/setup-call id pname])
                                    :color "lightblue"
                                    :fluid true
                                    :icon true
                                    :labelPosition "left"}
                                   props)
               "Set Up a Call"
               [:> ui/Icon {:name "left call"}]])}])

(defn c-ask-a-question-button
  [{:keys [id pname] :as product} {:keys [oname] :as vendor}]
  (let [modal-showing? (r/atom false)
        message (r/atom "")]
    (fn []
      [:<>
       [:> ui/Button {:onClick #(reset! modal-showing? true)
                      :color "lightblue"
                      :fluid true
                      :icon true
                      :labelPosition "left"
                      :style {:margin-right 15}}
        "Ask a Question"
        [:> ui/Icon {:name "question"}]]
       [:> ui/Modal {:open @modal-showing?
                     :size "tiny"
                     :dimmer "inverted"
                     :closeOnDimmerClick false
                     :closeOnEscape false}
        [:> ui/ModalHeader "Ask a Question About \"" pname "\""]
        [:> ui/ModalContent
         [:> ui/Form {:as "div"}
          [:> ui/FormField
           [:> ui/TextArea {:placeholder "Enter your question here..."
                            :autoFocus true
                            :spellCheck true
                            :onChange (fn [_ this]
                                        (reset! message (.-value this)))}]]]]
        [:> ui/ModalActions
         [:> ui/Button {:onClick #(reset! modal-showing? false)}
          "Cancel"]
         [:> ui/Button {:onClick #(do (rf/dispatch [:b/ask-a-question
                                                    id
                                                    pname
                                                    @message])
                                      (reset! modal-showing? false))
                        :color "blue"}
          "Submit"]]]])))

(defn c-categories
  "Given a product map, display the categories as tags."
  [product]
  [:<>
   (for [c (map :cname (:categories product))]
     ^{:key c}
     [:> ui/Label {:class "category-tag"
                   :as "a"
                   :on-click (fn [e]
                               (.stopPropagation e)
                               (rf/dispatch [:b/nav-search c]))}
      c])])

(defn c-free-trial-tag []
  [:> ui/Label {:color "teal"
                :size "small"
                :tag true}
   "Free Trial"])

(defn c-discount-details
  "Get hiccup for displaying all the details about a discount(s)."
  [discounts]
  (util/augment-with-keys
   (for [{:keys [gname group-discount-descr]} discounts]
     [:div group-discount-descr
      (when (> (count discounts) 1)
        (str " (" gname ")"))])))

(defn c-discount-tag [discounts]
  [:> ui/Popup
   {:content (r/as-element (c-discount-details discounts))
    :header "Community Discount"
    :position "bottom center"
    :trigger (r/as-element
              [:> ui/Label {:color "blue"
                            :size "small"
                            :tag true}
               "Discount"])}])

(defn c-tags
  [product v-fn & [discounts]]
  [:div.product-tags
   [c-categories product]
   (when (some-> (v-fn :product/free-trial?)
                 s/lower-case
                 (= "yes"))
     [c-free-trial-tag])
   (when (seq discounts)
     [c-discount-tag discounts])])

(defn c-product-logo
  [filename]                      ; TODO make config var 's3-base-url'
  [:div.product-logo {:style {:background-image (str "url('https://s3.amazonaws.com/vetd-logos/" filename "')")}}])

(defn c-pricing-estimate
  [v-fn]
  (let [value (v-fn :preposal/pricing-estimate "value" :nval)
        unit (v-fn :preposal/pricing-estimate "unit")
        details (v-fn :preposal/pricing-estimate "details")]
    (if value
      [:span (util/currency-format value) " / " unit " " [:small "(estimate) " details]]
      details)))

(defn product-description
  [v-fn]
  (-> (v-fn :product/description)
      (or "No description available.")
      (util/truncate-text 175)))

(defn has-data?
  [value]
  (not-empty (str value)))

(defn c-display-field*
  [profile width field-key field-value
   & {:keys [has-markdown? info]}]
  [:> ui/GridColumn {:width width}
   [:> ui/Segment {:class (str "display-field "
                               (when-not (has-data? field-value) "missing-data"))
                   :vertical true}
    [:h3.display-field-key
     field-key
     (when info
       [:> ui/Popup {:trigger (r/as-element [:span {:style {:font-size 16}}
                                             " " [:> ui/Icon {:name "info circle"}]])
                     :wide true}
        info])]
    (if (has-data? field-value)
      [:div.display-field-value
       (if has-markdown?
         (util/parse-md field-value)
         field-value)]
      [:<>
       [:div.display-field-value "Unavailable"]
       [:> ui/Button {:color "lightteal"
                      :onClick (fn [e]
                                 (.stopPropagation e)
                                 (rf/dispatch [:b/request-complete-profile
                                               (:type profile)
                                               (:id profile)
                                               (:name profile)
                                               field-key]))}
        "Request Complete Profile"]])]])

(defn requestable
  [component]
  (with-meta component
    {:component-did-mount
     (fn [this]             ; the args index for "field-value"
       (when-not (has-data? ((r/argv this) 3))
         (let [node (r/dom-node this)
               body (first (array-seq (.getElementsByTagName js/document "body")))
               mouseenter #(.add (.-classList body) "missing-data-hovering")
               mouseleave #(.remove (.-classList body) "missing-data-hovering")]
           (.addEventListener node "mouseenter" mouseenter)
           (.addEventListener node "mouseleave" mouseleave))))}))

(defn c-profile-segment
  [{:keys [title style icon icon-style]} & children]
  [:> ui/Segment {:class "detail-container profile"
                  :style style}
   [:h1.title
    (when icon
      [:> ui/Icon {:name icon
                   :style (merge {:margin-right 7}
                                 icon-style)}])
    title]
   [:> ui/Grid {:columns "equal"
                :style {:margin-top 0}}
    (util/augment-with-keys children)]])

(defn c-preposal
  "Component to display preposal information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term from the preposal doc"
  [c-display-field v-fn]
  (let [pricing-estimate-value (v-fn :preposal/pricing-estimate "value" :nval)
        pricing-estimate-unit (v-fn :preposal/pricing-estimate "unit")
        pricing-estimate-details (v-fn :preposal/pricing-estimate "details")
        pitch (v-fn :preposal/pitch)]
    [c-profile-segment {:title "PrePosal"
                        :icon "clipboard outline"}
     [:> ui/GridRow
      [c-display-field 16
       "Pitch"
       (util/parse-md pitch)]]
     [:> ui/GridRow
      [c-display-field 16
       "Pricing Estimate"
       (if pricing-estimate-value
         (str
          "$" (util/decimal-format pricing-estimate-value) " / " pricing-estimate-unit
          (when (not-empty pricing-estimate-details)
            (str " - " pricing-estimate-details)))
         pricing-estimate-details)]]]))

(defn c-average-rating
  [agg-group-prod-rating]
  (let [ ;; e.g., {[rating] [agg count], 2 8, 3 2, 4 0, 5 4}
        ratings-enum (->> agg-group-prod-rating
                          (reduce (fn [acc {:keys [rating count-stack-items]}]
                                    (update acc rating + count-stack-items))
                                  {1 0, 2 0, 3 0, 4 0, 5 0})
                          (remove (comp nil? key))
                          (into {}))
        ratings-sum (reduce (fn [acc [k v]] (+ acc (* k v))) 0 ratings-enum)
        ratings-count (reduce (fn [acc [k v]] (+ acc v)) 0 ratings-enum)
        ratings-mean (when (pos? ratings-count)
                       (/ ratings-sum ratings-count))]
    (if ratings-mean
      [:<>
       [:> ui/Rating {:rating ratings-mean
                      :maxRating 5
                      :size "huge"
                      :disabled true
                      :style {:margin "0 0 5px -3px"}}]
       [:br]
       (str (/ (Math/round (* ratings-mean 10)) 10)
            " out of 5 stars - " ratings-count
            " Rating" (when (> ratings-count 1) "s"))]
      "No community ratings available.")))

(defn c-community-usage-modal
  [showing?& orgs community-str]
  (fn [showing?& orgs community-str]
    [:> ui/Modal {:open @showing?&
                  :on-close #(reset! showing?& false)
                  :size "tiny"
                  :dimmer "inverted"
                  :closeOnDimmerClick true
                  :closeOnEscape true
                  :closeIcon true} 
     [:> ui/ModalHeader (str "Usage in Your " (s/capitalize community-str))]
     [:> ui/ModalContent {:scrolling true}
      [:p "This product has been used by " (count orgs) " organizations in your " community-str "."]
      [:> ui/List
       (util/augment-with-keys
        (for [{:keys [id oname]} orgs]
          [:> ui/ListItem {:as "a"}
           [:> ui/ListContent
            [:div {:style {:display "inline-block"
                           :float "left"
                           :margin-right 7}}
             [cc/c-avatar-initials oname]]
            [:> ui/ListHeader {:style {:line-height "40px"}}
             oname]]]))]]]))

(defn c-community
  "Component to display community information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field product-id agg-group-prod-rating agg-group-prod-price]
  (let [group-ids& (rf/subscribe [:group-ids])
        showing?& (r/atom false)
        stack-items& (rf/subscribe [:gql/sub
                                    {:queries
                                     [[:group-org-memberships {:group-id @group-ids&
                                                               :deleted nil}
                                       [:id
                                        [:orgs
                                         [:id :oname
                                          [:stack-items {:product-id product-id
                                                         :deleted nil}
                                           [:id :price-amount :price-period]]]]]]]}])]
    (fn [c-display-field product-id agg-group-prod-rating agg-group-prod-price]
      (let [community-str (str "communit" (if (> (count @group-ids&) 1) "ies" "y"))]
        [c-profile-segment {:title (str "Your " (s/capitalize community-str))
                            :style {:min-height 138} ;; to minimize rendering flash
                            :icon "group"
                            :icon-style {:margin-right 10}}
         (when-not (= :loading @stack-items&)
           (let [orgs (->> @stack-items&
                           :group-org-memberships
                           (map :orgs)
                           (filter (comp seq :stack-items))
                           distinct)]
             [:<>
              (when (seq orgs)
                [:> ui/GridRow
                 [:> ui/GridColumn
                  [:> ui/Segment {:class "display-field"
                                  :vertical true}
                   [:h3.display-field-key "Median Annual Price"]
                   [:div.display-field-value
                    (let [median-prices (map :median-price agg-group-prod-price)]
                      (if (seq median-prices)
                        (str "$" ;; get the mean from all the member'd groups' medians
                             (util/decimal-format (/ (apply + median-prices) (count median-prices)))
                             " / year")
                        "No community pricing data."))]]]
                 [:> ui/GridColumn
                  [:> ui/Segment {:class "display-field"
                                  :vertical true}
                   [:h3.display-field-key "Average Rating"]
                   [:div.display-field-value
                    [c-average-rating agg-group-prod-rating]]]]])
              [:> ui/GridRow
               [:> ui/GridColumn
                [:> ui/Segment {:class "display-field"
                                :vertical true}
                 [:h3.display-field-key "Usage"]
                 [:div.display-field-value
                  (let [max-orgs-showing 10]
                    (if (seq orgs)
                      [:<>
                       (str "Used by " (count orgs) " organizations in your " community-str ".")
                       [:div.used-by-orgs
                        (util/augment-with-keys
                         (for [{:keys [oname]} (take max-orgs-showing orgs)]
                           [:> ui/Popup
                            {:position "bottom center"
                             :content oname
                             :trigger (r/as-element
                                       [:a [cc/c-avatar-initials oname]])}]))
                        (when (> (count orgs) max-orgs-showing)
                          [:<>
                           [:a {:on-click #(reset! showing?& true)}
                            (str " see all " (count orgs) "...")]
                           [c-community-usage-modal showing?& orgs community-str]])]]
                      (str "No one in your " community-str " has used this product.")))]]]]]))]))))

(defn c-pricing
  "Component to display pricing information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn & [discounts]]
  [c-profile-segment {:title "Pricing"
                      :icon "dollar"
                      :icon-style {:margin-right 5
                                   :margin-left -5}}
   [:> ui/GridRow
    [c-display-field 16 "Range"
     (when (has-data? (v-fn :product/price-range))
       [:<>
        (v-fn :product/price-range)
        [:br]
        "Request a PrePosal to get a personalized estimate."])]]
   (when (or (not-empty discounts)
             (has-data? (v-fn :product/free-trial?)))
     [:> ui/GridRow
      (when (not-empty discounts)
        [c-display-field 8 "Community Discount"
         (c-discount-details discounts)])
      [c-display-field 8 "Free Trial"
       (when (has-data? (v-fn :product/free-trial?))
         (if (some-> (v-fn :product/free-trial?)
                     s/lower-case
                     (= "yes"))
           (if (has-data? (v-fn :product/free-trial-terms))
             (v-fn :product/free-trial-terms)
             "Yes")
           "No"))]])
   [:> ui/GridRow
    [c-display-field 8 "Model" (v-fn :product/pricing-model) :has-markdown? true]
    [c-display-field 8 "Payment Options" (v-fn :product/payment-options)]]
   [:> ui/GridRow
    [c-display-field 8 "Minimum Contract Length" (v-fn :product/minimum-contract)]
    [c-display-field 8 "Cancellation Process" (v-fn :product/cancellation-process)]]])

(defn c-onboarding
  "Component to display onboarding information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn]
  [c-profile-segment {:title "Onboarding"
                      ;; :icon "toggle on"
                      :icon "handshake outline"
                      :icon-style {:position "relative"
                                   :top 1
                                   :margin-right 10}
                      }
   [:> ui/GridRow
    [c-display-field 16
     "Estimated Time to Onboard" (v-fn :product/onboarding-estimated-time)]]
   [:> ui/GridRow
    [c-display-field 16
     "Onboarding Process" (v-fn :product/onboarding-process) :has-markdown? true]]
   [:> ui/GridRow
    [c-display-field 16
     "Onboarding Team Involvement" (v-fn :product/onboarding-team-involvement)
     :has-markdown? true]]])

(defn c-client-service
  "Component to display client service information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn]
  [c-profile-segment {:title "Client Service"
                      :icon "comment alternate outline"}
   [:> ui/GridRow
    [c-display-field 16 "Point of Contact" (v-fn :product/point-of-contact)]]
   [:> ui/GridRow
    [c-display-field 16 "Meeting Frequency" (v-fn :product/meeting-frequency)
     :has-markdown? true]]])

(defn c-reporting
  "Component to display reporting information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn]
  [c-profile-segment {:title "Reporting & Measurement"
                      ;; :icon "balance scale"
                      :icon "chart line"
                      :icon-style {:position "relative"
                                   :top 1}}
   [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
    [:> ui/GridRow
     [c-display-field 16 "Reporting" (v-fn :product/reporting)
      :has-markdown? true]]
    [:> ui/GridRow
     [c-display-field 16 "KPIs" (v-fn :product/kpis)
      :has-markdown? true
      :info "Key Performance Indicators"]]
    [:> ui/GridRow
     [c-display-field 16 "Integrations" (v-fn :product/integrations)
      :has-markdown? true]]
    [:> ui/GridRow
     [c-display-field 16 "Data Security" (v-fn :product/data-security)
      :has-markdown? true]]]])

(defn c-market-niche
  "Component to display market niche information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn]
  [c-profile-segment {:title "Industry Niche"
                      :icon "cubes"}
   [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
    [:> ui/GridRow
     [c-display-field 16 "Ideal Client Profile" (v-fn :product/ideal-client)
      :has-markdown? true
      :info "A typical user of this product, in terms of company size, revenue, verticals, etc."]]
    [:> ui/GridRow
     [c-display-field 16 "Case Studies"
      (when (has-data? (v-fn :product/case-studies "Links to Case Studies"))
        [c-external-link (v-fn :product/case-studies "Links to Case Studies")])]]
    [:> ui/GridRow
     [c-display-field 6 "Number of Current Clients"
      (when (has-data? (v-fn :product/num-clients nil :nval))
        (util/decimal-format (v-fn :product/num-clients nil :nval)))]
     [c-display-field 10 "Example Current Clients" (v-fn :product/clients)
      :has-markdown? true]]
    [:> ui/GridRow
     [c-display-field 16 "Competitors" (v-fn :product/competitors)
      :has-markdown? true]]
    [:> ui/GridRow
     [c-display-field 16 "Competitive Differentiator" (v-fn :product/competitive-differentiator)
      :has-markdown? true]]
    [:> ui/GridRow
     [c-display-field 16 "Product Roadmap" (v-fn :product/roadmap)
      :has-markdown? true]]]])

(defn c-vendor-profile
  [{:keys [response-prompts] :as vendor-profile-doc} vendor-id vendor-name]
  (let [c-display-field (requestable (partial c-display-field* {:type :vendor
                                                                :id vendor-id
                                                                :name vendor-name}))
        v-fn (partial docs/get-value-by-term response-prompts)]
    [c-profile-segment {:title "Company Profile"
                        ;; :icon "briefcase"
                        :icon "building"
                        }
     [:> ui/GridRow 
      [c-display-field 6 "Website"
       (when (has-data? (v-fn :vendor/website))
         [c-external-link (v-fn :vendor/website)])]
      [c-display-field 6 "Headquarters" (v-fn :vendor/headquarters)]]
     [:> ui/GridRow
      [c-display-field 6 "Funding Status" (v-fn :vendor/funding)]
      [c-display-field 5 "Year Founded" (v-fn :vendor/year-founded)]
      [c-display-field 5 "Number of Employees"
       (when (has-data? (v-fn :vendor/employee-count nil :nval))
         (util/decimal-format (v-fn :vendor/employee-count nil :nval)))]]]))

(defn c-share-modal
  [round-id round-title showing?&]
  (let [email-addresses-options& (r/atom [])
        email-addresses& (r/atom [])]
    (fn [round-id round-title showing?&]
      [:> ui/Modal {:open @showing?&
                    :on-close #(reset! showing?& false)
                    :size "tiny"
                    :dimmer "inverted"
                    :closeOnDimmerClick true
                    :closeOnEscape true
                    :closeIcon true} 
       [:> ui/ModalHeader "Share VetdRound"]
       [:> ui/ModalContent
        [:p
         "Share " [:strong round-title] " with someone outside your organization via email."]
        [:> ui/Form {:as "div"
                     :style {:padding-bottom "1rem"}}
         [:> ui/Dropdown {:style {:width "100%"}
                          :options @email-addresses-options&
                          :placeholder "Enter email addresses..."
                          :search true
                          :selection true
                          :multiple true
                          :selectOnBlur false
                          :selectOnNavigation true
                          :noResultsMessage "Type to add a new email address..."
                          :allowAdditions true
                          :additionLabel "Hit 'Enter' to Add "
                          :onAddItem (fn [_ this]
                                       (->> this
                                            .-value
                                            vector
                                            ui/as-dropdown-options
                                            (swap! email-addresses-options& concat)))
                          :onChange (fn [_ this]
                                      (reset! email-addresses& (.-value this)))}]]]
       [:> ui/ModalActions
        [:> ui/Button {:onClick #(do (reset! email-addresses& [])
                                     (reset! showing?& false))}
         "Cancel"]
        [:> ui/Button
         {:onClick #(do (rf/dispatch [:b/round.share round-id round-title @email-addresses&])
                        (reset! email-addresses& [])
                        (reset! showing?& false))
          :color "blue"
          :disabled (empty? @email-addresses&)}
         "Share"]]])))
