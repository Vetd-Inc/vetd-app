(ns vetd-app.nav
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(defn tab [current label disp]
  [{}
   [:a {:class (into [:tab]
                     (when (= current label)
                       [:selected]))
        :on-click #(rf/dispatch disp)}
    (name label)]])

(defn header []
  (let [page @(rf/subscribe [:page])
        tab-fn (partial tab page)]
    [ut/flexer {:p {:id :nav-header
                    :style {:align-items :center}}}
     [
      (tab-fn :home [:nav-home])
      #_(tab-fn :builder [:nav-builder])]]))




















































