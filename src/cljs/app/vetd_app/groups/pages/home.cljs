(ns vetd-app.groups.pages.home
  (:require [vetd-app.ui :as ui]
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
                                                        (println id)
                                                        (if (= id @org-id&)
                                                          (rf/dispatch [:b/nav-stack])
                                                          (rf/dispatch [:b/nav-stack-detail idstr])))
                                            :as "a"
                                            :color "lightblue"
                                            :style {:float "right"
                                                    :margin-top 5}}
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
  [bc/c-profile-segment {:title "Organizations"}
   (for [org orgs]
     ^{:key (:id org)}
     [c-org org group])])

(defn c-stack-item
  [{:keys [price-amount price-period renewal-date] :as stack-item}]
  (fn [{:keys [id rating price-amount price-period
               renewal-date renewal-reminder status
               product] :as stack-item}]
    (let [{product-id :id
           product-idstr :idstr
           :keys [pname short-desc logo vendor]} product]
      [:> ui/Item {:on-click #(rf/dispatch [:b/nav-product-detail product-idstr])}
       [bc/c-product-logo logo]
       [:> ui/ItemContent
        [:> ui/ItemHeader
         pname " " [:small " by " (:oname vendor)]]
        [:<>
         [:> ui/ItemExtra {:style {:color "rgba(0, 0, 0, 0.85)"
                                   :font-size 14
                                   :line-height "14px"}}
          [:> ui/Grid {:class "stack-item-grid"}
           [:> ui/GridRow {:class "field-row"}
            [:> ui/GridColumn {:width 13}
             [bc/c-categories product]]
            [:> ui/GridColumn {:width 3
                               :style {:text-align "right"}}
             (when rating
               [:<>
                [:div {:style {:margin-bottom 4}}
                 "Their Rating"]
                [:> ui/Rating {:rating rating
                               :maxRating 5
                               :size "huge"
                               :disabled true}]])]]]]]]])))

(defn c-popular-stack
  [{:keys [id] :as group}]
  (let [org-stack& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:orgs {
                                            :_order_by {:created :desc}
                                            
                                            :_limit 1}
                                     [:id :oname
                                      [:stack-items {:_order_by {:created :desc}
                                                     :deleted nil

                                                     :_limit 10

                                                     }
                                       [:id :idstr :status
                                        :price-amount :price-period :rating
                                        :renewal-date :renewal-reminder
                                        [:product
                                         [:id :pname :idstr :logo
                                          [:vendor
                                           [:id :oname :idstr :short-desc]]
                                          [:categories {:ref-deleted nil}
                                           [:id :idstr :cname]]]]]]]]]}])]
    (fn []
      (if (= :loading @org-stack&)
        [cc/c-loader]
        [bc/c-profile-segment {:title "Popular Products"}
         (let [{:keys [oname stack-items] :as org} (first (:orgs @org-stack&))]
           [:> ui/ItemGroup {:class "results"}
            (let [current-stack-items (filter (comp (partial = "current") :status) stack-items)]
              (if (seq current-stack-items)
                (for [stack-item current-stack-items]
                  ^{:key (:id stack-item)}
                  [c-stack-item stack-item])
                "No organizations have added products to their stack yet."
                ;; TODO spacing below this empty state?
                ))])]
        )
      )))

(defn c-group
  [{:keys [gname] :as group}]
  [:> ui/Grid {:stackable true
               :style {:padding-bottom 35}} ; in case they are admin of multiple communities
   [:> ui/GridRow
    [:> ui/GridColumn {:computer 16 :mobile 16}
     [:h1 {:style {:text-align "center"}}
      gname]]]
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

