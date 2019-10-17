(ns vetd-app.groups.pages.home
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [vetd-app.common.fx :as cfx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(def init-db
  {:filter {:groups #{}}})

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
    :dispatch [:groups.filter.reset]}))

(rf/reg-event-fx
 :groups.filter.add ;; group arg is "filter group" not community
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:groups :filter group] conj value)}))

(rf/reg-event-fx
 :groups.filter.remove
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:groups :filter group] disj value)}))

(rf/reg-event-fx
 :groups.filter.reset
 (fn [{:keys [db]}]
   (let [{:keys [memberships active-memb-id]} db]
     {:db (assoc-in db
                    [:groups :filter :groups]
                    (some->> memberships
                             (filter #(-> % :id (= active-memb-id)))
                             first
                             :org
                             :groups
                             (map :id)
                             set))})))

;;;; Subscriptions
(rf/reg-sub
 :groups
 (fn [{:keys [groups]}] groups))

(rf/reg-sub
 :groups.filter
 :<- [:groups]
 (fn [{:keys [filter]}]
   filter))

;;;; Components
(defn c-group-filter
  [group]
  (let [filter& (rf/subscribe [:groups.filter])]
    (fn [{:keys [id gname orgs] :as group}]
      (let [num-orgs (count orgs)]
        [:> ui/Checkbox
         {:checked (-> @filter& :groups (contains? id) boolean)
          :on-click (fn [_ this]
                      (rf/dispatch [(if (.-checked this)
                                      :groups.filter.add
                                      :groups.filter.remove)
                                    :groups
                                    id]))
          :label {:children
                  (r/as-element
                   [:div {:style {:position "relative"
                                  :top -1
                                  :left 4}}
                    [:h4 {:style {:margin "0 0 5px 0"
                                  :padding "0 0 0 0"}}
                     gname]
                    [:a {:on-click (fn [e]
                                     (do (.stopPropagation e)
                                         (rf/dispatch [:c/nav-detail id])))}
                     [:> ui/Icon {:name "group"}]
                     (str " " num-orgs " organization" (when-not (= num-orgs 1) "s") " ")]])}}]))))


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
       [bc/c-categories product]]]
     [:div.community {:on-click #(rf/dispatch
                                  [:dispatch-stash.push :product-detail-loaded
                                   [:do-fx
                                    {:dispatch-later
                                     [{:ms 200
                                       :dispatch [:scroll-to :product/community]}]}]])}
      [:div.metric 
       "Used by " orgs-using-count " "
       [:> ui/Popup
        {:position "bottom right"
         :offset 10
         :content "Click to see organizations that are using this product."
         :trigger (r/as-element [:> ui/Icon {:name "group"}])}]]
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

(def ftype->icon
  {:round-started "wpforms"
   :round-winner-declared "trophy"
   ;; blah blah rated product 4 stars
   :stack-update-rating "star"
   ;; this is a disaster, v1 just show everything piecemeal
   :create-stack-item "grid layout"
   :preposal-request "wpforms"
   :buy-request "wpforms"
   ;; these are the same event when user-facing
   :complete-vendor-profile-request "wpforms"
   :complete-product-profile-request "wpforms"})

(defn c-feed-event
  [{:keys [id ftype text]}]
  [:> ui/FeedEvent
   [:> ui/FeedLabel
    [:> ui/Icon {:name (ftype->icon ftype)}]]
   [:> ui/FeedContent
    [:> ui/FeedSummary text]
    [:> ui/FeedDate "2 days ago - Super VC"]]])

(defn c-feed
  [{:keys [id gname orgs] :as group}]
  (let [orgs-sorted (sort-by (comp count :stack-items) > orgs)]
    [bc/c-profile-segment {:title [:<>
                                   [:> ui/Icon {:name "feed"}]
                                   "Recent Activity"]}
     [:> ui/Feed
      (for [event [{:id 3
                    :ftype :round-started
                    :text "Blah Blah started a VetdRound called \"Wowza Products\""}
                   {:id 4
                    :ftype :round-winner-declared
                    :text "Blah Blah declared a winner in a  VetdRound called \"Wowza Products\""}
                   {:id 5
                    :ftype :create-stack-item
                    :text "Super Cool added a stack item Hey Product"}]]
        ^{:key (:id event)}
        [c-feed-event event])]]))

(defn c-group
  [{:keys [gname] :as group}]
  [:> ui/Grid {:stackable true
               :style {:padding-bottom 35}} ; in case they are admin of multiple communities
   [:> ui/GridRow
    [:> ui/GridColumn {:computer 9 :mobile 16
                       :style {:padding-right 7}}
     [c-feed group]]
    [:> ui/GridColumn {:computer 7 :mobile 16}
     #_[c-orgs group]
     [c-popular-stack group]]]])

(defn c-groups
  [groups]
  [:div
   (for [group groups]
     ^{:key (:id group)}
     [c-group group])])

(defn c-join-group-button
  []
  (let [popup-open? (r/atom false)]
    (fn []
      [:> ui/Popup
       {:position "right center"
        :on "click"
        :open @popup-open?
        :onOpen #(reset! popup-open? true)
        :onClose #(reset! popup-open? false)
        :hideOnScroll false
        :flowing true
        :content "Choose a Community"
        :trigger (r/as-element
                  [:> ui/Button {:color "lightblue"
                                 :icon true
                                 :labelPosition "left"
                                 :fluid true}
                   "Join a Community"
                   [:> ui/Icon {:name "user plus"}]])}])))

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
                                      [:id]]]]
                                   ;; TODO all groups mixed together
                                   [:top-products {:_order_by {:count-stack-items :desc}
                                                   :_limit 10}
                                    [:group-id :product-id :count-stack-items]]]]]}])]
    (fn []
      (if (= :loading @groups&)
        [cc/c-loader]
        [:div.container-with-sidebar
         [:div.sidebar
          [:> ui/Segment {:class "detail-container profile group-filter"}
           [:h1.title  "Communities"]
           (for [group (:groups @groups&)]
             ^{:key (:id group)}
             [c-group-filter group])]
          [:> ui/Segment {:class "detail-container profile"}
           [c-join-group-button]]]
         [:div.inner-container
          [c-groups (:groups @groups&)]]]))))
