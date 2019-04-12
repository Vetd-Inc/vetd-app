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
(defn c-round-initiation
  [round]
  (let [initiation-doc& (rf/subscribe
                         [:gql/sub
                          {:queries
                           [[:form-docs {:ftype "round-initiation"
                                         ;; :doc-from-org-id @org-id&
                                         :_order_by {:created :desc}
                                         :_limit 1}
                             [:id :title :doc-id :doc-title
                              :ftype :fsubtype
                              [:doc-from-org [:id :oname]]
                              [:doc-to-org [:id :oname]]
                              [:prompts {:ref-deleted nil
                                         :_order_by {:sort :asc}}
                               [:id :idstr :prompt :descr #_:sort ;; TODO sort
                                [:fields {:deleted nil
                                          :_order_by {:sort :asc}}
                                 [:id :idstr :fname :ftype
                                  :fsubtype :list? #_:sort]]]]
                              [:responses
                               {:ref-deleted nil}
                               [:id :prompt-id :notes
                                [:fields {:deleted nil}
                                 [:id :pf-id :idx :sval :nval :dval :jval]]]]]]]}])]
    (fn []
      (if (= :loading @initiation-doc&)
        [cc/c-loader]
        (if-let [form-doc (some-> @initiation-doc&
                                  :form-docs
                                  first)]
          [docs/c-form-maybe-doc
           (docs/mk-form-doc-state (assoc form-doc
                                          :to-org nil ;; (:doc-from-org form-doc)
                                          ))
           {:show-submit true}]
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
                                         [:id :idstr :prompt :descr #_:sort ;; TODO sort
                                          [:fields {:deleted nil
                                                    :_order_by {:sort :asc}}
                                           [:id :idstr :fname :ftype
                                            :fsubtype :list? #_:sort]]]]]]]}])
                initiation-form (first (:forms @initiation-forms&))
                _ (println initiation-form)]
            [docs/c-form-maybe-doc (docs/mk-form-doc-state (assoc initiation-form
                                                                  ;; this is reversed because of preposal request logic
                                                                  :to-org nil ;; {:id @org-id&}
                                                                  ))
             {:show-submit true}]))))))

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
                                   [:products [:pname]]]]]}])]
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
