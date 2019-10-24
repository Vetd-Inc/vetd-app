(ns vetd-app.groups.pages.detail
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [vetd-app.common.fx :as cfx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(rf/reg-event-fx
 :g/nav-detail
 (fn [_ [_ group-idstr]]
   {:nav {:path (str "/c/" group-idstr)}}))

(rf/reg-event-fx
 :g/route-detail
 (fn [{:keys [db]} [_ group-idstr]]
   {:db (assoc db
               :page :g/detail
               :page-params {:group-idstr group-idstr})
    :analytics/page {:name "Community Detail"
                     :props {:group-idstr group-idstr}}}))

;;;; Subscriptions
(rf/reg-sub
 :group-idstr
 :<- [:page-params] 
 (fn [{:keys [group-idstr]}] group-idstr))

;;;; Components
(defn c-org
  [org group]
  (let [org-id& (rf/subscribe [:org-id])]
    (fn [{:keys [id idstr oname memberships stack-items] :as org}
         {:keys [gname] :as group}]
      (let [num-members (count memberships)
            num-stack-items (count stack-items)]
        [cc/c-field {:label [:<>
                             (when (pos? num-stack-items)
                               [:> ui/Button {:on-click (fn []
                                                          (if (= id @org-id&)
                                                            (rf/dispatch [:b/nav-stack])
                                                            (rf/dispatch [:b/nav-stack-detail idstr])))
                                              :as "a"
                                              :size "small"
                                              :color "lightblue"
                                              :style {:float "right"
                                                      :width 170
                                                      :text-align "left"
                                                      :margin-top 7}}
                                [:> ui/Icon {:name "grid layout"}]
                                (str " " num-stack-items " Stack Item" (when-not (= num-stack-items 1) "s"))])
                             oname]
                     :value [:<> (str num-members " member" (when-not (= num-members 1) "s") " ")
                             [:> ui/Popup
                              {:position "bottom left"
                               :wide "very"
                               :offset -10
                               :content (let [max-members-show 15]
                                          (str (s/join ", " (->> memberships
                                                                 (map (comp :uname :user))
                                                                 (take max-members-show)))
                                               (when (> num-members max-members-show)
                                                 (str " and " (- num-members max-members-show) " more."))))
                               :trigger (r/as-element
                                         [:> ui/Icon {:name "question circle"}])}]]}]))))

(defn c-orgs
  [{:keys [id gname orgs] :as group}]
  (let [orgs-sorted (sort-by (comp count :stack-items) > orgs)]
    [bc/c-profile-segment {:title [:<>
                                   [:> ui/Icon {:name "group"}]
                                   " " gname " Community"]}
     (for [org orgs-sorted]
       ^{:key (:id org)}
       [c-org org group])]))

(defn c-group
  [group]
  [c-orgs group])

(defn c-page []
  (let [group-idstr& (rf/subscribe [:group-idstr])
        org-id& (rf/subscribe [:org-id])
        groups& (rf/subscribe [:gql/sub
                               {:queries
                                [[:groups {:idstr @group-idstr&
                                           :_limit 1
                                           :deleted nil}
                                  [:id :gname
                                   [:orgs
                                    [:id :idstr :oname
                                     [:memberships
                                      [:id
                                       [:user
                                        [:id :uname]]]]
                                     [:stack-items
                                      [:id :idstr :status]]]]
                                   [:top-products {:_order_by {:count-stack-items :desc}
                                                   :_limit 10}
                                    [:group-id :product-id :count-stack-items]]]]]}])]
    (fn []
      (if (= :loading @groups&)
        [cc/c-loader]
        [:div.container-with-sidebar
         [:div.sidebar
          [:div {:style {:padding "0 15px"}}
           [bc/c-back-button "Back"]]]
         [:div.inner-container
          [:> ui/Grid {:stackable true}
           [:> ui/GridRow
            [:> ui/GridColumn {:computer 10 :mobile 16}
             [c-group (first (:groups @groups&))]]
            [:> ui/GridColumn {:computer 6 :mobile 16} ]]]]]))))
