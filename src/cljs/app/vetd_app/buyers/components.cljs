(ns vetd-app.buyers.components
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]))

(defn c-start-round-button [{:keys [etype eid ename props]}]
  [:> ui/Popup
   {:content (str "Find and compare similar products to \""
                  ename "\" that meet your needs.")
    :header "What is a VetdRound?"
    :position "bottom left"
    :trigger (r/as-element
              [:> ui/Button
               (merge {:onClick #(do (.stopPropagation %)
                                     (rf/dispatch [:b/start-round etype eid]))
                       :class "start-round-button"
                       :color "blue"
                       :icon true
                       :labelPosition "left"}
                      props)
               "Start VetdRound"
               [:> ui/Icon {:name "vetd-icon"}]])}])

(defn c-round-in-progress [{:keys [props]}]
  [:> ui/Label (merge {:color "teal"
                       :size "medium"}
                      props)
   "VetdRound In Progress"])

(defn c-rounds
  "Given a product map, display the Round data."
  [product]
  (if (not-empty (:rounds product))
    [c-round-in-progress {:props {:ribbon "left"}}]
    [c-start-round-button {:etype :product
                           :eid (:id product)
                           :ename (:pname product)}]))

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
                               :labelPosition "left"
                               :style {:margin-right 15}}
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
         [:> ui/Form
          [:> ui/FormField
           [:> ui/TextArea {:placeholder ""
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
   (for [c (:categories product)]
     ^{:key (:id c)}
     [:> ui/Label {:class "category-tag"
                   :as "a"
                   :onClick #(do (.stopPropagation %)
                                 (rf/dispatch [:b/nav-search (:cname c)]))}
      (:cname c)])])

(defn c-free-trial-tag []
  [:> ui/Label {:class "free-trial-tag"
                :color "gray"
                :size "small"
                :tag true}
   "Free Trial"])

(defn c-display-field
  [props field-key field-value] 
  [:> ui/GridColumn props
   [:> ui/Segment {:class "display-field"
                   :vertical true}
    [:h3.display-field-key field-key]
    [:p field-value]]])

(defn has-data?
  [value]
  (not-empty (str value)))

(defn c-product
  "Component to display Product details."
  [{:keys [id pname logo form-docs vendor forms rounds categories] :as product}]
  (let [product-profile-responses (-> form-docs first :responses)
        v (fn [prompt & [field value]]
            (docs/get-field-value product-profile-responses prompt (or field "value") (or value :sval)))]
    [:<>
     [:> ui/Segment {:class "detail-container"}
      [:h1.product-title
       pname " " [:small " by " (:oname vendor)]]
      [:> ui/Image {:class "product-logo"
                    :src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]
      (if (not-empty (:rounds product))
        [c-round-in-progress {:props {:ribbon "left"}}])
      [c-categories product]
      [:> ui/Grid {:columns "equal"
                   :style {:margin-top 0}}
       [:> ui/GridRow
        [c-display-field {:width 12} "Product Description"
         [:<> (or (v "Describe your product or service") "No description available.")
          (when (not-empty (v "Product Website"))
            [:<>
             [:br]
             [:br]
             "Website: " [:a {:href (v "Product Website")
                              :target "_blank"}
                          [:> ui/Icon {:name "external square"
                                       :color "blue"}]
                          (v "Product Website")]])]]]
       [:> ui/GridRow
        [c-display-field {:width 12} "Pitch" "Request a Preposal to get a personalized pitch."]]]]
     [:> ui/Segment {:class "detail-container profile"}
      [:h1.title "Pricing"]
      [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
       [:> ui/GridRow
        (when (has-data? (v "Price Range"))
          [c-display-field {:width 5} "Price Range" (v "Price Range")])
        (when (has-data? (v "Pricing Model"))
          [c-display-field {:width 11} "Pricing Model" (v "Pricing Model")])]
       [:> ui/GridRow
        (if (= "Yes" (v "Do you offer a free trial?"))
          [c-display-field {:width 5} "Free Trial" (v "Please describe the terms of your trial")]
          [c-display-field {:width 5} "Free Trial" "No"])
        (when (has-data? (v "Minimum Contract Length"))
          [c-display-field {:width 6} "Minimum Contract Length" (v "Minimum Contract Length")])
        (when (has-data? (v "Cancellation Process"))
          [c-display-field {:width 5} "Cancellation Process" (v "Cancellation Process")])]
       [:> ui/GridRow
        (when (has-data? (v "Payment Options"))
          [c-display-field {:width 6} "Payment Options" (v "Payment Options")])]]


      #_(when (has-data? (v "Number of Current Clients"))
          [c-display-field {:width 6} "Number of Current Clients" (util/decimal-format (v "Number of Current Clients"))])

      ]]))

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
           [c-display-field {:width 8} "Website"
            [:a {:href website-url
                 :target "_blank"}
             [:> ui/Icon {:name "external square"
                          :color "blue"}]
             website-url]])
         (when (has-data? headquarters)
           [c-display-field {:width 8} "Headquarters" headquarters])]
        [:> ui/GridRow
         (when (has-data? funding-status)
           [c-display-field {:width 5} "Funding Status" funding-status])
         (when (has-data? year-founded)
           [c-display-field {:width 5} "Year Founded" year-founded])
         (when (has-data? num-employees)
           [c-display-field {:width 6} "Number of Employees" (util/decimal-format num-employees)])]]])
    [:> ui/Segment {:class "detail-container vendor-profile"}
     [:h1.title "Company Profile"]
     "This company has not completed a profile."
     [:br]
     [:br]
     [:a.blue {:onClick #(do (.stopPropagation %)
                             (rf/dispatch [:b/request-vendor-profile vendor-id vendor-name]))}
      "Request a Company Profile"]]))
