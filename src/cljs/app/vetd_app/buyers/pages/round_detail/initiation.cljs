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

;;;; Config
;; topics that are selected by default in the Round Initiation Form
(def default-topics-terms
  []
  ;; ["preposal/pricing-estimate"
  ;;  "product/pricing-model"
  ;;  "product/free-trial?"]
  )

;;;; Events
(rf/reg-event-fx
 :b/round.initiation-form.submit
 (fn [{:keys [db]} [_ round-id {:keys [topics start-using num-users budget products goal]}]]
   (let [[bad-input message]
         (cond
           (empty? topics) [:topics "Please enter at least one topic."]
           (s/blank? start-using) [:start-using "Please select an option for: \"When do you need to decide by?\""]
           (s/blank? num-users) [:num-users "Please enter how many users you estimate will be using the product."]
           :else nil)]
     (if bad-input
       {:db (assoc-in db [:page-params :bad-input] bad-input)
        :toast {:type "error" 
                :title "Error"
                :message message}}
       {:dispatch [:save-doc
                   {:dtype "round-initiation"
                    :round-id round-id
                    :return {:handler :b/round.initiation-form-saved
                             :round-id round-id}}
                   {:terms
                    {:rounds/goal {:value goal}
                     :rounds/start-using {:value start-using}
                     :rounds/num-users {:value num-users}
                     :rounds/budget {:value budget}
                     :rounds/requirements {:value topics}
                     :rounds/add-products-by-name {:value products}}}]}))))

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
  (let [goal (r/atom "")
        start-using (r/atom "")
        num-users (r/atom "")
        budget (r/atom "")
        topic-options (rf/subscribe [:b/topics.data-as-dropdown-options])
        new-topic-options (r/atom [])
        topics (r/atom default-topics-terms)
        products (r/atom "")
        topics-explainer-modal-showing?& (r/atom false)
        bad-input& (rf/subscribe [:bad-input])]
    (fn [round-id]
      [:<>
       [:h3 "VetdRound Initiation Form"]
       [:p "Let us know a little more about what features you are looking for and who will be using this product. Then, we'll gather quotes for you to compare right away."]
       [:> ui/Form {:as "div"
                    :class "round-initiation-form"}
        [:> ui/FormField {:error (= @bad-input& :topics)}
         [:label
          "What specific topics will help you make a decision?"
          [:a {:on-click #(reset! topics-explainer-modal-showing?& true)
               :style {:float "right"}}
           [:> ui/Icon {:name "question circle"}]
           "Learn more about topics"]]
         [:> ui/Dropdown {:value @topics
                          :options (concat @topic-options @new-topic-options)
                          :placeholder "Add topics..."
                          :search true
                          :selection true
                          :multiple true
                          :header "Enter a custom topic..."
                          :allowAdditions true
                          :additionLabel "Hit 'Enter' to Add "
                          :noResultsMessage "Type to add a new topic..."
                          :on-focus #(rf/dispatch [:bad-input.reset])
                          :onAddItem (fn [_ this]
                                       (let [value (.-value this)]
                                         (swap! new-topic-options
                                                conj
                                                {:key (str "new-topic-" value)
                                                 :text value
                                                 :value (str "new-topic/" value)})))
                          :onChange
                          (fn [_ this]
                            (reset! topics
                                    (let [db-topic-set (set (map :value @topic-options))
                                          has-term? (some-fn db-topic-set              
                                                             #(s/starts-with? % "new-topic/"))]
                                      (->> (.-value this)
                                           (map #(if (has-term? %)
                                                   %
                                                   (str "new-topic/" %)))))))}]]
        [:> ui/FormGroup {:widths "equal"}
         [:> ui/FormField {:error (= @bad-input& :start-using)}
          [:label "When do you need to decide by?"]
          [:> ui/Dropdown {:selection true
                           :placeholder "Within..."
                           :options (ui/as-dropdown-options
                                     ["Within 2 Weeks" "Within 3 Weeks" "Within 1 Month"
                                      "Within 2 Months" "Within 6 Months" "Within 12 Months"])
                           :on-focus #(rf/dispatch [:bad-input.reset])
                           :on-change (fn [_ this]
                                        (reset! start-using (.-value this)))}]]
         [:> ui/FormField {:error (= @bad-input& :num-users)}
          [:label "How many users?"]
          [:> ui/Input {:labelPosition "right"}
           [:input {:type "number"
                    :placeholder "Number"
                    :on-focus #(rf/dispatch [:bad-input.reset])
                    :on-change #(reset! num-users (-> % .-target .-value))}]
           [:> ui/Label {:basic true} "users"]]]
         [:> ui/FormField
          [:label "What is your annual budget? (optional)"]
          [:> ui/Input {:labelPosition "right"}
           [:> ui/Label {:basic true} "$"]
           [:input {:type "number"
                    :placeholder "Dollars"
                    :style {:width 0} ; idk why 0 width works, but it does
                    :on-change #(reset! budget (-> % .-target .-value))}]
           [:> ui/Label {:basic true} " per year"]]]]
        [:> ui/FormTextArea
         {:label "Is there any additional information you would like to provide? (optional)"
          :placeholder "E.g., we've been using XYZ product, but it doesn't have the ability to integrate with ABC system."
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
          :on-click #(rf/dispatch [:b/round.initiation-form.submit
                                   round-id
                                   {:goal @goal
                                    :start-using @start-using
                                    :num-users @num-users
                                    :budget @budget
                                    :topics @topics
                                    :products @products}])}
         "Submit"]]
       [c-topics-explainer-modal topics-explainer-modal-showing?&]])))
