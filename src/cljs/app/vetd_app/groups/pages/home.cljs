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

(rf/reg-event-fx
 :g/join-request
 (fn [{:keys [db]} [_ group-ids]]
   {:ws-send {:payload {:cmd :g/join-request
                        :return {:handler :g/join-request.return}
                        :group-ids group-ids
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Request to Join"
                      :props {:category "Community"}}}))

(rf/reg-event-fx
 :g/join-request.return
 (constantly
  {:toast {:type "success"
           :title "Request Sent"
           :message "We'll be in touch via email with next steps shortly."}}))

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
    (fn [{:keys [id idstr gname orgs] :as group}]
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
                                         (rf/dispatch [:g/nav-detail idstr])))}
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
      [:> ui/ItemHeader pname]
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
  [top-products]
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

(defn c-join-group-form
  [current-group-ids popup-open?&]
  (let [value& (r/atom [])
        options& (r/atom []) ; options from search results + current values
        search-query& (r/atom "")
        groups->options (fn [groups]
                          (for [{:keys [id gname]} groups]
                            {:key id
                             :text gname
                             :value id}))]
    (fn [current-group-ids popup-open?&]
      (let [groups& (rf/subscribe
                     [:gql/q
                      {:queries
                       [[:groups {:_where
                                  {:_and ;; while this trims search-query, the Dropdown's search filter doesn't...
                                   [{:gname {:_ilike (str "%" (s/trim @search-query&) "%")}}
                                    {:deleted {:_is_null true}}]}
                                  :_limit 100
                                  :_order_by {:gname :asc}}
                         [:id :gname]]]}])
            _ (when-not (= :loading @groups&)
                (let [options (->> @groups&
                                   :groups
                                   groups->options ; now we have options from gql sub
                                   ;; (this dumbly actually keeps everything, but that seems fine)
                                   (concat @options&) ; keep options for the current values
                                   distinct
                                   (remove (comp (partial contains? current-group-ids) :value)))]
                  (when-not (= @options& options)
                    (reset! options& options))))]
        [:> ui/Form {:as "div"
                     :class "popup-dropdown-form"}
         [:> ui/Dropdown {:loading (= :loading @groups&)
                          :options @options&
                          :placeholder "Search groups..."
                          :search true
                          :selection true
                          :multiple true
                          :selectOnBlur false
                          :selectOnNavigation true
                          :closeOnChange true
                          :allowAdditions true
                          :additionLabel "Hit 'Enter' to Write In "
                          :onAddItem (fn [_ this]
                                       (->> this
                                            .-value
                                            vector
                                            ui/as-dropdown-options
                                            (swap! options& concat)))
                          :onSearchChange (fn [_ this] (reset! search-query& (aget this "searchQuery")))
                          :onChange (fn [_ this] (reset! value& (.-value this)))}]
         [:> ui/Button
          {:color "teal"
           :disabled (empty? @value&)
           :on-click #(do (reset! popup-open?& false)
                          (rf/dispatch [:g/join-request (js->clj @value&)]))}
          "Request to Join"]]))))

(defn c-join-group-button
  [current-group-ids]
  (let [popup-open?& (r/atom false)]
    (fn [current-group-ids]
      [:> ui/Popup
       {:position "bottom left"
        :on "click"
        :open @popup-open?&
        :onOpen #(reset! popup-open?& true)
        :onClose #(reset! popup-open?& false)
        :hideOnScroll false
        :flowing true
        :content (r/as-element [c-join-group-form current-group-ids popup-open?&])
        :trigger (r/as-element
                  [:> ui/Button {:color "lightblue"
                                 :icon true
                                 :labelPosition "left"
                                 :fluid true}
                   "Join a Community"
                   [:> ui/Icon {:name "user plus"}]])}])))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        group-ids& (rf/subscribe [:group-ids])
        groups& (rf/subscribe [:gql/sub
                               {:queries
                                [[:groups {:id @group-ids&
                                           :deleted nil}
                                  [:id :idstr :gname
                                   [:orgs
                                    [:id]]
                                   ;; TODO put in separate gql and do single query for all group-ids
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
           [c-join-group-button (->> @groups& :groups (map :id))]]]
         [:div.inner-container
          [:> ui/Grid {:stackable true
                       :style {:padding-bottom 35}} ; in case they are admin of multiple communities
           [:> ui/GridRow
            [:> ui/GridColumn {:computer 9 :mobile 16
                               :style {:padding-right 7}}
             [c-feed {}]]
            [:> ui/GridColumn {:computer 7 :mobile 16}
             [c-popular-stack (->> (:groups @groups&)
                                   (map :top-products)
                                   flatten)]]]]]]))))
