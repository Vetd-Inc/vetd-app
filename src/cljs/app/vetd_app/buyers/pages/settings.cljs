(ns vetd-app.buyers.pages.settings
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [clojure.string :as s]))

;;;; Events
(rf/reg-event-fx
 :b/nav-settings
 (constantly
  {:nav {:path "/b/settings"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Buyers Settings"}}}))

(rf/reg-event-fx
 :b/route-settings
 (fn [{:keys [db]}]
   {:db (assoc db :page :b/settings)
    :analytics/page {:name "Buyers Settings"}}))

;;;; Components
(defn c-page []
  [:> ui/Grid
   [:> ui/GridRow
    [:> ui/GridColumn {:computer 2 :mobile 0}]
    [:> ui/GridColumn {:computer 12 :mobile 16}
     [:> ui/Segment {:placeholder true}
      [:> ui/Header {:icon true}
       [:> ui/Icon {:name "settings"}]
       "Settings"]]]
    [:> ui/GridColumn {:computer 2 :mobile 0}]]])
