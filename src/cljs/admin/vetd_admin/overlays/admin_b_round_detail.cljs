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

(rf/reg-event-fx
 :a/delete-any
 (fn [{:keys [db]} [_ {:keys [id]}]]
   {:ws-send {:payload {:cmd :a/delete-any
                        :id id}}}))

(defn c-delete-round [round-id]
  [:> ui/Button {:color "teal"
                 :on-click #(rf/dispatch [:a/delete-any {:id round-id}])}
   "Delete VetdRound"])

(defn prompt->topic-option
  [{:keys [id prompt]}]
  {:key (str prompt)
   :text (str prompt) 
   :value id})

(rf/reg-event-fx
 :a/remove-round-topic
 (fn [{:keys [db]} [_ {:keys [round-id prompt-id]}]]
   {:ws-send {:payload {:cmd :a/round.remove-topic
                        :round-id round-id
                        :prompt-id prompt-id}}}))

(defn c-remove-topic [round-id round-idstr]
  (let [selected& (r/atom nil)
        rounds& (rf/subscribe [:gql/sub
                               {:queries
                                [[:rounds {:idstr round-idstr
                                           :deleted nil}
                                  [:id :idstr :created :status :title
                                   ;; requirements form template
                                   [:req-form-template
                                    [:id
                                     [:prompts {:ref-deleted nil
                                                :_order_by {:sort :asc}}
                                      [:id :idstr :prompt :term :descr :sort]]]]]]]}])]
    (fn []
      (let [topics (->> @rounds& :rounds first :req-form-template :prompts (mapv prompt->topic-option))]
        [:<>
         [:> ui/Dropdown {:value @selected&
                          :onChange #(reset! selected& (.-value %2))
                          :selection true
                          :options topics}]
         [:> ui/Button {:color "teal"
                        :on-click #(rf/dispatch [:a/remove-round-topic {:round-id round-id
                                                                        :prompt-id @selected&}])}
          "Remove Topic"]]))))


(defn c-product-selector [round-id round-with-products&]
  (let [search-term& (r/atom "")
        selected& (r/atom [])]
    (fn [round-id round-with-products&]
      (let [round-with-products @round-with-products&
            search-term @search-term&
            product-options& (when (> (count search-term) 3)
                               (rf/subscribe [:gql/q
                                              {:queries
                                               [[:products {:_where {:pname {:_ilike (str search-term "%")}}
                                                            :deleted nil}
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
                                            (let [sq (aget data "searchQuery")]
                                              (util/call-debounce-by-id :a/product-dropdown-search-term-change
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
                               {:admin? true
                                :queries
                                [[:rounds {:idstr @round-idstr&}
                                  [:id
                                   [:products {:deleted nil
                                               :ref-deleted nil}
                                    [:id :pname
                                     [:vendor
                                      [:id :oname]]]]]]]}])]
    (fn []
      (let [round-id (util/base31->num @round-idstr&)]
        [:div
         [:div {:style {:display "flex"}}
          [c-product-selector round-id rounds&]]
         [c-delete-round round-id]
         [:div {:style {:margin-top "20px"}}
          [c-remove-topic round-id @round-idstr&]]]))))
