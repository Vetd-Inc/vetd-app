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
                             :round-id round-id
                             :products products}}
                   {:terms
                    {:rounds/goal {:value goal}
                     :rounds/start-using {:value start-using}
                     :rounds/num-users {:value num-users}
                     :rounds/budget {:value budget}
                     :rounds/requirements {:value topics}}}]}))))

(rf/reg-event-fx
 :b/round.initiation-form-saved
 (fn [_ [_ _ {{:keys [round-id products]} :return}]]
   (merge {:toast {:type "success"
                   :title "Initiation Form Submitted"
                   :message "Status updated to \"In Progress\""}
           :analytics/track {:event "Initiation Form Saved"
                             :props {:category "Round"
                                     :label round-id}}}
          (when (seq products)
            {:dispatch [:b/round.add-products round-id products]}))))

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

(def curated-topics-terms
  [ ;; "preposal/pitch"
   "preposal/pricing-estimate"
   "product/cancellation-process"
   "product/case-studies"
   ;; "product/categories"
   "product/clients"
   "product/competitive-differentiator"
   "product/competitors"
   "product/data-security"
   "product/demo"
   ;; "product/description"
   ;; "product/free-trial-terms"
   "product/free-trial?"
   "product/ideal-client"
   "product/integrations"
   "product/kpis"
   ;; "product/logo"
   "product/meeting-frequency"
   "product/minimum-contract"
   "product/num-clients"
   "product/onboarding-estimated-time"
   "product/onboarding-process"
   "product/onboarding-team-involvement"
   "product/payment-options"
   "product/point-of-contact"
   "product/price-range"
   "product/pricing-model"
   "product/reporting"
   "product/roadmap"
   "product/tagline"
   ;; "product/website"
   "vendor/employee-count"
   "vendor/funding"
   "vendor/headquarters"
   ;; "vendor/logo"
   ;; "vendor/website"
   "vendor/year-founded"])

(defn c-round-initiation-form
  [{round-id :id
    round-product :round-product
    initiation-form-prefill :initiation-form-prefill
    :as round}]
  (let [goal (r/atom "")
        start-using (r/atom "")
        num-users (r/atom "")
        budget (r/atom "")

        prefill-prompt-ids (vec (map (comp str :id) (:prompts initiation-form-prefill)))
        topic-options (rf/subscribe [:gql/q
                                     {:queries
                                      [[:prompts {:_where {:_or [{:term {:_in curated-topics-terms}}
                                                                 {:id {:_in prefill-prompt-ids}}]} 
                                                  :deleted nil
                                                  :_limit 500 ;; sanity check
                                                  :_order_by {:term :asc}} ;; a little easier to read
                                        [:id :prompt :term]]]}])
        new-topic-options (r/atom [])
        ;; the ids of the selected topics
        topics (r/atom prefill-prompt-ids)
        topics-explainer-modal-showing?& (r/atom false)

        products-results->options (fn [products-results]
                                    (for [{:keys [id pname vendor]} products-results]
                                      {:key id
                                       :text (str pname
                                                  (when-not (= pname (:oname vendor))
                                                    (str " by " (:oname vendor))))
                                       :value id}))
        products (r/atom (->> round-product
                              (map (comp :id :product))
                              distinct
                              vec))
        products-options& (r/atom (->> round-product
                                       (map :product)
                                       products-results->options))
        products-search-query& (r/atom "")
        
        bad-input& (rf/subscribe [:bad-input])]
    (fn [{round-id :id
          round-product :round-product
          :as round}]
      (let [db-topic-options (if (= :loading @topic-options)
                               []
                               (map #(hash-map :key (str (:id %))
                                               :text (:prompt %)
                                               :value (str (:id %)))
                                    (:prompts @topic-options)))
            products-results& (rf/subscribe
                               [:gql/q
                                {:queries
                                 [[:products {:_where
                                              {:_and ;; while this trims search-query, the Dropdown's local search doesn't...
                                               [{:pname {:_ilike (str "%" (s/trim @products-search-query&) "%")}}
                                                {:deleted {:_is_null true}}]}
                                              :_limit 100
                                              :_order_by {:pname :asc}}
                                   [:id :pname
                                    [:vendor
                                     [:oname]]]]]}])
            _ (when-not (= :loading @products-results&)
                (let [options (->> @products-results&
                                   :products
                                   products-results->options ; now we have options from gql sub
                                   ;; (this dumbly actually keeps everything, but that seems fine)
                                   (concat @products-options&) ; keep options for the current values
                                   distinct)]
                  (when-not (= @products-options& options)
                    (reset! products-options& options))))]
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
                            :options (concat db-topic-options @new-topic-options)
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
                                      (let [db-topic-set (set (map :value db-topic-options))
                                            needs-prefix-added? (some-fn db-topic-set              
                                                                         #(s/starts-with? % "new-topic/"))]
                                        (->> (.-value this)
                                             (map #(if (needs-prefix-added? %)
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
          [:> ui/FormField
           [:label "Are there specific products you want to include?"]
           [:> ui/Dropdown {:loading (= :loading @products-results&)
                            :options @products-options&
                            :value @products
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
                                              (swap! products-options& concat)))
                            :onSearchChange (fn [_ this] (reset! products-search-query& (aget this "searchQuery")))
                            :onChange (fn [_ this]
                                        (reset! products (.-value this)))}]]
          [:> ui/FormTextArea
           {:label "Is there any additional information you would like to provide? (optional)"
            :placeholder "E.g., we've been using XYZ product, but it doesn't have the ability to integrate with ABC system."
            :on-change (fn [e this]
                         (reset! goal (.-value this)))}]
          [:> ui/FormButton
           {:color "blue"
            :on-click #(rf/dispatch [:b/round.initiation-form.submit
                                     round-id
                                     {:goal @goal
                                      :start-using @start-using
                                      :num-users @num-users
                                      :budget @budget
                                      :topics @topics
                                      :products (js->clj @products)}])}
           "Submit"]]
         [c-topics-explainer-modal topics-explainer-modal-showing?&]]))))
