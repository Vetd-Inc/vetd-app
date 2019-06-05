(ns vetd-app.buyers.components
  (:require [vetd-app.ui :as ui]
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
                :content (r/as-element
                          [:> ui/Form {:style {:width 500}}
                           [:> ui/FormField
                            [:label "New VetdRound Title"]
                            [:> ui/Input
                             {:placeholder "Enter a name for your VetdRound..."
                              :default-value @title&
                              :on-change (fn [_ this]
                                           (reset! title& (.-value this)))
                              :action (r/as-element
                                       [:> ui/Button
                                        {:color "blue"
                                         :on-click #(do (reset! popup-open?& false)
                                                        (start-round-fn))}
                                        "Create"])}]]])}
               popup-props)]
       [:> ui/Popup
        (merge
         {:content (case etype
                     :product (str "Find and compare similar products to \""
                                   ename "\" that meet your needs.")
                     :category (str "Find and compare " (util/capitalize-words ename)
                                    " products that meet your needs.")
                     :none "Find and compare similar products that meet your needs.")
          :header "What is a VetdRound?"
          :position "bottom left"
          :context @context-ref&
          :trigger (r/as-element
                    [:> ui/Button (merge {:on-click #(do (.stopPropagation %)
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
  [:> ui/Label (merge {:color "teal"
                       :size "medium"
                       :as "a"
                       :onClick #(do (.stopPropagation %)
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
    [:> ui/Icon {:name "clipboard outline"}]
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
  [id rejected?]
  (let [popup-open?& (r/atom false)
        context-ref& (r/atom nil)
        reason& (atom "")
        options (ui/as-dropdown-options ["Outside our budget"
                                         "Not relevant to our business"
                                         "We already use a similar tool"])]
    (fn [id rejected?]
      (if-not rejected?
        [:<>
         [:> ui/Popup
          {:position "top right"
           :on "click"
           :open @popup-open?&
           :on-close #(reset! popup-open?& false)
           :on-click #(.stopPropagation %)
           :context @context-ref&
           :content (r/as-element
                     [:> ui/Form {:as "div"
                                  :class "popup-dropdown-form"
                                  :style {:width 400}}
                      [:> ui/Dropdown {:options options
                                       :placeholder "Enter reason..."
                                       :search true
                                       :selection true
                                       :multiple false
                                       :selectOnBlur false
                                       :selectOnNavigation true
                                       :closeOnChange true
                                       :allowAdditions true
                                       :additionLabel "Hit 'Enter' to Submit "
                                       :onAddItem (fn [_ this]
                                                    #_(->> this
                                                           .-value
                                                           vector
                                                           ui/as-dropdown-options
                                                           (swap! options& concat)))
                                       :onChange (fn [_ this] (reset! reason& (.-value this)))}]
                      [:> ui/Button
                       {:color "red"
                        :on-click #(do (reset! popup-open?& false)
                                       #_(rf/dispatch [:b/round.disqualify
                                                       (:id round)
                                                       (:id product)
                                                       @reason&]))}
                       "Reject"]])}]
         [:> ui/Popup
          {:content "Reject PrePosal"
           :position "bottom center"
           :context @context-ref&
           :trigger (r/as-element
                     [:> ui/Icon {:on-click #(do (.stopPropagation %)
                                                 (swap! popup-open?& not))
                                  :color "black"
                                  :link true
                                  :name "close"
                                  :size "large"
                                  :style {:position "absolute"
                                          :right 7}
                                  :ref (fn [this] (reset! context-ref& (r/dom-node this)))}])}]]
        [:> ui/Popup
         {:content "Undo Disqualify"
          :position "bottom center"
          :trigger (r/as-element
                    [:> ui/Button {:icon "undo"
                                   :basic true
                                   :floated "right"
                                   ;; :on-click #(rf/dispatch [:b/round.undo-disqualify (:id round) (:id product)])
                                   :size "mini"}])}]))))

(defn c-setup-call-button
  [{:keys [id pname] :as product}
   {:keys [oname] :as vendor}
   props]
  [:> ui/Popup
   {:content (str "Let us set up a call for you with " oname
                  " to discuss \"" pname "\".")
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
                   :onClick #(do (.stopPropagation %)
                                 (rf/dispatch [:b/nav-search c]))}
      c])])

(defn c-free-trial-tag []
  [:> ui/Label {:class "free-trial-tag"
                :color "gray"
                :size "small"
                :tag true}
   "Free Trial"])

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
                      :onClick #(do (.stopPropagation %)
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
  [{:keys [title]} & children]
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title title]
   [:> ui/Grid {:columns "equal"
                :style {:margin-top 0}}
    (util/augment-with-keys children)]])

(defn c-pricing
  "Component to display pricing information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn]
  [c-profile-segment {:title "Pricing"}
   [:> ui/GridRow
    [c-display-field 5 "Range"
     (when (has-data? (v-fn :product/price-range))
       [:<>
        (v-fn :product/price-range)
        [:br]
        "Request a PrePosal to get a personalized estimate."])]
    [c-display-field 6 "Model" (v-fn :product/pricing-model) :has-markdown? true]
    [c-display-field 5 "Free Trial"
     (when (has-data? (v-fn :product/free-trial?))
       (if (= "Yes" (v-fn :product/free-trial?))
         (v-fn :product/free-trial-terms)
         "No"))]]
   [:> ui/GridRow
    [c-display-field 5 "Payment Options" (v-fn :product/payment-options)]
    [c-display-field 6 "Minimum Contract Length" (v-fn :product/minimum-contract)]
    [c-display-field 5 "Cancellation Process" (v-fn :product/cancellation-process)]]])

(defn c-onboarding
  "Component to display onboarding information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn]
  [c-profile-segment {:title "Onboarding"}
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
  "Component to display onboarding information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn]
  [c-profile-segment {:title "Client Service"}
   [:> ui/GridRow
    [c-display-field 16 "Point of Contact" (v-fn :product/point-of-contact)]]
   [:> ui/GridRow
    [c-display-field 16 "Meeting Frequency" (v-fn :product/meeting-frequency)
     :has-markdown? true]]])

(defn c-reporting
  "Component to display onboarding information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn]
  [c-profile-segment {:title "Reporting & Measurement"}
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
  "Component to display onboarding information of a product profile.
  c-display-field - component to display a field (key/value)
  v-fn - function to get value per some prompt term"
  [c-display-field v-fn]
  [c-profile-segment {:title "Industry Niche"}
   [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
    [:> ui/GridRow
     [c-display-field 16 "Ideal Client Profile" (v-fn :product/ideal-client)
      :has-markdown? true
      :info "A typical user of this product, in terms of company size, revenue, verticals, etc."]]
    [:> ui/GridRow
     [c-display-field 16 "Case Studies"
      (when (has-data? (v-fn :product/case-studies))
        [c-external-link (v-fn :product/case-studies)])
      :has-markdown? true]]
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
    [c-profile-segment {:title "Company Profile"}
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
        [:p "Share \"" round-title "\" via Email"]
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
