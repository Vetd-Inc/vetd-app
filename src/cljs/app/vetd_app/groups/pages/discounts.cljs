(ns vetd-app.groups.pages.discounts
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))


(rf/reg-event-fx
 :g/nav-discounts
 (constantly
  {:nav {:path "/c/discounts"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Groups Discounts"}}}))

(rf/reg-event-fx
 :g/route-discounts
 (fn [{:keys [db]}]
   {:db (assoc db :page :g/discounts)
    :analytics/page {:name "Groups Discounts"}}))

(defn c-org
  [{:keys [id idstr oname memberships] :as org}]
  (let [num-members (count memberships)]
    [cc/c-field {:label oname
                 :value [:<> (str num-members " member" (when (> num-members 1) "s") " ")
                         [:> ui/Popup
                          {:position "bottom left"
                           :content (s/join ", " (map (comp :uname :user) memberships))
                           :trigger (r/as-element
                                     [:> ui/Icon {:name "question circle"}])}]
                         ]
                 
                 }]))

(defn c-discount
  [{:keys [id idstr pname group-discount-descr vendor] :as discount}]
  [cc/c-field {:label [:<>
                       [:a.name {:on-click #(rf/dispatch [:b/nav-product-detail idstr])}
                        pname]
                       [:br]
                       [:small (:oname vendor)]]
               :value group-discount-descr}])

(defn c-group
  [{:keys [id gname orgs discounts] :as group}]
  [:> ui/Grid {:stackable true}
   [:> ui/GridRow
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [bc/c-profile-segment {:title (str gname " - Organizations")}
      (for [org orgs]
        ^{:key (:id org)}
        [c-org org])]]
    [:> ui/GridColumn {:computer 8 :mobile 16}
     [bc/c-profile-segment {:title (str gname " - Community Discounts")}
      (for [discount discounts]
        ^{:key (:id discount)}
        [c-discount discount])]]]])

(defn c-groups
  [groups]
  [:div
   (for [group groups]
     ^{:key (:id group)}
     [c-group group])])

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        groups& (rf/subscribe [:gql/sub
                               {:queries
                                [[:groups {:admin-org-id @org-id&
                                           :deleted nil}
                                  [:id :gname
                                   [:orgs
                                    [:id :oname
                                     [:memberships
                                      [:id
                                       [:user
                                        [:id :uname]]]]]]
                                   [:discounts
                                    ;; i.e., product 'id' and product 'idstr'
                                    [:id :idstr :pname
                                     :group-discount-descr
                                     [:vendor
                                      [:id :oname]]]]]]]}])]
    (fn []
      (if (= :loading @groups&)
        [cc/c-loader]
        [c-groups (:groups @groups&)]))))

