(ns vetd-app.pages.buyers.b-preposals
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

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

(defn c-preposal
  "Component to display Preposal as a list item."
  [{:keys [id idstr product from-org from-user responses] :as args}]
  (let [get-prompt-field-key-value (fn [prompt field k]
                                     (-> (group-by (comp :prompt :prompt) responses)
                                         (get prompt)
                                         first
                                         :fields
                                         (->> (group-by (comp :fname :prompt-field)))
                                         (get field)
                                         first
                                         (get k)))]
    [:div.list-item
     [:img {:src (str "https://s3.amazonaws.com/vetd-logos/" ; todo: make config var
                      (:logo product))}]
     [:div.details
      [:div.details-heading
       [:h3 (:pname product) " " [:small " by " (:oname from-org)]]
       [:div.categories
        (for [c (:categories product)]
          ^{:key (:id c)}
          [rc/button
           :label (:cname c)
           :class "btn-category btn-sm"])]]
      [:div (get-prompt-field-key-value "Pitch" "value" :sval)]
      [:div.price
       (format/currency-format
        (get-prompt-field-key-value "Pricing Estimate" "value" :nval))
       " / "
       (get-prompt-field-key-value "Pricing Estimate" "unit" :sval)
       " "
       [:small "(estimate)"]]]]))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:docs {:dtype "preposal"
                                        :to-org-id @org-id&}
                                 [:id :idstr :title
                                  [:product [:id :pname :logo
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
                                      [:prompt-field [:id :fname]]]]]]]]]}])]
    (fn []
      [flx/col
       (let [preps @preps&]
         (when-not (= :loading preps)
           (for [p (:docs preps)]
             ^{:key (:id p)}
             [c-preposal p])))])))
