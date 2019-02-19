(ns vetd-app.pages.buyers.b-preposals
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :b/nav-preposals
 (fn [{:keys [db]} _]
   {:nav {:path "/b/preposals/"}}))

(rf/reg-event-db
 :b/route-preposals
 (fn [db [_ query-params]]
   (assoc db
          :page :b/preposals
          :query-params query-params)))

(defn c-preposal
  [{:keys [id idstr product from-org from-user responses]}]
  [flx/col
   [:div id]
   [:div idstr]
   [:div (:pname product)]
   [:div (:oname from-org)]
   [:div (:uname from-user)]
   [:div (str responses)]])

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:docs {:dtype "preposal"
                                        :to-org-id @org-id&}
                                 [:id :idstr :title
                                  [:product [:id :pname]]
                                  [:from-org [:id :oname]]
                                  [:product [:id :pname]]
                                  [:from-user [:id :uname]]
                                  [:to-org [:id :oname]]
                                  [:to-user [:id :uname]]
                                  [:responses
                                   [:id :prompt-id :notes
                                    [:prompt
                                     [:id :prompt]]
                                    [:fields
                                     [:id :pf-id :idx :sval :nval :dval
                                      [:prompt-field [:id :fname]]]]]]]]]}])]
    (fn []
      [flx/col
       (let [preps @preps&]
         (def p1 preps)
         (when-not (= :loading preps)
           (for [p (:docs preps)]
             ^{:key (:id p)}
             [c-preposal p])))])))

