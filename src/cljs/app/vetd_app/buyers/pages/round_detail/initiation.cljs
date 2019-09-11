(ns vetd-app.buyers.pages.round-detail.initiation
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [clojure.string :as s]))

;; TODO retrieve from server and store in app-db

(defn get-requirements-options []
  (ui/as-dropdown-options
   ["Pricing Estimate"
    "Pricing Model"
    "Free Trial"
    "Payment Options"
    "Minimum Contract Length"
    "Cancellation Process"
    "Company Funding Status"
    "Company Headquarters"
    "Company Year Founded"
    "Number of Employees at Company"
    "How long does it take to onboard?"
    "Onboarding Process"
    "How involved do we need to be in the onboarding process?"
    "What is our point of contact after signing?"
    "How often will we have meetings after signing?"
    "What KPIs do you provide?"
    "Integrations with other services"
    "Describe your Data Security"
    "Who are some of your current clients?"
    "Number of Current Clients"
    "Case Studies of Current Clients"
    "Key Differences from Competitors"
    "What is your Product Roadmap?"]))

;; TODO this could be prompt-ids instead

;; topics that are selected by default in the Round Initiation Form
(def default-requirements ["Pricing Estimate" "Pricing Model" "Free Trial"])


;;;; Events
(rf/reg-event-fx
 :b/round.initiation-form-saved
 (fn [_ [_ _ {{:keys [round-id]} :return}]]
   {:toast {:type "success"
            :title "Initiation Form Submitted"
            :message "Status updated to \"In Progress\""}
    :analytics/track {:event "Initiation Form Saved"
                      :props {:category "Round"
                              :label round-id}}}))

;;;; Components
(defn c-topics-explainer-list
  [{:keys [title items icon-name]}]
  [:div.explainer-item
   [:h4 title]
   [:ul {:style {:list-style "none"
                 :margin 0
                 :padding 0}}
    (util/augment-with-keys
     (for [item items]
       [:li [:> ui/Icon {:name icon-name}] (str " " item)]))]])

(defn c-topics-explainer-modal
  [topics-explainer-modal-showing?&]
  [cc/c-modal
   {:showing?& topics-explainer-modal-showing?&
    :size "tiny"
    :header "What is a Topic?"
    :content [:div.explainer-section
              (str "Topics can be any factor, feature, use case, or question that"
                   " you would like to have vendors directly respond to. Choose"
                   " existing Topics or create new Topics that will help you"
                   " narrow the field of products, making your decision easier.")
              [:br] [:br]
              [c-topics-explainer-list
               {:title "Examples of Good Topics"
                :icon-name "check"
                :items ["API that can pull customer service ratings data into XYZ Dashboard"
                        "Onboarding in less than a week with our Marketing Manager"
                        "Integrates with XYZ Chat"]}]
              [c-topics-explainer-list
               {:title "Examples of Bad Topics"
                :icon-name "x"
                :items ["Has an API"
                        "Easy to onboard"
                        "Integration"]}]]}])

(defn c-round-initiation-form
  [round-id]
  (let [requirements-options (r/atom (get-requirements-options))
        goal (r/atom "")
        start-using (r/atom "")
        num-users (r/atom "")
        budget (r/atom "")
        requirements (r/atom default-requirements)
        add-products-by-name (r/atom "")
        topics-explainer-modal-showing?& (r/atom false)]
    (fn [round-id]
      [:<>
       [:h3 "VetdRound Initiation Form"]
       [:p "Let us know a little more about what features you are looking for and who will be using this product. Then, we'll gather quotes for you to compare right away."]
       [:> ui/Form {:as "div"
                    :class "round-initiation-form"}
        [:> ui/FormField
         [:label
          "What specific topics will help you make a decision?"
          [:a {:on-click #(reset! topics-explainer-modal-showing?& true)
               :style {:float "right"}}
           [:> ui/Icon {:name "question circle"}]
           "Learn more about topics"]]
         [:> ui/Dropdown {:value @requirements
                          :options @requirements-options
                          :placeholder "Add topics..."
                          :search true
                          :selection true
                          :multiple true
                          :header "Enter a custom topic..."
                          :allowAdditions true
                          :additionLabel "Hit 'Enter' to Add "
                          :noResultsMessage "Type to add a new topic..."
                          :onAddItem (fn [_ this]
                                       (let [value (.-value this)]
                                         (swap! requirements-options
                                                conj
                                                {:key value
                                                 :text value
                                                 :value value})))
                          :onChange (fn [_ this]
                                      (reset! requirements (.-value this)))}]]
        [:> ui/FormGroup {:widths "equal"}
         [:> ui/FormField
          [:label "When do you need to decide by?"]
          [:> ui/Dropdown {:selection true
                           :options (ui/as-dropdown-options
                                     ["Within 2 Weeks" "Within 3 Weeks" "Within 1 Month"
                                      "Within 2 Months" "Within 6 Months" "Within 12 Months"])
                           :on-change (fn [_ this]
                                        (reset! start-using (.-value this)))}]]
         [:> ui/FormField
          [:label "How many users?"]
          [:> ui/Input {:labelPosition "right"}
           [:input {:type "number"
                    :on-change #(reset! num-users (-> % .-target .-value))}]
           [:> ui/Label {:basic true} "users"]]]
         [:> ui/FormField
          [:label "What is your annual budget? (optional)"]
          [:> ui/Input {:labelPosition "right"}
           [:> ui/Label {:basic true} "$"]
           [:input {:type "number"
                    :style {:width 0} ; idk why 0 width works, but it does
                    :on-change #(reset! budget (-> % .-target .-value))}]
           [:> ui/Label {:basic true} " per year"]]]]
        [:> ui/FormTextArea
         {:label "Is there any additional information you would like to provide? (optional)"
          :on-change (fn [e this]
                       (reset! goal (.-value this)))}]
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
         "Submit"]]
       [c-topics-explainer-modal topics-explainer-modal-showing?&]])))
