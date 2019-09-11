(ns vetd-app.buyers.pages.round-detail.index
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.pages.round-detail.initiation :as initiation]
            [vetd-app.buyers.pages.round-detail.grid :as grid]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(def init-db
  {:topics {:loading? true
            :data []}
   :products-order []})

;;;; Subscriptions
(rf/reg-sub
 :round-idstr
 :<- [:page-params] 
 (fn [{:keys [round-idstr]}] round-idstr))

(rf/reg-sub
 :b/topics
 (fn [{:keys [topics]}] topics))

(rf/reg-sub
 :b/topics.loading?
 :<- [:b/topics]
 (fn [{:keys [loading?]}] loading?))

(rf/reg-sub
 :b/topics.data
 :<- [:b/topics]
 (fn [{:keys [data]}] data))

;;;; Events
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

;;;; Components
(defn c-round-initiation
  [{:keys [id status title products init-doc] :as round}]
  (if init-doc
    "You have already submitted your requirements." ; this should never show
    [initiation/c-round-initiation-form id]))

(defn c-explainer-modal
  [modal-showing?&]
  [cc/c-modal {:showing?& modal-showing?&
               :header "How VetdRounds Work"
               :content [:<>
                         [:div.explainer-section
                          [:h3 "Keep Track"]
                          [:div.explainer-item
                           [:h4 "Approve or Disapprove every response "
                            [:> ui/Icon {:name "thumbs up outline"}]
                            [:> ui/Icon {:name "thumbs down outline"}]]
                           "Approve or Disapprove the responses you receive to keep track of which products best meet your needs."]]
                         [:div.explainer-section
                          [:h3.teal "Learn More"]
                          [:div.explainer-item
                           [:h4 
                            "Add a Topic "
                            [:> ui/Icon {:name "plus"}]]
                           "Request that all vendors respond to a new requirement / use case."]
                          [:div.explainer-item
                           [:h4 
                            "Add a Product "
                            [:> ui/Icon {:name "plus"}]]
                           "Add specific products to your VetdRound, and we'll get them to respond to your topics."]
                          [:div.explainer-item
                           [:h4 "Ask Questions "
                            [:> ui/Icon {:name "chat outline"}]]
                           "Ask vendors follow-up questions about their responses."]]
                         [:div.explainer-section
                          [:h3.blue "Make a Decision"]
                          [:div.explainer-item
                           [:h4 
                            "Disqualify Products "
                            [:> ui/Icon {:name "ban"}]]
                           "Mark products as unsatisfactory and hide them on the grid. Don't worry, you can change your mind later!"]
                          [:div.explainer-item
                           [:h4 
                            "Set Up a Call "
                            [:> ui/Icon {:name "call"}]]
                           "(Optional) Have Vetd set up a phone call with your top choices."]
                          [:div.explainer-item
                           [:h4 
                            "Declare a Winner "
                            [:> ui/Icon {:name "check"}]]
                           "Let the vendor know that you have made a final decision."]]]}])

