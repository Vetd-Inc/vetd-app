(ns vetd-admin.overlays.admin-b-round-detail
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]   
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(def last-query-id (atom 0))

(defn get-next-query-id []
  (swap! last-query-id inc))

(rf/reg-sub
 :a/round-detail-products
 (fn [db _]
   (:a/round-detail-products db)))

(defn ->dropdown-item
  [{:keys [id pname vendor]}]
  {:text (str pname " / " (:oname vendor))
   :key id   
   :value id})

(rf/reg-event-fx
 :a/set-round-products
 (fn [{:keys [db]} [_ {:keys [round-id product-ids]}]]
   {:ws-send {:payload {:cmd :a/set-round-products
                        :round-id round-id
                        :product-ids product-ids}}}))

(defn c-product-selector [round-id round-with-products&]
  (let [search-term& (r/atom "")
        selected& (r/atom [])]
    (fn [round-id round-with-products&]
      (let [round-with-products @round-with-products&
            search-term @search-term&
            product-options& (when (> (count search-term) 3)
                               (rf/subscribe [:gql/q
                                              {:queries
                                               [[:products {:_where {:pname {:_ilike (str search-term "%")}}}
                                                 [:id :pname
                                                  [:vendor
                                                   [:id :oname]]]]]}]))
            _ (when (empty? @selected&)
                (reset! selected&
                        (or (some->> round-with-products :rounds first :products not-empty (mapv ->dropdown-item))
                            [])))
            product-options (distinct
                             (into @selected&
                                   (or (some->> product-options& deref :products not-empty (mapv ->dropdown-item))
                                       [])))]
        [:<>
         [:> ui/Dropdown {:value (mapv :value @selected&)
                          :options product-options
                          :onChange (fn [_ this]
                                      (reset! selected&
                                              (filterv (comp (set (.-value this))
                                                             :value)
                                                       product-options)))
                          :onSearchChange (fn [_ data]
                                            (let [sq (.-searchQuery data)]
                                              (when (> (count sq) 3)
                                                (reset! search-term& sq))
                                              #_(util/call-debounce-by-id :a/product-dropdown-search-term-change
                                                                          500
                                                                          #(reset! search-term& sq))))
                          :placeholder "Type Product/Vendor Name..."
                          :selection true
                          :multiple true
                          :search true
                          :fluid true
                          :style {:flex 1
                                  :margin-right 5}}]
         [:> ui/Button {:color "teal"
                        :on-click #(rf/dispatch [:a/set-round-products {:round-id round-id
                                                                        :product-ids (mapv :value @selected&)}])}
          "Update VetdRound with Products"]]))))

(defn c-overlay []
  (let [round-idstr& (rf/subscribe [:round-idstr])
        rounds& (rf/subscribe [:gql/sub
                               {:queries
                                [[:rounds {:idstr @round-idstr&}
                                  [:id
                                   [:products {:deleted nil
                                               :ref-deleted nil}
                                    [:id :pname
                                     [:vendor
                                      [:id :oname]]]]]]]}])]
    (fn []
      [:div {:style {:display "flex"}}
       [c-product-selector (util/base31->num @round-idstr&) rounds&]])))
