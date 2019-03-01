(ns vetd-app.pages.buyers.b-search
  (:require [vetd-app.flexer :as flx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(def last-query-id (atom 0))

(defn get-next-query-id []
  (swap! last-query-id inc))

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
     {:db (assoc db
                 :buyer-qid qid)
      :ws-send {:payload {:cmd :b/search
                          :return {:handler :b/ws-search-result-ids
                                   :qid qid}
                          :query q-str
                          :buyer-id (:org-id db)
                          :qid qid}}})))

(rf/reg-sub
 :b/search-result-ids
  (fn [db _]
    (:b/search-result-ids db)))

(rf/reg-event-fx
 :b/ws-search-result-ids
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   (def res1 results)
   #_ (println res1)
   (def ret1 return)
   (if (= (:buyer-qid db) (:qid return))
     {:db (assoc db
                 :b/search-result-ids
                 results)}
     {})))

(rf/reg-event-fx
 :start-round
 (fn [{:keys [db]} [_ etype eid]]
   (let [qid (get-next-query-id)]
     {:ws-send {:payload {:cmd :start-round
                          :return nil
                          :etype etype
                          :buyer-id (:org-id db)
                          :eid eid}}})))


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
  [{:keys [id pname short-desc preposals logo rounds categories]} org-name ]
  [flx/row #{:product-search-result}
   [#{:prod-logo} [:img {:src (str "https://s3.amazonaws.com/vetd-logos/" logo)}]]
   [#{:content} [flx/row #{:header}
                 [#{:product-name} pname]
                 " by "
                 [#{:org-name} org-name]
                 (str (mapv :cname categories))]
    [:div {:class :body}
     [:div {:class :product-short-desc} short-desc]
     (cond (not-empty rounds) (for [r rounds]
                                [c-round-search-result r])
           :else [rc/button
                  :on-click #(rf/dispatch [:start-round :product id])
                  :label "Start Round"])]]])

(defn c-vendor-search-results
  [v]
  [:div {:class :vendor-search-results}
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
      [rc/button :on-click #(rf/dispatch [:start-round :category id])
       :label "Start Round for Category"]
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
                   :placeholder "Search products and categories"]

                  [c-search-results]]])))
