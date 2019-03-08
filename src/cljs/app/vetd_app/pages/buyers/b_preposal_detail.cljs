(ns vetd-app.pages.buyers.b-preposal-detail
  (:require [vetd-app.flexer :as flx]
            [vetd-app.ui :as ui]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

;; Events
(rf/reg-event-fx
 :b/nav-preposal-detail
 (fn [_ [_ preposal-id]]
   {:nav {:path (str "/b/preposals/" preposal-id)}}))

(rf/reg-event-db
 :b/route-preposal-detail
 (fn [db [_ preposal-id]]
   (assoc db
          :page :b/preposal-detail
          :page-params {:preposal-id preposal-id})))


;; Components
(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])]
    (fn []
      [:div
       "Preposal detail"
       ])))
