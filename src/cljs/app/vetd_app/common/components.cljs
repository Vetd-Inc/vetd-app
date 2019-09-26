(ns vetd-app.common.components
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [clojure.string :as s]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn c-loader [{:keys [props]}]
  [:div.spinner props [:i] [:i]])

(defn c-avatar-initials
  [user-name]
  (let [parts (s/split user-name " ")]
    [:div.avatar.avatar-initials (->> (select-keys parts [0 (dec (count parts))])
                                      vals
                                      (map first)
                                      (apply str))]))

(defn c-field-container
  [& children]
  [:> ui/GridRow
   [:> ui/GridColumn {:width 16}
    [:> ui/Segment {:class "display-field"
                    :vertical true}
     (util/augment-with-keys children)]]])

(defn c-field
  [{:keys [label value]}]
  [c-field-container
   [:h3.display-field-key label]
   [:div.display-field-value value]])

(defn c-grid
  [& args]
  (let [[a & as] args
        props (when (map? a) a)
        rows (if props as args)]
    [:> ui/Grid props
     (util/augment-with-keys
      (for [row rows]
        [:> ui/GridRow
         (util/augment-with-keys
          (for [[cmp width] row]
            [:> ui/GridColumn {:width width}
             cmp]))]))])) 

(defn c-modal
  "showing?& - ratom to toggle visibility of modal"
  [{:keys [showing?& header content buttons size]}]
  [:> ui/Modal {:open @showing?&
                :on-close #(reset! showing?& false)
                :size size
                :dimmer "inverted"
                :closeOnDimmerClick true
                :closeOnEscape true
                :closeIcon true}
   [:> ui/ModalHeader header]
   [:> ui/ModalContent content]
   (when buttons
     [:> ui/ModalActions
      (for [{:keys [text event color]} buttons]
        ^{:key text}
        [:> ui/Button {:on-click (fn []
                                   (do (when event (rf/dispatch event))
                                       (reset! showing?& false)))
                       :color color}
         text])])])
