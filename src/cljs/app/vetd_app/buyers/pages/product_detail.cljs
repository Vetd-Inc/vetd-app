(ns vetd-app.buyers.pages.product-detail
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
 (fn [{:keys [db]} [_ etype eid ename]]
   (let [qid (get-next-query-id)]
     {:ws-send {:payload {:cmd :b/request-complete-profile
                          :return {:handler :b/request-complete-profile-return}
                          :etype etype
                          :eid eid
                          :buyer-id (util/db->current-org-id db)}}
      :analytics/track {:event "Request"
                        :props {:category (str (s/capitalize (name etype)) " Profile")
                                :label ename}}})))

(rf/reg-event-fx
 :b/request-complete-profile-return
 (constantly
  {:toast {:type "success"
           :title "Complete Profile Requested"
           :message "We'll let you know when the profile is completed."}}))

(rf/reg-event-fx
 :b/setup-call
 (fn [{:keys [db]} [_ product-id product-name]]
   (let [qid (get-next-query-id)]
     {:ws-send {:payload {:cmd :b/setup-call
                          :return {:handler :b/setup-call-return}
                          :product-id product-id
                          :buyer-id (util/db->current-org-id db)}}
      :analytics/track {:event "Setup Call"
                        :props {:category "Product"
                                :label product-name}}})))

(rf/reg-event-fx
 :b/setup-call-return
 (constantly
  {:toast {:type "success"
           :title "Setup a Call"
           :message "We'll setup a call for you soon."}}))

(rf/reg-event-fx
 :b/ask-a-question
 (fn [{:keys [db]} [_ product-id product-name message]]
   (let [qid (get-next-query-id)]
     {:ws-send {:payload {:cmd :b/ask-a-question
                          :return {:handler :b/ask-a-question-return}
                          :product-id product-id
                          :message message
                          :buyer-id (util/db->current-org-id db)}}
      :analytics/track {:event "Ask A Question"
                        :props {:category "Product"
                                :label product-name}}})))

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

(defn c-product
  "Component to display Product details."
  [{:keys [id pname logo form-docs vendor forms rounds categories] :as product}]
  (let [product-profile-responses (-> form-docs first :responses)
        v (fn [prompt & [field value]]
            (docs/get-field-value product-profile-responses prompt (or field "value") (or value :sval)))]
    [:<>
     [:> ui/Segment {:class "detail-container"}
      [:h1.product-title
       pname " " [:small " by " (:oname vendor)]]
      [:> ui/Image {:class "product-logo"
                    :src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]
      (if (not-empty (:rounds product))
        [bc/c-round-in-progress {:round-idstr (-> rounds first :idstr)
                                 :props {:ribbon "left"}}])
      [bc/c-categories product]
      (when (= "Yes" (v "Do you offer a free trial?"))
        [bc/c-free-trial-tag])
      [:> ui/Grid {:columns "equal"
                   :style {:margin-top 4}}
       [:> ui/GridRow
        [:> ui/GridColumn {:width 12}
         (or (some-> (v "Describe your product or service")
                     md/md->hiccup
                     md/component)
             [:p "No description available."])
         [:br]
         [:h3.display-field-key "Pitch"]
         [:p "Request a Preposal to get a personalized pitch."]
         [:br]
         [:h3.display-field-key "Pricing Estimate"]
         "Request a Preposal to get a personalized estimate."]
        [:> ui/GridColumn {:width 4}
         (when (bc/has-data? (v "Product Website"))
           [:<>
            [:a {:href (str (when-not (.startsWith (v "Product Website") "http") "http://") (v "Product Website"))
                 :target "_blank"}
             [:> ui/Icon {:name "external square"
                          :color "blue"}]
             "Product Website"]
            [:br]
            [:br]])
         (when (bc/has-data? (v "Product Demo"))
           [:a {:href (v "Product Demo")
                :target "_blank"}
            [:> ui/Icon {:name "external square"
                         :color "blue"}]
            "Watch Demo Video"])]]]]
     ;; [bc/c-pricing product v]
     ;; [bc/c-onboarding product v]
     ;; [bc/c-client-service product v]
     ;; [bc/c-reporting product v]
     ;; [bc/c-market-niche product v]
     ]))

(defn c-page []
  (let [product-idstr& (rf/subscribe [:product-idstr])
        org-id& (rf/subscribe [:org-id])
        products& (rf/subscribe [:gql/sub
                                 {:queries
                                  [[:products {:idstr @product-idstr&}
                                    [:id :pname :logo
                                     [:form-docs {:ftype "product-profile"
                                                  :_order_by {:created :desc}
                                                  :_limit 1}
                                      [:id 
                                       [:response-prompts {:ref-deleted nil}
                                        [:id :prompt-id :prompt-prompt :prompt-term
                                         [:response-prompt-fields
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
                                     [:rounds {:buyer-id @org-id&}
                                      [:id :idstr :created :status]]
                                     [:categories [:id :idstr :cname]]]]]}])]
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
          (let [product (-> @products& :products first)
                {:keys [docs-out id oname]} (:vendor product)]
            [:<>
             [c-product product]
             [bc/c-vendor-profile (first docs-out) id oname]]))]])))
