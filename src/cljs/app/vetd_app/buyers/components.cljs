(ns vetd-app.buyers.components
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [markdown-to-hiccup.core :as md]
            [clojure.string :as s]))

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
                        :color "grey"
                        :fluid true
                        :icon true
                        :labelPosition "left"}
                       props)
   text
   [:> ui/Icon {:name icon}]])

;; note: there is another type of start-round button in category search results
(defn c-start-round-button
  [{:keys [etype eid ename props]}]
  [:> ui/Popup
   {:content (str "Find and compare similar products to \""
                  ename "\" that meet your needs.")
    :header "What is a VetdRound?"
    :position "bottom left"
    :trigger
    (r/as-element
     [:> ui/Button
      (merge {:onClick #(do (.stopPropagation %)
                            (rf/dispatch
                             [:b/start-round
                              (str "Products Similar to " ename)
                              etype
                              eid]))
              :class "start-round-button"
              :color "blue"
              :icon true
              :labelPosition "left"}
             props)
      "Start VetdRound"
      [:> ui/Icon {:name "vetd-icon"}]])}])

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

(defn c-setup-call-button
  [{:keys [id pname] :as product} {:keys [oname] :as vendor}]
  (fn []
    [:> ui/Popup
     {:content (str "Let us setup a call for you with " oname
                    " to discuss " pname ".")
      :header "Setup a Call"
      :position "bottom left"
      :trigger (r/as-element
                [:> ui/Button {:onClick #(rf/dispatch [:b/setup-call id pname])
                               :color "grey"
                               :fluid true
                               :icon true
                               :labelPosition "left"}
                 "Setup a Call"
                 [:> ui/Icon {:name "left call"}]])}]))

(defn c-ask-a-question-button
  [{:keys [id pname] :as product} {:keys [oname] :as vendor}]
  (let [modal-showing? (r/atom false)
        message (r/atom "")]
    (fn []
      [:<>
       [:> ui/Button {:onClick #(reset! modal-showing? true)
                      :color "grey"
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

(defn c-display-field
  [props field-key field-value & {:keys [has-markdown? info]}]
  [:> ui/GridColumn props
   [:> ui/Segment {:class "display-field"
                   :vertical true}
    [:h3.display-field-key
     field-key
     (when info
       [:> ui/Popup {:trigger (r/as-element [:span {:style {:font-size 16}}
                                             " " [:> ui/Icon {:name "info circle"}]])
                     :wide true}
        info])]
    (if has-markdown?
      (-> field-value
          md/md->hiccup
          md/component)
      [:p field-value])]])

(defn has-data?
  [value]
  (not-empty (str value)))

(defn c-request-profile
  [section-name etype eid ename]
  [:<>
   "This company has not completed their " section-name " section."
   [:br]
   [:br]
   [:a.blue {:onClick #(do (.stopPropagation %)
                           (rf/dispatch [:b/request-complete-profile etype eid ename]))}
    "Request Complete Profile"]])

(defn c-pricing
  [product v & {:keys [:preposal-estimate]}] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Pricing"]
   (if (or preposal-estimate
           (has-data? (v "Price Range")))
     [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
      [:> ui/GridRow
       ;; show Preposal Estimate if exists, otherwise Price Range
       (if preposal-estimate
         [c-display-field {:width 5} "Estimate" preposal-estimate]
         (when (has-data? (v "Price Range"))
           [c-display-field {:width 5} "Range"
            [:<>
             (v "Price Range")
             [:br]
             "Request a Preposal to get a personalized estimate."]]))
       (when (has-data? (v "Pricing Model"))
         [c-display-field {:width 6} "Model" (v "Pricing Model") :has-markdown? true])
       (when (has-data? (v "Do you offer a free trial?"))
         (if (= "Yes" (v "Do you offer a free trial?"))
           [c-display-field {:width 5} "Free Trial" (v "Please describe the terms of your trial")]
           [c-display-field {:width 5} "Free Trial" "No"]))]
      [:> ui/GridRow
       (when (has-data? (v "Payment Options"))
         [c-display-field {:width 5} "Payment Options" (v "Payment Options")])
       (when (has-data? (v "Minimum Contract Length"))
         [c-display-field {:width 6} "Minimum Contract Length" (v "Minimum Contract Length")])
       (when (has-data? (v "Cancellation Process"))
         [c-display-field {:width 5} "Cancellation Process" (v "Cancellation Process")])]]
     [c-request-profile "Pricing" :product (:id product) (:pname product)])])

(defn c-onboarding
  [product v] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Onboarding"]
   (if (has-data? (v "Onboarding Process" "Estimated Time To Onboard"))
     [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
      [:> ui/GridRow
       (when (has-data? (v "Onboarding Process" "Estimated Time To Onboard"))
         [c-display-field {:width 16} "Estimated Time to Onboard" (v "Onboarding Process" "Estimated Time To Onboard")])]
      [:> ui/GridRow
       (when (has-data? (v "Onboarding Process"))
         [c-display-field {:width 16} "Onboarding Process" (v "Onboarding Process") :has-markdown? true])]
      [:> ui/GridRow
       (when (has-data? (v "Onboarding Team Involvement"))
         [c-display-field {:width 16} "Onboarding Team Involvement" (v "Onboarding Team Involvement") :has-markdown? true])]]
     [c-request-profile "Onboarding" :product (:id product) (:pname product)])])

(defn c-client-service
  [product v] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Client Service"]
   (if (has-data? (v "Point of Contact"))
     [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
      [:> ui/GridRow
       (when (has-data? (v "Point of Contact"))
         [c-display-field {:width 16} "Point of Contact" (v "Point of Contact")])]
      [:> ui/GridRow
       (when (has-data? (v "Meeting Frequency"))
         [c-display-field {:width 16} "Meeting Frequency" (v "Meeting Frequency") :has-markdown? true])]]
     [c-request-profile "Client Service" :product (:id product) (:pname product)])])

(defn c-reporting
  [product v] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Reporting & Measurements"]
   (if (has-data? (v "Reporting"))
     [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
      [:> ui/GridRow
       (when (has-data? (v "Reporting"))
         [c-display-field {:width 16} "Reporting" (v "Reporting") :has-markdown? true])]
      [:> ui/GridRow
       (when (has-data? (v "KPIs"))
         [c-display-field {:width 16} "KPIs" (v "KPIs")
          :has-markdown? true
          :info "Key Performance Indicators"])]
      [:> ui/GridRow
       (when (has-data? (v "Integrations"))
         [c-display-field {:width 16} "Integrations" (v "Integrations") :has-markdown? true])]
      [:> ui/GridRow
       (when (has-data? (v "Data Security"))
         [c-display-field {:width 16} "Data Security" (v "Data Security") :has-markdown? true])]]
     [c-request-profile "Reporting & Measurements" :product (:id product) (:pname product)])])

(defn c-market-niche
  [product v] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Industry Niche"]
   (if (has-data? (v "Ideal Client Profile"))
     [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
      [:> ui/GridRow
       (when (has-data? (v "Ideal Client Profile"))
         [c-display-field {:width 16} "Ideal Client Profile" (v "Ideal Client Profile")
          :has-markdown? true
          :info "A typical user of this product, in terms of company size, revenue, verticals, etc."])]
      (when (has-data? (v "Case Studies" "Links to Case Studies"))
        [:> ui/GridRow
         [c-display-field {:width 16} "Case Studies"
          [:a {:href (v "Case Studies" "Links to Case Studies")
               :target "_blank"}
           [:> ui/Icon {:name "external square"
                        :color "blue"}]
           (v "Case Studies" "Links to Case Studies")]]])
      [:> ui/GridRow
       (when (has-data? (v "Number of Current Clients"))
         [c-display-field {:width 6} "Number of Current Clients" (util/decimal-format (v "Number of Current Clients"))])
       (when (has-data? (v "Example Current Clients"))
         [c-display-field {:width 10} "Example Current Clients" (v "Example Current Clients") :has-markdown? true])]
      [:> ui/GridRow
       (when (has-data? (v "Competitors"))
         [c-display-field {:width 16} "Competitors" (v "Competitors") :has-markdown? true])]
      [:> ui/GridRow
       (when (has-data? (v "Competitive Differentiator"))
         [c-display-field {:width 16} "Competitive Differentiator" (v "Competitive Differentiator") :has-markdown? true])]
      [:> ui/GridRow
       (when (has-data? (v "Product Roadmap"))
         [c-display-field {:width 16} "Product Roadmap" (v "Product Roadmap") :has-markdown? true])]]
     [c-request-profile "Industry Niche" :product (:id product) (:pname product)])])

