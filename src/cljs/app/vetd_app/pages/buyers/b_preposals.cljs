(ns vetd-app.pages.buyers.b-preposals
  (:require [vetd-app.flexer :as flx]
            [vetd-app.ui :as ui]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

;; Events
(rf/reg-event-fx
 :b/nav-preposals
 (constantly
  {:nav {:path "/b/preposals/"}}))

(rf/reg-event-db
 :b/route-preposals
 (fn [db [_ query-params]]
   (assoc db
          :page :b/preposals
          :query-params query-params)))

;; Components
(defn c-preposal
  "Component to display Preposal as a list item."
  [{:keys [id idstr product from-org responses]}]
  (let [pricing-estimate-value (docs/get-field-value responses "Pricing Estimate" "value" :nval)
        pricing-estimate-unit (docs/get-field-value responses "Pricing Estimate" "unit" :sval)
        pricing-estimate-details (docs/get-field-value responses "Pricing Estimate" "details" :sval)
        free-trial? (= "yes" (docs/get-field-value responses "Do you offer a free trial?" "value" :sval))]
    [:> ui/Item {:onClick #(rf/dispatch [:b/nav-preposal-detail idstr])} 
     [:> ui/ItemImage {:class "product-logo" ; todo: make config var 's3-base-url'
                       :src (str "https://s3.amazonaws.com/vetd-logos/" (:logo product))}]
     [:> ui/ItemContent
      [:> ui/ItemHeader
       (:pname product) " " [:small " by " (:oname from-org)]]
      [:> ui/ItemMeta
       (if pricing-estimate-value
         [:span
          (format/currency-format pricing-estimate-value)
          " / "
          pricing-estimate-unit
          " "
          [:small "(estimate)"]]
         (when pricing-estimate-details
           (str "Price Estimate: " pricing-estimate-details)))]
      [:> ui/ItemDescription (:short-desc product)]
      [:> ui/ItemExtra
       (when (empty? (:rounds product))
         [:> ui/Button {:onClick #(rf/dispatch [:b/start-round :product (:id product)])
                        :color "blue"
                        :icon true
                        :labelPosition "right"
                        :floated "right"}
          "Start VetdRound"
          [:> ui/Icon {:name "right arrow"}]])
       (for [c (:categories product)]
         ^{:key (:id c)}
         [:> ui/Label
          {:class "category-tag"
           ;; use the below two keys when we make category tags clickable
           ;; :as "a"
           ;; :onClick #(println "category search: " (:id c))
           }
          (:cname c)])
       (when free-trial? [:> ui/Label {:class "free-trial-tag"
                                       :color "gray"
                                       :size "small"
                                       :tag true}
                          "Free Trial"])]]
     (when (not-empty (:rounds product))
       [:> ui/Label {:color "teal"
                     :attached "bottom right"}
        "VetdRound In Progress"])]))

(defn filter-preposals
  [preposals selected-categories]
  (->> (for [{:keys [product] :as preposal} preposals
             category (:categories product)]
         (when (selected-categories (:id category))
           preposal))
       (remove nil?)
       distinct))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:docs {:dtype "preposal"
                                        :to-org-id @org-id&}
                                 [:id :idstr :title
                                  [:product [:id :pname :logo :short-desc
                                             [:rounds {:buyer-id @org-id&
                                                       :status "active"}
                                              [:id :created :status]]
                                             [:categories [:id :idstr :cname]]]]
                                  [:from-org [:id :oname]]
                                  [:from-user [:id :uname]]
                                  [:to-org [:id :oname]]
                                  [:to-user [:id :uname]]
                                  [:responses
                                   [:id :prompt-id :notes
                                    [:prompt
                                     [:id :prompt]]
                                    [:fields
                                     [:id :pf-id :idx :sval :nval :dval
                                      [:prompt-field [:id :fname]]]]]]]]]}])
        ;; a set of category ID's to allow through filter
        ;; if empty, let all categories through
        selected-categories (r/atom #{})]
    (fn []
      [:div.container-with-sidebar
       [:div.sidebar
        "Filter By Category"
        (let [categories (->> @preps&
                              :docs
                              (map (comp :categories :product))
                              flatten
                              (map #(select-keys % [:id :cname]))
                              (group-by :id))]
          (for [[id v] categories]
            ^{:key id} 
            [:> ui/Checkbox {:label (str (-> v first :cname) " (" (count v) ")")
                             :onChange (fn [_ this]
                                         (if (.-checked this)
                                           (swap! selected-categories conj id)
                                           (swap! selected-categories disj id)))}]))]
       [:> ui/ItemGroup {:class "inner-container results"}
        (if (= :loading @preps&)
          [:> ui/Loader {:active true :inline true}]
          (for [preposal (cond-> (:docs @preps&)
                           (seq @selected-categories) (filter-preposals
                                                       @selected-categories))]
            ^{:key (:id preposal)}
            [c-preposal preposal]))]])))
