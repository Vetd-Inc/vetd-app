(ns vetd-app.buyers.pages.round-detail
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [markdown-to-hiccup.core :as md]
            [clojure.string :as s]))

(def last-query-id (atom 0))

(defn get-next-query-id []
  (swap! last-query-id inc))

;; Events
(rf/reg-event-fx
 :b/nav-round-detail
 (fn [_ [_ round-idstr]]
   {:nav {:path (str "/b/rounds/" round-idstr)}}))

(rf/reg-event-fx
 :b/route-round-detail
 (fn [{:keys [db]} [_ round-idstr]]
   {:db (assoc db
               :page :b/round-detail
               :page-params {:round-idstr round-idstr})
    :analytics/page {:name "Buyers Round Detail"
                     :props {:round-idstr round-idstr}}}))

;; Subscriptions
(rf/reg-sub
 :round-idstr
 :<- [:page-params] 
 (fn [{:keys [round-idstr]}] round-idstr))

;; Components
(defn c-round-initiation-form
  [round-id]
  (let [initiation-forms& (rf/subscribe
                           [:gql/sub
                            {:queries
                             [[:forms {:ftype "round-initiation"
                                       :deleted nil
                                       :_order_by {:created :desc}
                                       :_limit 1}
                               [:id :title :ftype :fsubtype
                                [:prompts {:ref-deleted nil
                                           :_order_by {:sort :asc}}
                                 [:id :idstr :prompt :descr
                                  [:fields {:deleted nil
                                            :_order_by {:sort :asc}}
                                   [:id :idstr :fname :ftype
                                    :fsubtype :list?]]]]]]]}])]
    (fn []
      (let [initiation-form (first (:forms @initiation-forms&))
            goal (r/atom "")
            start-using (r/atom "")
            num-users (r/atom "")
            budget (r/atom "")
            requirements (r/atom [])
            add-products-by-name (r/atom "")]
        [:> ui/Form {:style {:width 500}}
         [:> ui/FormTextArea
          {:label "What are you hoping to accomplish with the product?"}]
         [:> ui/FormInput
          {:label "When would you like to start using the product?"}]
         [:> ui/FormField
          [:label "How many people will be using the product?"]
          [:> ui/Input {:labelPosition "right"}
           [:input {:type "number"}]
           [:> ui/Label "users"]]]
         [:> ui/FormField
          [:label "What is your annual budget?"]
          [:> ui/Input {:labelPosition "right"}
           [:> ui/Label {:basic true} "$"]
           [:input {:type "number"}]
           [:> ui/Label " per year"]]]
         [:> ui/FormInput
          {:label "What are your product requirements?"}]
         [:> ui/FormInput
          {:label "Are there specific products you want to include?"}]
         [:> ui/FormButton "Submit"]
         
         
         


         ]
        
        #_[docs/c-form-maybe-doc
           (docs/mk-form-doc-state (assoc initiation-form :round-id round-id))
           {:show-submit true}]
        ))))

(defn c-round-initiation
  [{:keys [id status title products doc] :as round}]
  (if doc
    (println doc)
    [c-round-initiation-form id]))

(defn c-round-grid
  [round]
  "Show the grid.")

(defn c-round
  "Component to display Round details."
  [{:keys [id status title products] :as round}]
  [:<>
   [bc/c-round-status status]
   [:> ui/Segment {:class "detail-container"}
    [:h1.product-title title]
    (case status
      "initiation" [c-round-initiation round]
      "in-progress" [c-round-grid round]
      "complete" [c-round-grid round])]])

(defn c-page []
  (let [round-idstr& (rf/subscribe [:round-idstr])
        org-id& (rf/subscribe [:org-id])
        rounds& (rf/subscribe [:gql/sub
                               {:queries
                                [[:rounds {:idstr @round-idstr&}
                                  [:id :idstr :created :status :title
                                   [:products [:pname]]
                                   [:doc [:id
                                          [:response-prompts {:ref-deleted nil}
                                           [:id :prompt-id :prompt-prompt :prompt-term
                                            [:response-prompt-fields
                                             [:id :prompt-field-fname :idx
                                              :sval :nval :dval]]]]]]]]]}])]
    (fn []
      [:div.container-with-sidebar
       [:div.sidebar
        [:div {:style {:padding "0 15px"}}
         [bc/c-back-button {:on-click #(rf/dispatch [:b/nav-rounds])}
          "All VetdRounds"]]
        #_(when-not (= :loading @products&)
            (let [{:keys [vendor rounds] :as product} (-> @products& :products first)]
              (when (empty? (:rounds product))
                [:> ui/Segment
                 [bc/c-start-round-button {:etype :product
                                           :eid (:id product)
                                           :ename (:pname product)
                                           :props {:fluid true}}]
                 [c-preposal-request-button product]
                 [bc/c-setup-call-button product vendor]
                 [bc/c-ask-a-question-button product vendor]])))]
       [:div.inner-container
        (if (= :loading @rounds&)
          [cc/c-loader]
          (let [round (-> @rounds& :rounds first)]
            [c-round round]))]])))
