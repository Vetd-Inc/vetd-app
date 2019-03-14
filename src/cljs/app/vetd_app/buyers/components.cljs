(ns vetd-app.buyers.components
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]))

(defn c-rounds
  "Given a product map, display the Round data."
  [product]
  (if (not-empty (:rounds product))
    [:> ui/Label {:color "teal"
                  :size "medium"
                  :ribbon "top left"}
     "VetdRound In Progress"]
    [:> ui/Popup
     {:content (str "Find and compare similar products to " (:pname product) " that meet your needs.")
      :header "What is a VetdRound?"
      :position "bottom left"
      :trigger (r/as-element
                [:> ui/Button {:onClick #(rf/dispatch [:b/start-round :product (:id product)])
                               :color "blue"
                               :icon true
                               :labelPosition "right"
                               :style {:marginRight 15}}
                 "Start VetdRound"
                 [:> ui/Icon {:name "right arrow"}]])}]))

(defn c-categories
  "Given a product map, display the categories as tags."
  [product]
  [:<>
   (for [c (:categories product)]
     ^{:key (:id c)}
     [:> ui/Label {:class "category-tag"}
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
