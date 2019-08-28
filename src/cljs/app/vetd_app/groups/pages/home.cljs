(ns vetd-app.groups.pages.home
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [vetd-app.common.fx :as cfx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(rf/reg-event-fx
 :g/nav-home
 (constantly
  {:nav {:path "/c/home"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Groups Home"}}}))

(rf/reg-event-fx
 :g/route-home
 (fn [{:keys [db]}]
   {:db (assoc db :page :g/home)
    :analytics/page {:name "Groups Home"}}))

;;;; Components
(defn c-org
  [org group]
  (let [org-id& (rf/subscribe [:org-id])]
    (fn [{:keys [id idstr oname memberships] :as org}
         {:keys [gname] :as group}]
      (let [num-members (count memberships)]
        [cc/c-field {:label [:<>
                             [:> ui/Button {:on-click (fn []
                                                        (if (= id @org-id&)
                                                          (rf/dispatch [:b/nav-stack])
                                                          (rf/dispatch [:b/nav-stack-detail idstr])))
                                            :as "a"
                                            :size "small"
                                            :color "lightblue"
                                            :style {:float "right"
                                                    :margin-top 7}}
                              [:> ui/Icon {:name "grid layout"}]
                              ;; TODO ? number of items in stack
                              "View Stack"]
                             oname]
                     :value [:<> (str num-members " member" (when-not (= num-members 1) "s") " ")
                             [:> ui/Popup
                              {:position "bottom left"
                               :wide "very"
                               :content (let [max-members-show 15]
                                          (str (s/join ", " (->> memberships
                                                                 (map (comp :uname :user))
                                                                 (take max-members-show)))
                                               (when (> num-members max-members-show)
                                                 (str " and " (- num-members max-members-show) " more."))))
                               :trigger (r/as-element
                                         [:> ui/Icon {:name "question circle"}])}]]}]))))

(defn c-orgs
  [{:keys [id orgs] :as group}]
  [bc/c-profile-segment {:title [:<>
                                 [:> ui/Icon {:name "group"}]
                                 " Organizations"]}
   (for [org orgs]
     ^{:key (:id org)}
     [c-org org group])])

(defn c-stack-item
  [product agg-group-prod-rating]
  (let [{product-id :id
         product-idstr :idstr
         :keys [pname short-desc logo vendor
                agg-group-prod-price discounts]} product
        ;; TODO easy to optimize
        orgs-using-count (-> (filter #(= (:product-id %) product-id) agg-group-prod-rating)
                             (util/rating-avg-map {:keep-nil true})
                             :count)
        mean (->> agg-group-prod-rating
                  (filter #(= (:product-id %) product-id))
                  util/rating-avg-map
                  :mean)]
    [:> ui/Item {:on-click #(rf/dispatch [:b/nav-product-detail product-idstr])}
     [bc/c-product-logo logo]
     [:> ui/ItemContent
      [:> ui/ItemHeader
       pname ;; " " [:small " by " (:oname vendor)]
       ]
      [:> ui/ItemExtra {:class "product-tags"}
       [bc/c-categories product]
       (when (seq discounts)
         [bc/c-discount-tag discounts])]]
     [:div.community
      [:div.metric
       "Used by " orgs-using-count " "
       [:> ui/Icon {:name "group"}]]
      [:div.metric
       (let [median-prices (map :median-price agg-group-prod-price)]
         (if (seq median-prices)
           (str "$" ;; get the mean from all the member'd groups' medians
                (util/decimal-format (/ (apply + median-prices) (count median-prices)))
                " / year")
           "No pricing data."))]
      [:> ui/Rating {:rating mean
                     :maxRating 5
                     :size "large"
                     :disabled true}]]]))

(defn c-popular-stack
  [{:keys [id] :as group}]
  (let [products-showing-count 10
        group-ids& (rf/subscribe [:group-ids])
        agg-group-prod-rating& (rf/subscribe
                                [:gql/sub
                                 {:queries
                                  [[:agg-group-prod-rating {:group-id @group-ids&
                                                            ;; not the best way to determine popularity, basically just means:
                                                            ;; which product had the most ratings of the same star value
                                                            :_order_by {:count-stack-items :desc}
                                                            ;; at least 10 products fully represented (all rating choices including nil)
                                                            :_limit (* 6 products-showing-count)}
                                    [:group-id :product-id
                                     :count-stack-items :rating]]]}])]
    (fn []
      (if (= :loading @agg-group-prod-rating&)
        [cc/c-loader]
        (let [products @(rf/subscribe
                         [:gql/q
                          {:queries
                           [[:products {:id (->> @agg-group-prod-rating&
                                                 :agg-group-prod-rating
                                                 (map :product-id)
                                                 distinct
                                                 (take products-showing-count))}
                             [:id :pname :idstr :logo :score
                              [:vendor
                               [:id :oname :idstr :short-desc]]
                              [:categories {:ref-deleted nil}
                               [:id :idstr :cname]]

                              ;; TODO, this isn't being used because the above agg-group-prod-rating is being used instead...
                              [:agg-group-prod-rating {:group-id @group-ids&}
                               [:group-id :product-id
                                :count-stack-items :rating]]
                              
                              [:agg-group-prod-price {:group-id @group-ids&}
                               [:group-id :median-price]]
                              [:discounts {:id @group-ids&
                                           :ref-deleted nil}
                               [:group-discount-descr :gname]]]]]}])]
          [:div.popular-products
           [:h1 "Popular Products"]
           [:> ui/ItemGroup {:class "results"}
            (if (seq (:products products))
              (doall
               (for [product (:products products)]
                 ^{:key (:id product)}
                 [c-stack-item product (:agg-group-prod-rating @agg-group-prod-rating&)]))
              "No organizations have added products to their stack yet."
              ;; TODO spacing below this empty state?
              )]])
        )
      )))

(defn c-group
  [{:keys [gname] :as group}]
  [:> ui/Grid {:stackable true
               :style {:padding-bottom 35}} ; in case they are admin of multiple communities
   [:> ui/GridRow
    [:> ui/GridColumn {:computer 16 :mobile 16}
     [:h1 {:style {:text-align "center"}}
      gname " Community"]]]
   [:> ui/GridRow
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [c-orgs group]]
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [c-popular-stack group]]]])

(defn c-groups
  [groups]
  [:div
   (for [group groups]
     ^{:key (:id group)}
     [c-group group])])

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        group-ids& (rf/subscribe [:group-ids])
        groups& (rf/subscribe [:gql/sub
                               {:queries
                                [[:groups {:id @group-ids&
                                           :deleted nil}
                                  [:id :gname
                                   [:orgs
                                    [:id :idstr :oname
                                     [:memberships
                                      [:id
                                       [:user
                                        [:id :uname]]]]]]]]]}])]
    (fn []
      (if (= :loading @groups&)
        [cc/c-loader]
        [c-groups (:groups @groups&)]))))

