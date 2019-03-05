(ns vetd-app.pages.buyers.b-preposals
  (:require [vetd-app.flexer :as flx]
            [vetd-app.ui :as ui]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

;; Events
(rf/reg-event-fx
 :b/nav-preposals
 (fn []
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
  [{:keys [id product from-org responses] :as args}]
  (let [get-prompt-field-key-value (fn [prompt field k]
                                     (-> (group-by (comp :prompt :prompt) responses)
                                         (get prompt)
                                         first
                                         :fields
                                         (->> (group-by (comp :fname :prompt-field)))
                                         (get field)
                                         first
                                         (get k)))
        pricing-estimate-value (get-prompt-field-key-value "Pricing Estimate"
                                                           "value"
                                                           :nval)
        pricing-estimate-unit (get-prompt-field-key-value "Pricing Estimate"
                                                          "unit"
                                                          :sval)]    
    [:> ui/Item {:onClick #(println "go to this preposal")} ; todo: make config var 's3-base-url'
     [:> ui/ItemImage {:class "product-logo"
                       :src (str "https://s3.amazonaws.com/vetd-logos/" (:logo product))}]
     [:> ui/ItemContent
      [:> ui/ItemHeader
       (:pname product) " " [:small " by " (:oname from-org)]]
      [:> ui/ItemMeta
       [:span
        (format/currency-format pricing-estimate-value)
        " / "
        pricing-estimate-unit
        " "
        [:small "(estimate)"]]]
      ;; Pitch
      ;; [:> ui/ItemDescription
      ;;  (get-prompt-field-key-value "Pitch" "value" :sval)]
      [:> ui/ItemDescription (:short-desc product)]
      [:> ui/ItemExtra
       (for [c (:categories product)]
         ^{:key (:id c)}
         [:> ui/Label
          {:as "a"
           :class "category-tag"
           ;; :onClick #(println "category search: " (:id c))
           }
          (:cname c)])]]]))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:docs {:dtype "preposal"
                                        :to-org-id @org-id&}
                                 [:id :idstr :title
                                  [:product [:id :pname :logo :short-desc
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
        ]
    (fn []
      [:div.preposals
       [:div.refine
        "Filter By Category"
        (let [categories (->> @preps&
                              :docs
                              (map (comp :categories :product))
                              flatten
                              (map #(select-keys % [:id :cname]))
                              (group-by :id))]
          (for [[id v] categories]
            ^{:key id} 
            [:> ui/Checkbox {:label (str (-> v first :cname) " (" (count v) ")")}]))]
       [:> ui/ItemGroup {:class "results"}
        (let [preps @preps&]
          (if (= :loading preps)
            [:> ui/Loader {:active true
                           :inline true}]
            (for [p (:docs preps)]
              ^{:key (:id p)}
              [c-preposal p])))]])))
