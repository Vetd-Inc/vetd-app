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
   {:db (assoc db :page :g/home)}))

;;;; Components
(defn c-org
  [org group]
  (let [org-id& (rf/subscribe [:org-id])]
    (fn [{:keys [id idstr oname memberships stack-items] :as org}
         {:keys [gname] :as group}]
      (let [num-members (count memberships)
            num-stack-items (count stack-items)]
        [:> ui/GridColumn
         [:> ui/Button {:on-click (fn []
                                    (if (= id @org-id&)
                                      (rf/dispatch [:b/nav-stack])
                                      (rf/dispatch [:b/nav-stack-detail idstr])))
                        :as "a"
                        :size "small"
                        :color "white"
                        :fluid true
                        :style {:padding "7px"
                                :text-align "left"}}
          [:h4.blue {:style {:margin "0 0 5px 0"
                             :padding "0 0 0 0"}}
           oname]
          [:div {:style {:font-weight 300}}
           (str num-members " member" (when-not (= num-members 1) "s") " ")
           ]
          (when (pos? num-stack-items)
            [:div {:style {:margin "7px 0 0 0"}}
             [:> ui/Icon {:name "grid layout"}]
             (str " " num-stack-items " Stack Item" (when-not (= num-stack-items 1) "s"))])]
         ;; (when (pos? num-members)
         ;;   [:> ui/Popup
         ;;    {:position "bottom center"
         ;;     :wide "very"
         ;;     :content (let [max-members-show 15]
         ;;                (str (s/join ", " (->> memberships
         ;;                                       (map (comp :uname :user))
         ;;                                       (take max-members-show)))
         ;;                     (when (> num-members max-members-show)
         ;;                       (str " and " (- num-members max-members-show) " more."))))
         ;;     :trigger (r/as-element
         ;;               )}])
         ]))))

(defn c-orgs
  [{:keys [id gname orgs] :as group}]
  (let [orgs-sorted (sort-by (comp count :stack-items) > orgs)]
    [:> ui/Segment {:class "detail-container profile"}
     [:h1.title [:> ui/Icon {:name "group"}] " " gname " Community"]
     [:> ui/Grid {:class "orgs-grid"
                  :stackable true
                  :columns "equal"
                  :style {:margin-top 0}}
      (let [org-cmps (for [org orgs-sorted]
                       ^{:key (:id org)}
                       [c-org org group])]
        (for [cmp-row (partition 3 org-cmps)]
          ^{:key (gensym "row-")}
          [:> ui/GridRow cmp-row]))]]))

(defn c-stack-item
  [product top-products]
  (let [{product-id :id
         product-idstr :idstr
         :keys [pname short-desc logo vendor discounts
                agg-group-prod-rating agg-group-prod-price]} product
        orgs-using-count (->> top-products
                              (filter #(= (:product-id %) product-id))
                              first
                              :count-stack-items)
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
  [{:keys [id top-products] :as group}]
  (let [group-ids& (rf/subscribe [:group-ids])]
    (fn []
      (let [top-products-ids (map :product-id top-products)
            products-unordered @(rf/subscribe
                                 [:gql/q
                                  {:queries
                                   [[:products {:id top-products-ids}
                                     [:id :pname :idstr :logo :score
                                      [:vendor
                                       [:id :oname :idstr :short-desc]]
                                      [:categories {:ref-deleted nil
                                                    :_limit 1}
                                       [:id :idstr :cname]]
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
          (when-not (= :loading products-unordered)
            (let [products (->> products-unordered
                                :products
                                (sort-by #(.indexOf top-products-ids (:id %))))]
              (if (seq products)
                (doall
                 (for [product products]
                   ^{:key (:id product)}
                   [c-stack-item product top-products]))
                "No organizations have added products to their stack yet.")))]]))))

(defn c-feed-event
  [{:keys [id ftype text]}]
  [:> ui/FeedEvent
   {:icon "wpforms"
    :date "Now"
    :summary text}])

(defn c-feed
  [{:keys [id gname orgs] :as group}]
  (let [orgs-sorted (sort-by (comp count :stack-items) > orgs)]
    [bc/c-profile-segment {:title [:<>
                                   [:> ui/Icon {:name "feed"}]
                                   "Recent Activity"]}
     [:> ui/Feed
      (for [event [{:id 3
                    :ftype "round-add"
                    :text "Blah Blah started a VetdRound called \"Wowza Products\""}
                   {:id 4
                    :ftype "round-add"
                    :text "Blah Blah started a VetdRound called \"Wowza Products\""}
                   {:id 5
                    :ftype "round-add"
                    :text "Blah Blah started a VetdRound called \"Wowza Products\""}]]
        ^{:key (:id event)}
        [c-feed-event event])]]))

(defn c-group
  [{:keys [gname] :as group}]
  [:> ui/Grid {:stackable true
               :style {:padding-bottom 35}} ; in case they are admin of multiple communities
   [:> ui/GridRow
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [c-feed group]]
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [c-orgs group]
     [c-popular-stack group]]]])

(defn c-groups
  [groups]
  [:div
   (for [group groups]
     ^{:key (:id group)}
     [c-group group])])

;; currently unused
(defn c-explainer []
  [:> ui/Segment {:placeholder true
                  :class "how-vetd-works"}
   [:h2 "How Vetd Works . . ."]
   [cc/c-grid {:columns "equal"
               :stackable true
               :style {:margin-top 4}}
    [[[:<>
       [:h3 "Your Stack"]
       "Add products to your stack to keep track of renewals, get recommendations, and share with your community."]]
     [[:<>
       [:h3 "Browse Products"]
       "Search for products or product categories to find products that meet your needs."]]
     [[:<>
       [:h3 "VetdRounds"]
       "Compare similar products side-by-side based on your unique requirements, and make an informed buying decision in a fraction of the time."]]]]])

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
                                        [:id :uname]]]]
                                     [:stack-items
                                      [:id :idstr :status]]]]
                                   [:top-products {:_order_by {:count-stack-items :desc}
                                                   :_limit 10}
                                    [:group-id :product-id :count-stack-items]]]]]}])]
    (fn []
      (if (= :loading @groups&)
        [cc/c-loader]
        [c-groups (:groups @groups&)]))))

