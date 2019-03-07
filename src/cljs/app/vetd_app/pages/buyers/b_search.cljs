(ns vetd-app.pages.buyers.b-search
  (:require [vetd-app.flexer :as flx]
            [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(def last-query-id (atom 0))

(defn get-next-query-id []
  (swap! last-query-id inc))

;; Events
(rf/reg-event-fx
 :b/nav-search
 (fn [_ _]
   {:nav {:path "/b/search/"}}))

(rf/reg-event-db
 :b/route-search
 (fn [db [_ query-params]]
   (assoc db
          :page :b/search
          :query-params query-params)))

(def dispatch-search-DB
  (goog.functions.debounce
   #(do (rf/dispatch [:b/search %]))
   250))

(rf/reg-event-fx
 :b/search
 (fn [{:keys [db ws]} [_ q-str q-type]]
   (let [qid (get-next-query-id)]
     {:db (assoc db :buyer-qid qid)
      :ws-send {:payload {:cmd :b/search
                          :return {:handler :b/ws-search-result-ids
                                   :qid qid}
                          :query q-str
                          :buyer-id (-> db :memberships first :org-id)
                          :qid qid}}})))

(rf/reg-event-fx
 :b/ws-search-result-ids
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   (def res1 results)
   #_ (println res1)
   (def ret1 return)
   (if (= (:buyer-qid db) (:qid return))
     {:db (assoc db :b/search-result-ids results)}
     {})))

(rf/reg-event-fx
 :start-round
 (fn [{:keys [db]} [_ etype eid]]
   (let [qid (get-next-query-id)]
     {:ws-send {:payload {:cmd :b/start-round
                          :return nil
                          :etype etype
                          :eid eid
                          :buyer-id (-> db :memberships first :org-id)}}})))

;; Subscriptions
(rf/reg-sub
 :b/search-result-ids
 (fn [db _]
   (:b/search-result-ids db)))


;; Components

;; TODO link to request preposal <==================
(defn rndr-preposal
  [{:keys [vendor-name pitch created]}]
  [:div {:style {:border "solid 1px #AAA"}}
   "Preposal"
   [:div pitch]
   [:div (str created)]])

(defn c-round-search-result
  [{:keys [id created status]}]
  [:div "Active Round " (str [status created])])

(defn c-preposal-search-result
  [{:keys [created]}]
  [:div "Preposal " (str created)])

(defn c-preposal-req-search-result
  [{:keys [created]}]
  [:div "Preposal Requested " (str created)])

(defn c-product-search-result
  [{:keys [id pname short-desc preposals logo rounds categories]} org-name]

  [:> ui/Item {:onClick #(println "go to this product")}
   [:> ui/ItemImage {:class "product-logo"
                     :src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]
   [:> ui/ItemContent
    [:> ui/ItemHeader
     pname " " [:small " by " org-name]]
    [:> ui/ItemDescription short-desc]
    [:> ui/ItemExtra
     (when (empty? rounds)
       [:> ui/Button {:onClick #(rf/dispatch [:start-round :product id])
                      :icon true
                      :labelPosition "right"
                      :floated "right"}
        "Start VetdRound"
        [:> ui/Icon {:name "right arrow"}]])
     (for [c categories]
       ^{:key (:id c)}
       [:> ui/Label
        {:as "a"
         :class "category-tag"
         ;; :onClick #(println "category search: " (:id c))
         }
        (:cname c)])]]
   (when (not-empty rounds)
     [:> ui/Label {:color "red"
                   :attached "bottom right"}
      "VetdRound In Progress"])])

(defn c-vendor-search-results
  [v]
  [:> ui/ItemGroup {:class "results"}
   (for [p (:products v)]
     ^{:key (:id p)}
     [c-product-search-result p (:oname v)])])

(defn c-category-search-results
  [{:keys [cname id idstr rounds] :as cat}]
  [:div {:class :category-search-result}
   [:div.category-name cname]
   [:div {:style {:flex-grow 1
                  :display :flex
                  :align-items :center
                  :justify-content :flex-end}}
    (if (empty? rounds)
      [:> ui/Button {:on-click #(rf/dispatch [:start-round :category id])
                     :icon true
                     :labelPosition "right"}
       "Start VetdRound for Category"
       [:> ui/Icon {:name "right arrow"}]]
      "[In active round]")]])

(defn c-search-results []
  (let [org-id @(rf/subscribe [:org-id])
        {:keys [product-ids vendor-ids category-ids] :as ids} @(rf/subscribe [:b/search-result-ids])
        prods (if (not-empty (concat product-ids vendor-ids))
                @(rf/subscribe [:gql/sub
                                {:queries
                                 [[:orgs {:id vendor-ids}
                                   [:id :oname :idstr :short-desc
                                    [:products {:id product-ids}
                                     [:id :pname :idstr :short-desc :logo
                                      [:rounds {:buyer-id org-id
                                                :status "active"}
                                       [:id :created :status]]
                                      [:categories [:id :idstr :cname]]]]]]]}])
                [])
        categories (if (not-empty category-ids)
                     @(rf/subscribe [:gql/sub
                                     {:queries
                                      [[:categories {:id category-ids}
                                        [:id :idstr :cname
                                         [:rounds {:buyer-id org-id
                                                   :status "active"}
                                          [:id :created :status]]]]]}])
                     [])]
    [:div {:class :search-results}
     [:div {:class :categories}
      (for [c (:categories categories)]
        ^{:key (:id c)}
        [c-category-search-results c])]
     (when (and (-> categories :categories not-empty)
                (-> prods :orgs not-empty))
       [:div {:style {:height "30px"
                      :border-top "solid 1px #444"}}])
     [:div {:class :orgs}
      (for [v (:orgs prods)]
        ^{:key (:id v)}
        [c-vendor-search-results v])]]))

(defn c-page []
  (let [search-query (r/atom "")]
    (fn []
      [:> ui/Grid
       [:> ui/GridRow {:columns 3}
        [:> ui/GridColumn {:width 4}]
        [:> ui/GridColumn {:width 8}
         [:> ui/Input {:class "product-search"
                       :value @search-query
                       :size "big"
                       :icon "search"
                       :autoFocus true
                       :onChange (fn [_ this]
                                   (dispatch-search-DB (.-value this))
                                   (reset! search-query (.-value this)))
                       :placeholder "Search products & categories..."}]]
        [:> ui/GridColumn {:width 4}]]
       [:> ui/GridRow {:columns 3}
        [:> ui/GridColumn {:width 2}]
        [:> ui/GridColumn {:width 12}
         [c-search-results]]
        [:> ui/GridColumn {:width 2}]]])))
