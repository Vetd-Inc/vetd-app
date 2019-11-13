(ns vetd-app.buyers.pages.round-detail.index
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            vetd-app.buyers.pages.round-detail.subs
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
  {:products-order []})

;;;; Subscriptions
(rf/reg-sub
 :round-idstr
 :<- [:page-params] 
 (fn [{:keys [round-idstr]}] round-idstr))

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

;; sets round buyer-name for use locally (app-db)
(rf/reg-event-fx
 :b/set-buyer-name
 (fn [{:keys [db]} [_ buyer-name]]
   {:db (assoc-in db [:round :buyer-name] buyer-name)}))

(rf/reg-event-fx
 :b/set-buyer-id
 (fn [{:keys [db]} [_ buyer-id]]
   {:db (assoc-in db [:round :buyer-id] buyer-id)}))

(rf/reg-event-fx
 :b/set-status
 (fn [{:keys [db]} [_ status]]
   {:db (assoc-in db [:round :status] status)}))

;;;; Components
(defn c-round-initiation
  [{:keys [init-doc] :as round}]
  (if init-doc
    "You have already submitted your requirements." ; this should never show
    [initiation/c-round-initiation-form round]))

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
  [round req-form-template round-product show-top-scrollbar? read-only? explainer-modal-showing?&]
  (let [share-modal-showing?& (r/atom false)
        buyer?& (rf/subscribe [:b/round.buyer?])
        buyer-name& (rf/subscribe [:b/round.buyer-name])]
    (fn [{:keys [id status title products] :as round}
         req-form-template
         round-product
         show-top-scrollbar?
         read-only?
         explainer-modal-showing?&]
      [:<>
       [:> ui/Segment {:id "round-title-container"
                       :style {:margin-bottom 14}
                       :class (str "detail-container " (when (> (count title) 40) "long"))}
        [:<>
         [:h1.round-title title
          (when-not @buyer?&
            ;; [:> ui/Label {:color "white"}
            ;;  "created by " @buyer-name& ""]
            [:small {:style {:font-size 14
                             :font-weight 400
                             :position "relative"
                             :top -2
                             :left 5}}
             " (created by " @buyer-name& ")"]
            )
          [:> ui/Button {:onClick #(reset! share-modal-showing?& true)
                         :color "lightblue"
                         :icon true
                         :labelPosition "right"
                         :floated "right"}
           "Share" ;; this feature needs to handle buyer?s and visitors
           [:> ui/Icon {:name "share"}]]]
         (when (and (#{"in-progress" "complete"} status)
                    (seq round-product)
                    (not read-only?))
           [:<>
            [:a {:on-click #(reset! explainer-modal-showing?& true)
                 :style {:font-size 13}}
             [:> ui/Icon {:name "question circle"}]
             "How VetdRounds Work"]
            [c-explainer-modal explainer-modal-showing?&]])
         [bc/c-round-status status]
         (when-not @buyer?&
           (if (#{"complete"} status)
             [:p "View the products that " @buyer-name& " compared and read vendor responses to their top questions."]
             [:p "View the products that " @buyer-name& " is currently comparing and read vendor responses to their top questions."]))
         (when (and (#{"in-progress" "complete"} status)
                    (empty? round-product))
           (if @buyer?&
             [:<>
              [:> ui/Header "Your VetdRound is in progress!"]
              [:p
               [:em "We will provide responses to your selected topics from top vendors shortly."]
               [:br][:br]
               "If there are specific products you would like to have Vetd evaluate, feel free "
               "to add them by clicking the Add Products button."]]
             [:<>
              [:p {:style {:margin-top 10}}
               [:em "This VetdRound does not currently have any products in it."]]]))]]
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
        explainer-modal-showing?& (r/atom false)
        buyer?& (rf/subscribe [:b/round.buyer?])
        read-only?& (rf/subscribe [:b/round.read-only?])]
    (fn []
      (let [round-detail& (rf/subscribe
                           (vetd-app.buyers.pages.round-detail.subs/mk-round-detail-gql @round-idstr& @org-id&))]
        (if (= @round-detail& :loading)
          [cc/c-loader]
          (let [{:keys [status req-form-template round-product buyer initiation-form-prefill] :as round}
                (-> @round-detail& :rounds first)

                sorted-round-products (sort-round-products round-product)
                show-top-scrollbar? (> (count sorted-round-products) 4)
                ;; TODO refactor this system
                _ (rf/dispatch [:b/set-buyer-name (:oname buyer)])
                _ (rf/dispatch [:b/set-buyer-id (:id buyer)])
                _ (rf/dispatch [:b/set-status status])]
            [:<>
             [:> ui/Container {:class "main-container"
                               :style {:padding-top 0}}
              [:div.container-with-sidebar.round-details
               [:<> ;; sidebar margins (and detail container margins) are customized
                [:div.sidebar {:style {:margin-right 0}}
                 [:div {:style {:padding "0 15px"}}
                  (if @buyer?&
                    [bc/c-back-button {:on-click #(rf/dispatch [:b/nav-rounds])}
                     "All VetdRounds"]
                    [bc/c-back-button])]
                 (if @buyer?&
                   (when (= status "in-progress")
                     [:> ui/Segment
                      [grid/c-add-requirement-button round]
                      [grid/c-add-product-button round]])
                   [:> ui/Segment
                    [bc/c-start-round-button {:etype :duplicate
                                              :defaults round
                                              :props {:fluid true}}]])]
                [:div.inner-container
                 [c-round
                  round req-form-template sorted-round-products show-top-scrollbar?
                  @read-only?& explainer-modal-showing?&]]]]]
             (when (and (#{"in-progress" "complete"} status)
                        (seq sorted-round-products))
               [grid/c-round-grid
                round req-form-template sorted-round-products
                show-top-scrollbar? @read-only?&])]))))))
