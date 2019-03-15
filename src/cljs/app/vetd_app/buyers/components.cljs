(ns vetd-app.buyers.components
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]))

(defn c-start-round-button [{:keys [etype eid props]}]
  [:> ui/Button
   (merge {:onClick #(rf/dispatch [:b/start-round etype eid])
           :class "start-round-button"
           :color "blue"
           :icon true
           :labelPosition "right"}
          props)
   "Start VetdRound"
   [:> ui/Icon {:name "right arrow"}]])

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
    [:> ui/Popup
     {:content (str "Find and compare similar products to "
                    (:pname product) " that meet your needs.")
      :header "What is a VetdRound?"
      :position "bottom left"
      :trigger (r/as-element
                [c-start-round-button {:etype :product
                                       :eid (:id product)}])}]))

(defn c-categories
  "Given a product map, display the categories as tags."
  [product]
  [:<>
   (for [c (:categories product)]
     ^{:key (:id c)}
     [:> ui/Label {:class "category-tag"
                   ;; use the below two keys to make category tags clickable
                   ;; :as "a"
                   ;; :onClick #(println "category search: " (:id c))
                   }
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
   [:> ui/Segment {:style {:padding "40px 20px 10px 20px"}}
    [:> ui/Label {:attached "top"} field-key]
    field-value]])
