(ns vetd-app.buyers.components
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [markdown-to-hiccup.core :as md]
            [clojure.string :as s]))

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
  (let [categories (->> (:categories product) ; combine with categories defined in profile
                        (map :cname)
                        (concat (some-> product
                                        :form-docs
                                        first
                                        :responses
                                        (docs/get-field-value "Categories" "value" :sval)
                                        (s/split #",")
                                        (#(map (comp s/lower-case s/trim) %))))
                        distinct)]
    [:<>
     (for [c categories]
       ^{:key c}
       [:> ui/Label {:class "category-tag"
                     :as "a"
                     :onClick #(do (.stopPropagation %)
                                   (rf/dispatch [:b/nav-search c]))}
        c])]))

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
          (md/md->hiccup #_{:encode? true})
          (md/component))
      [:p field-value])]])

(defn has-data?
  [value]
  (not-empty (str value)))

(defn c-pricing
  [v] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Pricing"]
   [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
    [:> ui/GridRow
     (when (has-data? (v "Price Range"))
       [c-display-field {:width 5} "Range"
        [:<>
         (v "Price Range")
         [:br]
         "Request a Preposal to get a personalized estimate."]])
     (when (has-data? (v "Pricing Model"))
       [c-display-field {:width 6} "Model" (v "Pricing Model") :has-markdown? true])
     (if (= "Yes" (v "Do you offer a free trial?"))
       [c-display-field {:width 5} "Free Trial" (v "Please describe the terms of your trial")]
       [c-display-field {:width 5} "Free Trial" "No"])]
    [:> ui/GridRow
     (when (has-data? (v "Payment Options"))
       [c-display-field {:width 5} "Payment Options" (v "Payment Options")])
     (when (has-data? (v "Minimum Contract Length"))
       [c-display-field {:width 6} "Minimum Contract Length" (v "Minimum Contract Length")])
     (when (has-data? (v "Cancellation Process"))
       [c-display-field {:width 5} "Cancellation Process" (v "Cancellation Process")])]]])

(defn c-onboarding
  [v] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Onboarding"]
   [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
    [:> ui/GridRow
     (when (has-data? (v "Onboarding Process" "Estimated Time To Onboard"))
       [c-display-field {:width 16} "Estimated Time to Onboard" (v "Onboarding Process" "Estimated Time To Onboard")])]
    [:> ui/GridRow
     (when (has-data? (v "Onboarding Process"))
       [c-display-field {:width 16} "Onboarding Process" (v "Onboarding Process") :has-markdown? true])]
    [:> ui/GridRow
     (when (has-data? (v "Onboarding Team Involvement"))
       [c-display-field {:width 16} "Onboarding Team Involvement" (v "Onboarding Team Involvement") :has-markdown? true])]]])

(defn c-client-service
  [v] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Client Service"]
   [:> ui/Grid {:columns "equal" :style {:margin-top 0}}
    [:> ui/GridRow
     (when (has-data? (v "Point of Contact"))
       [c-display-field {:width 16} "Point of Contact" (v "Point of Contact")])]
    [:> ui/GridRow
     (when (has-data? (v "Meeting Frequency"))
       [c-display-field {:width 16} "Meeting Frequency" (v "Meeting Frequency") :has-markdown? true])]]])

(defn c-reporting
  [v] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Reporting & Measurements"]
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
       [c-display-field {:width 16} "Data Security" (v "Data Security") :has-markdown? true])]]])

(defn c-market-niche
  [v] ; v - value function, retrieves value by prompt name
  [:> ui/Segment {:class "detail-container profile"}
   [:h1.title "Industry Niche"]
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
       [c-display-field {:width 16} "Product Roadmap" (v "Product Roadmap") :has-markdown? true])]]])

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
      (when (= "Yes" (v "Do you offer a free trial?"))
        [c-free-trial-tag])
      [:> ui/Grid {:columns "equal"
                   :style {:margin-top 0}}
       [:> ui/GridRow
        [c-display-field {:width 11} "Description"
         [:<> (or (v "Describe your product or service") "No description available.")
          [:br]
          [:br]
          [:h3.display-field-key "Pitch"]
          [:p "Request a Preposal to get a personalized pitch."]]]
        [:> ui/GridColumn {:width 5}
         [:> ui/Grid {:columns "equal"
                      :style {:margin-top 0}}
          [:> ui/GridRow
           (when (has-data? (v "Product Website"))
             [c-display-field {:width 16} "Website"
              [:a {:href (v "Product Website")
                   :target "_blank"}
               [:> ui/Icon {:name "external square"
                            :color "blue"}]
               (v "Product Website")]])]
          [:> ui/GridRow
           (when (has-data? (v "Product Demo"))
             [c-display-field {:width 16} "Demo"
              [:a {:href (v "Product Demo")
                   :target "_blank"}
               [:> ui/Icon {:name "external square"
                            :color "blue"}]
               "Watch Video"]])]]]]]]
     (when product-profile-responses
       [:<>
        [c-pricing v]
        [c-onboarding v]
        [c-client-service v]
        [c-reporting v]
        [c-market-niche v]])]))

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
