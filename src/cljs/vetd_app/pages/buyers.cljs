(ns vetd-app.pages.buyers
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(def last-query-id (atom 0))

(defn get-next-query-id []
  (swap! last-query-id inc))

(rf/reg-event-fx
 :nav-buyers
 (fn [{:keys [db]} [_ query]]
   {:nav {:path (:org-id db)
          :query query}}))

(rf/reg-event-db
 :route-b-search
 (fn [db [_ query-params]]
   (assoc db
          :page :buyers
          :query-params query-params)))

(def dispatch-search-DB
  (goog.functions.debounce
   #(do (rf/dispatch [:search %]))
   250))

(rf/reg-event-fx
 :search
 (fn [{:keys [db ws]} [_ q-str q-type]]
   (let [qid (get-next-query-id)]
     {:db (assoc db
                 :buyer-qid qid)
      :ws-send {:payload {:cmd :search
                          :return {:handler :ws/search-result-ids
                                   :qid qid}
                          :query q-str
                          :buyer-id (:org-id db)
                          :qid qid}}})))

(rf/reg-sub
 :search-result-ids
  (fn [db _]
    (:search-result-ids db)))

(rf/reg-event-fx
 :ws/search-result-ids
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   (def res1 results)
   #_ (println res1)
   (def ret1 return)
   (if (= (:buyer-qid db) (:qid return))
     {:db (assoc db
                 :search-result-ids
                 results)}
     {})))

(rf/reg-event-fx
 :add-to-cart
 (fn [{:keys [db]} [_ prod-id]]
   (let [qid (get-next-query-id)]
     {:ws-send {:payload {:cmd :add-to-cart
                          :return nil
                          :buyer-id (:org-id db)
                          :prod-id prod-id}}})))


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
  [{:keys [id pname short-desc preposals logo rounds cart_items categories]} org-name ]
  [:div {:class :product-search-result}
   [:div {:class :header}
    [:div.org-logo
     [:div
      [:img {:src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]]]
    [:span.product-name pname] " by "
    [:span.org-name org-name]]
   [:div "Categories: " (str (mapv :cname categories))]
   [:div {:class :product-short-desc} short-desc]
   (cond (not-empty rounds) (for [r rounds]
                              [c-round-search-result r])
         (empty? cart_items) [rc/button
                              :on-click #(rf/dispatch [:start-round :product id])
                              :label "Start Round"]
         :else [:div.in-cart [:span "In Cart"]])
   #_   (if (not-empty preposals)
          (for [p preposals]
            [c-preposal-search-result p])
          [:div "REQUEST A PREPOSAL"])
   #_   (if (not-empty reqs)
          (for [r reqs]
            [c-preposal-req-search-result r])
          [:div])])

(defn c-vendor-search-results
  [v]
  [:div {:class :vendor-search-results}
   (for [p (:products v)]
     ^{:key (:id p)}
     [c-product-search-result p (:oname v)])])

(defn c-category-search-results
  [{:keys [cname id idstr]}]
  [:div {:class :category-search-result}
   [rc/button :on-click #(rf/dispatch [:start-round :product id])
    :label "Start Round"]
   [:span.category-name cname]])

(defn c-search-results []
  (let [org-id @(rf/subscribe [:org-id])
        {:keys [product-ids vendor-ids categories] :as ids} @(rf/subscribe [:search-result-ids])
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
                                      [:cart_items {:buyer-id org-id}
                                       [:id]]
                                      [:categories [:id :idstr :cname]]]]]]]}])
                [])]
    [:div {:class :search-results}
     [:div {:class :categories}
      (for [c categories]
        ^{:key (:id c)}
        [c-category-search-results c])]
     [:div {:class :orgs}
      (for [v (:orgs prods)]
        ^{:key (:id v)}
        [c-vendor-search-results v])]]))

(defn buyers-page []
  (let [ ;;search-type (r/atom :all)
        search-query (r/atom "")]
    (fn []
      [rc/v-box
       :style {:align-items :center}
       :children [[rc/input-text
                   :model search-query
                   :attr {:auto-focus true}
                   :width "50%"
                   :on-change #(do
                                 (dispatch-search-DB %)
                                 (reset! search-query %))
                   :change-on-blur? false
                   :placeholder "Search vendor data"]
                  #_[rc/horizontal-bar-tabs
                    :model search-type
                    :tabs [{:id :all
                            :label "All Vendor Data"}
                           {:id :preposals
                            :label "Preposals"}
                           {:id :profiles
                            :label "Profiles"}
                           {:id :contacts
                            :label "Sales Contacts"}]
                    :on-change #(do
                                  (dispatch-search-DB @search-query %)
                                  (reset! search-type %))]
                  [c-search-results]]])))