(defn c-round
  "Component to display round details."
  [round req-form-template round-product show-top-scrollbar? explainer-modal-showing?&]
  (let [share-modal-showing?& (r/atom false)]
    (fn [{:keys [id status title products] :as round}
         req-form-template
         round-product
         show-top-scrollbar?
         explainer-modal-showing?&]
      [:<>
       [:> ui/Segment {:id "round-title-container"
                       :style {:margin-bottom 14}
                       :class (str "detail-container " (when (> (count title) 40) "long"))}
        [:h1.round-title title
         [:> ui/Button {:onClick #(reset! share-modal-showing?& true)
                        :color "lightblue"
                        :icon true
                        :labelPosition "right"
                        :floated "right"}
          "Share"
          [:> ui/Icon {:name "share"}]]]
        (when (and (#{"in-progress" "complete"} status)
                   (seq round-product))
          [:<>
           [:a {:on-click #(reset! explainer-modal-showing?& true)
                :style {:font-size 13}}
            [:> ui/Icon {:name "question circle"}]
            "How VetdRounds Work"]
           [c-explainer-modal explainer-modal-showing?&]])
        [bc/c-round-status status]
        (when (and (#{"in-progress" "complete"} status)
                   (empty? round-product))
          [:<>
           [:> ui/Header "Your VetdRound is in progress!"]
           [:p
            [:em "We will provide responses to your selected topics from top vendors shortly. "]
            [:br][:br]
            "If there are specific products you would like to have Vetd evaluate, feel free "
            "to add them by clicking the Add Products button."]])]
       (when (= status "initiation")
         [:> ui/Segment {:class "detail-container"
                         :style {:margin-left 20}}
          [c-round-initiation round]])
       [bc/c-share-modal id title share-modal-showing?&]])))

(defn sort-round-products
  [round-product]
  (sort-by (juxt #(- 1 (or (:result %) 0.5))
                 #(:sort % 1)
                 (comp :pname :product))
           compare round-product))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        round-idstr& (rf/subscribe [:round-idstr])
        rounds& (rf/subscribe [:gql/sub
                               {:queries
                                [[:rounds {:idstr @round-idstr&
                                           :deleted nil}
                                  [:id :idstr :created :status :title
                                   ;; requirements form template
                                   [:req-form-template
                                    [:id
                                     [:prompts {:ref-deleted nil
                                                :_order_by {:sort :asc}}
                                      [:id :idstr :prompt :descr :sort]]]]
                                   ;; round initiation form response
                                   [:init-doc
                                    [:id
                                     [:response-prompts {:ref-deleted nil}
                                      [:id :prompt-id :prompt-prompt :prompt-term
                                       [:response-prompt-fields
                                        [:id :prompt-field-fname :idx
                                         :sval :nval :dval]]]]]]
                                   ;; requirements responses from vendors
                                   [:round-product {:deleted nil
                                                    :_order_by {:sort :asc}}
                                    [:id :result :reason :sort
                                     [:product
                                      [:id :idstr :pname
                                       [:docs {:dtype "preposal" ; completed preposals
                                               :to-org-id @org-id&}
                                        [:id :idstr]]
                                       [:vendor
                                        [:id :oname]]]]
                                     [:vendor-response-form-docs
                                      [:id :title :doc-id :doc-title
                                       :ftype :fsubtype
                                       [:doc-from-org [:id :oname]]
                                       [:doc-to-org [:id :oname]]
                                       [:response-prompts {:ref-deleted nil}
                                        [:id :prompt-id :prompt-prompt :prompt-term
                                         [:response-prompt-fields
                                          [:id :prompt-field-fname :idx :resp-id
                                           :sval :nval :dval]]
                                         [:subject-of-response-prompt
                                          {:deleted nil
                                           :prompt-term "round.response/rating"}
                                          [[:response-prompt-fields
                                            {:deleted nil}
                                            [:nval]]]]]]]]]]]]]}])
        explainer-modal-showing?& (r/atom false)]
    (fn []
      (if (= :loading @rounds&)
        [cc/c-loader]
        (let [{:keys [status req-form-template round-product] :as round} (-> @rounds& :rounds first)
              sorted-round-products (sort-round-products round-product)
              show-top-scrollbar? (> (count sorted-round-products) 4)]
          [:<>
           [:> ui/Container {:class "main-container"
                             :style {:padding-top 0}}
            [:div.container-with-sidebar.round-details
             [:<> ; sidebar margins (and detail container margins) are customized on this page
              [:div.sidebar {:style {:margin-right 0}}
               [:div {:style {:padding "0 15px"}}
                [bc/c-back-button {:on-click #(rf/dispatch [:b/nav-rounds])}
                 "All VetdRounds"]]
               (when (#{"in-progress" "complete"} status)
                 (when-not (some (comp (partial = 1) :result) sorted-round-products) ; has a winner
                   [:<>
                    [:> ui/Segment
                     [grid/c-add-requirement-button round]
                     [grid/c-add-product-button round]]]))]
              [:div.inner-container [c-round round req-form-template sorted-round-products show-top-scrollbar? explainer-modal-showing?&]]]]]
           (when (and (#{"in-progress" "complete"} status)
                      (seq sorted-round-products))
             [grid/c-round-grid round req-form-template sorted-round-products show-top-scrollbar?])])))))
