(ns vetd-app.buyers.pages.product-detail
  (:require [vetd-app.buyers.components :as bc]
            [vetd-app.common.components :as cc]
            [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [reagent.format :as format]
            [re-frame.core :as rf]
            [clojure.string :as s]))

;; Events
(rf/reg-event-fx
 :b/nav-product-detail
 (fn [_ [_ product-idstr]]
   {:nav {:path (str "/b/products/" product-idstr)}}))

(rf/reg-event-fx
 :b/route-product-detail
 (fn [{:keys [db]} [_ product-idstr]]
   {:db (assoc db
               :page :b/product-detail
               :page-params {:product-idstr product-idstr})
    :analytics/page {:name "Buyers Product Detail"
                     :props {:product-idstr product-idstr}}}))

(rf/reg-event-fx
 :b/request-complete-profile
 (fn [{:keys [db]} [_ etype eid ename field-key]]
   {:ws-send {:payload {:cmd :b/request-complete-profile
                        :return {:handler :b/request-complete-profile-return}
                        :etype etype
                        :eid eid
                        :field-key field-key
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Request"
                      :props {:category (str (s/capitalize (name etype)) " Profile")
                              :label (str ename " - " field-key)}}}))

(rf/reg-event-fx
 :b/request-complete-profile-return
 (constantly
  {:toast {:type "success"
           :title "Complete Profile Requested"
           :message "We'll let you know when the profile is completed."}}))

(rf/reg-event-fx
 :b/setup-call
 (fn [{:keys [db]} [_ product-id product-name]]
   {:ws-send {:payload {:cmd :b/setup-call
                        :return {:handler :b/setup-call-return}
                        :product-id product-id
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Set Up Call"
                      :props {:category "Product"
                              :label product-name}}}))

(rf/reg-event-fx
 :b/setup-call-return
 (constantly
  {:toast {:type "success"
           :title "Set Up a Call"
           :message "We'll set up a call for you soon."}}))

(rf/reg-event-fx
 :b/ask-a-question
 (fn [{:keys [db]} [_ product-id product-name message]]
   {:ws-send {:payload {:cmd :b/ask-a-question
                        :return {:handler :b/ask-a-question-return}
                        :product-id product-id
                        :message message
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Ask A Question"
                      :props {:category "Product"
                              :label product-name}}}))

;; :b/round.ask-a-question also points to this
(rf/reg-event-fx
 :b/ask-a-question-return
 (constantly
  {:toast {:type "success"
           :title "Question Sent!"
           :message "We'll get an answer for you soon."}}))

;; Subscriptions
(rf/reg-sub
 :product-idstr
 :<- [:page-params] 
 (fn [{:keys [product-idstr]}] product-idstr))

;; Components
(defn c-preposal-request-button
  [{:keys [vendor forms] :as product}]
  (if (not-empty forms) ; already requested preposal
    [:> ui/Popup
     {:content "We will be in touch with next steps."
      :header "Preposal Requested!"
      :position "bottom left"
      :trigger (r/as-element
                [:> ui/Label {:color "teal"
                              :size "large"
                              :basic true
                              :style {:display "block"
                                      :text-align "center"}}
                 "Preposal Requested"])}]
    [:> ui/Popup
     {:content (str "Get a pricing estimate, personalized pitch, and more from "
                    (:oname vendor) ".")
      :header "What is a Preposal?"
      :position "bottom left"
      :trigger (r/as-element
                [:> ui/Button {:onClick #(rf/dispatch [:b/create-preposal-req product vendor])
                               :color "teal"
                               :fluid true
                               :icon true
                               :labelPosition "left"}
                 "Request Preposal"
                 [:> ui/Icon {:name "wpforms"}]])}]))

(defn c-product-header-segment
  [{:keys [vendor rounds pname logo] :as product} v-fn]
  [:> ui/Segment {:class "detail-container"}
   [:h1.product-title
    pname " " [:small " by " (:oname vendor)]]
   [:> ui/Image {:class "product-logo"
                 :src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]
   (when (not-empty (:rounds product))
     [bc/c-round-in-progress {:round-idstr (-> rounds first :idstr)
                              :props {:ribbon "left"}}])
   [bc/c-categories product]
   (when (= "Yes" (v-fn :product/free-trial?))
     [bc/c-free-trial-tag])
   [:> ui/Grid {:columns "equal"
                :style {:margin-top 4}}
    [:> ui/GridRow
     [:> ui/GridColumn {:width 12}
      (or (util/parse-md (v-fn :product/description))
          [:p "No description available."])
      [:br]
      [:h3.display-field-key "Preposal Pitch"]
      [:p "Request a Preposal to get a personalized pitch."]
      [:br]
      [:h3.display-field-key "Preposal Pricing Estimate"]
      "Request a Preposal to get a personalized estimate."]
     [:> ui/GridColumn {:width 4}
      (when-let [website-url (v-fn :product/website)]
        [:<>
         [bc/c-external-link website-url "Product Website"]
         [:br]
         [:br]])
      (when-let [demo-url (v-fn :product/demo)]
        [:<>
         [bc/c-external-link demo-url "Watch Demo Video"]
         [:br]
         [:br]])]]]])

(defn c-product
  "Component to display Product details."
  [{:keys [id pname form-docs vendor] :as product}]
  (let [v-fn (partial docs/get-value-by-term (-> form-docs first :response-prompts))
        c-display-field (bc/requestable
                         (partial bc/c-display-field* {:type :product
                                                       :id id
                                                       :name pname}))]
    [:<>
     [c-product-header-segment product v-fn]
     [bc/c-pricing c-display-field v-fn]
     [bc/c-vendor-profile (-> vendor :docs-out first) (:id vendor) (:oname vendor)]
     [bc/c-onboarding c-display-field v-fn]
     [bc/c-client-service c-display-field v-fn]
     [bc/c-reporting c-display-field v-fn]
     [bc/c-market-niche c-display-field v-fn]]))

(defn c-page []
  (let [product-idstr& (rf/subscribe [:product-idstr])
        org-id& (rf/subscribe [:org-id])
        products& (rf/subscribe [:gql/sub
                                 {:queries
                                  [[:products {:idstr @product-idstr&}
                                    [:id :pname :logo
                                     [:form-docs {:ftype "product-profile"
                                                  :_order_by {:created :desc}
                                                  :_limit 1
                                                  :doc-deleted nil}
                                      [:id 
                                       [:response-prompts {:deleted nil
                                                           :ref-deleted nil}
                                        [:id :prompt-id :prompt-prompt :prompt-term
                                         [:response-prompt-fields
                                          {:deleted nil
                                           :ref-deleted nil}
                                          [:id :prompt-field-fname :idx
                                           :sval :nval :dval]]]]]]
                                     [:vendor
                                      [:id :oname :url
                                       [:docs-out {:dtype "vendor-profile"
                                                   :_order_by {:created :desc}
                                                   :_limit 1}
                                        [:id
                                         [:response-prompts {:ref-deleted nil}
                                          [:id :prompt-id :prompt-prompt :prompt-term
                                           [:response-prompt-fields
                                            [:id :prompt-field-fname :idx
                                             :sval :nval :dval]]]]]]]]
                                     [:forms {:ftype "preposal" ; preposal requests
                                              :from-org-id @org-id&}
                                      [:id]]
                                     [:rounds {:buyer-id @org-id&
                                               :deleted nil}
                                      [:id :idstr :created :status]]
                                     [:categories {:ref-deleted nil}
                                      [:id :idstr :cname]]]]]}])]
    (fn []
      [:div.container-with-sidebar
       [:div.sidebar
        [:div {:style {:padding "0 15px"}}
         [bc/c-back-button {:on-click #(rf/dispatch [:b/nav-search])}
          "Back to Search"]]
        (when-not (= :loading @products&)
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
        (if (= :loading @products&)
          [cc/c-loader]
          [c-product (-> @products& :products first)])]])))
