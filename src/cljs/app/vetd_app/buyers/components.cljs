(ns vetd-app.buyers.components
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]))

(defn c-start-round-button [{:keys [etype eid ename show-icon? props]}]
  [:> ui/Popup
   {:content (str "Find and compare similar products to \""
                  ename "\" that meet your needs.")
    :header "What is a VetdRound?"
    :position "top left"
    :trigger (r/as-element
              [:> ui/Button
               (merge {:onClick #(do (.stopPropagation %)
                                     (rf/dispatch [:b/start-round etype eid]))
                       :class "start-round-button"
                       :color "blue"}
                      props
                      (when show-icon?
                        {:icon true
                         :labelPosition "right"}))
               "Start VetdRound"
               (when show-icon?
                 [:> ui/Icon {:name "right arrow"}])])}])

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

(defn c-vendor-profile
  [{:keys [responses] :as vendor-profile-doc}]
  (let [website-url (docs/get-field-value responses "Website" "value" :sval)
        funding-status (docs/get-field-value responses "Funding Status" "value" :sval)
        year-founded (docs/get-field-value responses "Year Founded" "value" :sval)
        headquarters (docs/get-field-value responses "Headquarters Location" "value" :sval)
        num-employees (docs/get-field-value responses "Employee Count" "value" :nval)]
    [:> ui/Segment {:class "detail-container vendor-profile"}
     [:h1.title "Company Profile"]
     [:> ui/Grid {:columns "equal"
                  :style {:margin-top 0}}
      [:> ui/GridRow
       [c-display-field {:width 8} "Website"
        [:a {:href website-url
             :target "_blank"}
         [:> ui/Icon {:name "external square"
                      :color "blue"}]
         website-url]]
       (when (has-data? headquarters)
         [c-display-field {:width 8} "Headquarters" headquarters])]
      [:> ui/GridRow
       (when (has-data? funding-status)
         [c-display-field {:width 5} "Funding Status" funding-status])
       (when (has-data? year-founded)
         [c-display-field {:width 5} "Year Founded" year-founded])
       (when (has-data? num-employees)
         [c-display-field {:width 6} "Number of Employees" (util/decimal-format num-employees)])]]]))
