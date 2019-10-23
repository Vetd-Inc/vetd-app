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
  (let [group-ids& (rf/subscribe [:group-ids])
        filter& (rf/subscribe [:groups.filter])]
    (fn [top-products]
      (let [top-products-ids (map :product-id top-products)
            selected-group-ids (:groups @filter&)
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
                                      [:agg-group-prod-rating {:group-id selected-group-ids}
                                       [:group-id :product-id
                                        :count-stack-items :rating]]
                                      [:agg-group-prod-price {:group-id selected-group-ids}
                                       [:group-id :median-price]]
                                      [:discounts {:id selected-group-ids
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
  {:round-started "vetd"
   :round-winner-declared "trophy"
   :stack-update-rating "star"
   :stack-add-items "grid layout"
   :preposal-request "clipboard outline"
   :buy-request "cart"
   :complete-vendor-profile-request "wpforms"
   :complete-product-profile-request "wpforms"})

(defn event-data->message
  [ftype data]
  (println data (type data))
  (case ftype
    :round-started (let [{:keys [buyer-org-name title]} data]
                     [:span buyer-org-name " started a VetdRound called "
                      [:em title]])
    :round-winner-declared (let [{:keys [buyer-org-name product-name]} data]
                             [:span buyer-org-name " chose " [:em product-name]
                              " as the winner of their VetdRound"])
    :stack-update-rating (let [{:keys [buyer-org-name product-name rating]} data]
                           [:span buyer-org-name " rated "
                            [:em product-name] " with " rating " star" (when-not (= rating 1) "s")])
    :stack-add-items (let [{:keys [buyer-org-name product-name]} data]
                       [:span buyer-org-name " added " [:em product-name] " to their Stack"])
    :preposal-request (let [{:keys [buyer-org-name product-name]} data]
                        [:span buyer-org-name " requested an estimate for " [:em product-name]])
    :buy-request (let [{:keys [buyer-org-name product-name]} data]
                   [:span buyer-org-name " decided to buy " [:em product-name]])
    :complete-vendor-profile-request (let [{:keys [buyer-org-name vendor-name]} data]
                                       [:span buyer-org-name " requested the complete profile of "
                                        [:em vendor-name]])
    :complete-product-profile-request (let [{:keys [buyer-org-name product-name]} data]
                                        [:span buyer-org-name " requested the complete profile of "
                                         [:em product-name]])
    "Unknown event."))

(defn event-data->click-event
  [ftype data]
  (case ftype
    :round-started (let [{:keys [round-id]} data] [:b/nav-round-detail (util/base31->str round-id)])
    :round-winner-declared (let [{:keys [round-id]} data] [:b/nav-round-detail (util/base31->str round-id)])
    :stack-update-rating (let [{:keys [product-id]} data]
                           [:b/nav-product-detail (util/base31->str product-id)])
    :stack-add-items (let [{:keys [buyer-org-id]} data]
                       (if (= buyer-org-id @(rf/subscribe [:org-id]))
                         [:b/nav-stack]
                         [:b/nav-stack-detail (util/base31->str buyer-org-id)]))
    :preposal-request (let [{:keys [product-id]} data]
                        [:b/nav-product-detail (util/base31->str product-id)])
    :buy-request (let [{:keys [product-id]} data]
                   [:b/nav-product-detail (util/base31->str product-id)])
    :complete-vendor-profile-request (let [{:keys [vendor-name]} data]
                                       ;; TODO this is a questionable stopgap till we have vendor pages
                                       [:b/nav-search vendor-name])
    :complete-product-profile-request (let [{:keys [product-id]} data]
                                        [:b/nav-product-detail (util/base31->str product-id)])
    "Unknown event."))

(defn c-feed-event
  [{:keys [id ftype created data]} group-name]
  [:> ui/FeedEvent {:on-click #(rf/dispatch (event-data->click-event (keyword ftype) data))}
   [:> ui/FeedLabel
    [:> ui/Icon {:name (ftype->icon (keyword ftype))}]]
   [:> ui/FeedContent
    [:> ui/FeedSummary (event-data->message (keyword ftype) data)]
    [:> ui/FeedDate
     (str (util/relative-datetime (.getTime (js/Date. created))) " - " group-name)]]])

(defn c-feed
  [groups]
  (let [org-ids (->> groups (map :orgs) flatten (map :id) distinct)
        org-id->group-name (->> (for [{:keys [gname orgs]} groups]
                                  (map (fn [org] [(:id org) gname]) orgs))
                                (apply concat)
                                (into {}))
        feed-events& (rf/subscribe [:gql/sub
                                    {:queries
                                     [[:feed-events {:org-id org-ids
                                                     :_order_by {:created :desc}
                                                     :_limit 30
                                                     :deleted nil}
                                       [:id :created :ftype :data]]]}])]
    (if (= :loading @feed-events&)
      [cc/c-loader]
      (let [feed-events (:feed-events @feed-events&)]
        [bc/c-profile-segment {:title [:<> [:> ui/Icon {:name "feed"}] "Recent Activity"]}
         (if (seq feed-events)
           [:> ui/Feed
            (for [event feed-events]
              ^{:key (:id event)}
              [c-feed-event event (org-id->group-name (-> event :data :buyer-org-id))])]
           [:p {:style {:padding-bottom 15}}
            "No recent activity."])]))))

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
                                   ;; TODO put in separate gql and do single query for all selected-group-ids
                                   [:top-products {:_order_by {:count-stack-items :desc}
                                                   :_limit 10}
                                    [:group-id :product-id :count-stack-items]]]]]}])
        filter& (rf/subscribe [:groups.filter])]
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
         (let [selected-groups (->> (:groups @groups&)
                                    (filter (comp (:groups @filter&) :id)))]
           [:div.inner-container
            [:> ui/Grid {:stackable true
                         :style {:padding-bottom 35}} ; in case they are admin of multiple communities
             [:> ui/GridRow
              [:> ui/GridColumn {:computer 9 :mobile 16
                                 :style {:padding-right 7}}
               [c-feed selected-groups]]
              [:> ui/GridColumn {:computer 7 :mobile 16}
               [c-popular-stack (->> selected-groups
                                     (map :top-products)
                                     ;; needs distinct ?
                                     flatten)]]]]])]))))
