(ns vetd-app.pages.buyers.b-preposals
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
            [reagent.core :as r]
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
    ;; (println (str args))
    [:div.list-item
     
     [:img {:src "https://s3.amazonaws.com/vetd-logos/1e20fc47f430315c72f4c7e5328601be.png"}]
     [:div.details
      [:h3 "Product Name" [:small " by " (:oname from-org)]]
      [:div (get-prompt-field-key-value "Pitch" "value" :sval)]]]))



(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:docs {:dtype "preposal"
                                        :to-org-id @org-id&}
                                 [:id :idstr :title
                                  [:product [:id :pname]]
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
