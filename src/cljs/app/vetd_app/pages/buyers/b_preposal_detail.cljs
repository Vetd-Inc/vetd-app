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

;; Subscriptions
(rf/reg-sub
 :preposal-id
 :<- [:page-params] 
 (fn [{:keys [preposal-id]}] preposal-id))

;; Components
(defn c-preposal
  "Component to display preposal details."
  [{:keys [id product from-org responses]}]
  (let [pricing-estimate-value (docs/get-field-value responses "Pricing Estimate" "value" :nval)
        pricing-estimate-unit (docs/get-field-value responses "Pricing Estimate" "unit" :sval)
        free-trial? (= "yes" (docs/get-field-value responses "Do you offer a free trial?" "value" :sval))]    
    [:div
     [:> ui/ItemImage {:class "product-logo" ; todo: make config var 's3-base-url'
                       :src (str "https://s3.amazonaws.com/vetd-logos/" (:logo product))}]
     [:> ui/ItemContent
      [:> ui/ItemHeader
       (:pname product) " " [:small " by " (:oname from-org)]]
      [:> ui/ItemMeta
       [:span
        (format/currency-format pricing-estimate-value)
        " / "
        pricing-estimate-unit
        " "
        [:small "(estimate)"]]]
      [:> ui/ItemDescription (:short-desc product)]
      [:> ui/ItemExtra
       (for [c (:categories product)]
         ^{:key (:id c)}
         [:> ui/Label
          {:class "category-tag"
           ;; use the below two keys when we make category tags clickable
           ;; :as "a"
           ;; :onClick #(println "category search: " (:id c))
           }
          (:cname c)])
       (when free-trial? [:> ui/Label {:class "free-trial-tag"
                                       :color "teal"
                                       :size "small"
                                       :tag true}
                          "Free Trial"])]]
     (when (not-empty (:rounds product))
       [:> ui/Label {:color "blue"
                     :attached "bottom right"}
        "VetdRound In Progress"])]))

(defn c-page []
  (let [preposal-id& (rf/subscribe [:preposal-id])
        org-id& (rf/subscribe [:org-id])
        preps& (rf/subscribe [:gql/sub
                              {:queries
                               [[:docs {:dtype "preposal"
                                        :id @preposal-id&}
                                 [:id :idstr :title
                                  [:product [:id :pname :logo :short-desc
                                             [:rounds {:buyer-id @org-id&
                                                       :status "active"}
                                              [:id :created :status]]
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
      [:div.container-with-sidebar
       [:div.sidebar
        (when (empty? (-> @preps& :docs first :product :rounds))
          [:> ui/Button {:onClick #(rf/dispatch [:b/start-round :product (-> @preps& :docs first :product :id)])
                         :color "blue"}
           "Start VetdRound"])
        ]
       [:> ui/Segment {:class "inner-container"}
        (if (= :loading @preps&)
          [:> ui/Loader {:active true :inline true}]
          [c-preposal (-> @preps& :docs first)])]])))
