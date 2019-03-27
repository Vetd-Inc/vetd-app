(ns vetd-app.non-exclusive-accordion
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn dropdown-icon []
  [:> ui/Icon {:name "dropdown"}])


(defn accordion-item
  [title & body]
  (let [active& (r/atom false)]
    (fn [title & body]
      (def t1 title)
      (def b1 body)
      [:div
       [:> ui/AccordionTitle {:active @active&
                              :onClick (fn [_ this] (swap! active& not))}
        title]
       [:> ui/AccordionContent {:active @active&}
        (for [b body]
          b)]])))

(defn accordion
  [& accordion-items]
  (fn [& accordion-items]
    [:> ui/Accordion
     (for [ai accordion-items]
       ai)]))

(defn sub-accordion
  [& accordion-items]
  (fn [& accordion-items]
    [:> ui/AccordionAccordion
     (for [ai accordion-items]
       ai)]))