(defn c-vendor-profile
  [{:keys [responses] :as vendor-profile-doc} vendor-id vendor-name]
  (if vendor-profile-doc
    (let [website-url (docs/get-field-value responses "Website" "value" :sval)
          funding-status (docs/get-field-value responses "Funding Status" "value" :sval)
          year-founded (docs/get-field-value responses "Year Founded" "value" :sval)
          headquarters (docs/get-field-value responses "Headquarters Location" "value" :sval)
          num-employees (docs/get-field-value responses "Employee Count" "value" :nval)]
      [:> ui/Segment {:class "detail-container profile"}
       [:h1.title "Company Profile"]
       [:> ui/Grid {:columns "equal"
                    :style {:margin-top 0}}
        [:> ui/GridRow
         (when (has-data? website-url)
           [c-display-field {:width 6} "Website"
            [:a {:href (str (when-not (.startsWith website-url "http") "http://") website-url)
                 :target "_blank"}
             [:> ui/Icon {:name "external square"
                          :color "blue"}]
             website-url]])
         (when (has-data? headquarters)
           [c-display-field {:width 5} "Headquarters" headquarters])]
        [:> ui/GridRow
         (when (has-data? funding-status)
           [c-display-field {:width 6} "Funding Status" funding-status])
         (when (has-data? year-founded)
           [c-display-field {:width 5} "Year Founded" year-founded])
         (when (has-data? num-employees)
           [c-display-field {:width 5} "Number of Employees" (util/decimal-format num-employees)])]]])
    [:> ui/Segment {:class "detail-container profile"}
     [:h1.title "Company Profile"]
     "This company has not completed their Company profile."
     [:br]
     [:br]
     [:a.blue {:onClick #(do (.stopPropagation %)
                             (rf/dispatch [:b/request-complete-profile :vendor vendor-id vendor-name]))}
      "Request Complete Profile"]]))
