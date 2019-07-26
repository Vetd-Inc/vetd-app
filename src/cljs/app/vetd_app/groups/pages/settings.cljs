(ns vetd-app.groups.pages.settings
  (:require [vetd-app.ui :as ui]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(rf/reg-event-fx
 :g/nav-settings
 (constantly
  {:nav {:path "/c/settings"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Groups Settings"}}}))

(rf/reg-event-fx
 :g/route-settings
 (fn [{:keys [db]}]
   {:db (assoc db
               :page :g/settings
               :page-params {:fields-editing #{}})
    :analytics/page {:name "Groups Settings"}}))

(rf/reg-event-fx
 :g/add-orgs-to-group
 (fn [{:keys [db]} [_ group-id org-ids]]
   {:ws-send {:payload {:cmd :g/add-orgs-to-group
                        :return {:handler :g/add-orgs-to-group-return
                                 :org-ids org-ids}
                        :group-id group-id
                        :org-ids org-ids}}
    :analytics/track {:event "Add Organization to Community"
                      :props {:category "Community"}}}))

(rf/reg-event-fx
 :g/add-orgs-to-group-return
 (fn [{:keys [db]} [_ _ {{:keys [org-ids]} :return}]]
   {:toast {:type "success"
            :title (str "Organization" (when (> (count org-ids) 1) "s") " Added to Community")}
    :dispatch [:stop-edit-field "add-orgs-to-group"]}))

;; use this to remove a user from an org
;; (rf/reg-event-fx
;;  :delete-membership
;;  (fn [{:keys [db]} [_ memb-id user-name org-id org-name]]
;;    {:ws-send {:payload {:cmd :delete-membership
;;                         :return {:handler :delete-membership-return
;;                                  :user-name user-name
;;                                  :org-name org-name}
;;                         :id memb-id}}
;;     :analytics/track {:event "Remove User From Org"
;;                       :props {:category "Account"
;;                               :label org-id}}}))

;; (rf/reg-event-fx
;;  :delete-membership-return
;;  (fn [{:keys [db]} [_ _ {{:keys [user-name org-name]} :return}]]
;;    {:toast {:type "success"
;;             :title "Member Removed from Organization"
;;             :message (str user-name " has been removed from " org-name)}}))

(defn c-add-orgs-form [group]
  (let [fields-editing& (rf/subscribe [:fields-editing])
        bad-input& (rf/subscribe [:bad-input])
        value& (r/atom [])
        options& (r/atom []) ; options from search results + current values
        search-query& (r/atom "")
        orgs->options (fn [orgs]
                        (for [{:keys [id oname]} orgs]
                          {:key id
                           :text oname
                           :value id}))]
    (fn [group]
      (when (@fields-editing& "add-orgs-to-group")
        (let [orgs& (rf/subscribe
                     [:gql/q
                      {:queries
                       [[:orgs {:_where {:oname {:_ilike (str "%" @search-query& "%")}}
                                :_limit 25
                                :_order_by {:oname :asc}}
                         [:id :oname]]]}])
              org-ids-already-in-group (set (map :id (:orgs group)))
              _ (when-not (= :loading @orgs&)
                  (let [options (->> @orgs&
                                     :orgs
                                     orgs->options ; now we have options from gql sub
                                     ;; (this dumbly actually keeps everything, but that seems fine)
                                     (concat @options&) ; keep options for the current values
                                     distinct
                                     (remove (comp (partial contains? org-ids-already-in-group) :value)))]
                    (when-not (= @options& options)
                      (reset! options& options))))]
          [:> ui/Form {:as "div"
                       :class "popup-dropdown-form"} ;; popup is a misnomer here
           [:> ui/FormField {:error (= @bad-input& :add-orgs-to-group)
                             :style {:padding-top 7
                                     :width "100%"}
                             ;; this class combo w/ width 100% is a hack
                             :class "ui action input"}
            [:> ui/Dropdown {:loading (= :loading @orgs&)
                             :options @options&
                             :placeholder "Search organizations..."
                             :search true
                             :selection true
                             :multiple true
                             ;; :auto-focus true ;; TODO this doesn't work
                             :selectOnBlur false
                             :selectOnNavigation true
                             :closeOnChange true
                             :allowAdditions false ;; TODO this should be changed to true when we allow invites of new orgs
                             ;; :additionLabel "Hit 'Enter' to Add "
                             ;; :onAddItem (fn [_ this]
                             ;;              (->> this
                             ;;                   .-value
                             ;;                   vector
                             ;;                   ui/as-dropdown-options
                             ;;                   (swap! options& concat)))
                             :onSearchChange (fn [_ this] (reset! search-query& (aget this "searchQuery")))
                             :onChange (fn [_ this] (reset! value& (.-value this)))}]
            [:> ui/Button
             {:color "blue"
              :disabled (empty? @value&)
              :on-click #(rf/dispatch [:g/add-orgs-to-group (:id group) (js->clj @value&)])}
             "Add"]]])))))

(defn c-org
  [org group]
  (let [popup-open? (r/atom false)]
    (fn [{:keys [id idstr oname memberships] :as org}
         {:keys [gname] :as group}]
      (let [num-members (count memberships)]
        [cc/c-field {:label [:<>
                             [:> ui/Popup
                              {:position "bottom right"
                               :on "click"
                               :open @popup-open?
                               :on-close #(reset! popup-open? false)
                               :content (r/as-element
                                         [:div
                                          [:h5 "Are you sure you want to remove " oname " from " gname "?"]
                                          [:> ui/ButtonGroup {:fluid true}
                                           [:> ui/Button {:on-click #(reset! popup-open? false)}
                                            "Cancel"]
                                           [:> ui/Button {:on-click (fn []
                                                                      (reset! popup-open? false)
                                                                      #_(rf/dispatch [:delete-membership id user-name org-id org-name]))
                                                          :color "red"}
                                            "Remove"]]])
                               :trigger (r/as-element
                                         [:> ui/Label {:on-click #(swap! popup-open? not)
                                                       :as "a"
                                                       :style {:float "right"
                                                               :margin-top 5}}
                                          [:> ui/Icon {:name "remove"}]
                                          "Remove"])}]
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

(defn c-discount
  [{:keys [id idstr pname group-discount-descr vendor] :as discount}]
  [cc/c-field {:label [:<>
                       [:a.name {:on-click #(rf/dispatch [:b/nav-product-detail idstr])}
                        pname]
                       [:br]
                       [:small (:oname vendor)]]
               :value group-discount-descr}])

(defn c-group
  [group]
  (let [fields-editing& (rf/subscribe [:fields-editing])]
    (fn [{:keys [id gname orgs discounts] :as group}]
      [:> ui/Grid {:stackable true}
       [:> ui/GridRow
        [:> ui/GridColumn {:computer 8 :mobile 16}
         [bc/c-profile-segment
          {:title [:<>
                   (if (@fields-editing& "add-orgs-to-group")
                     [:> ui/Label {:on-click #(rf/dispatch
                                               [:stop-edit-field "add-orgs-to-group"])
                                   :as "a"
                                   :style {:float "right"}}
                      "Cancel"]
                     [:> ui/Label {:on-click #(rf/dispatch
                                               [:edit-field "add-orgs-to-group"])
                                   :as "a"
                                   :color "teal"
                                   :style {:float "right"}}
                      [:> ui/Icon {:name "add group"}]
                      "Add Organization"])
                   "Organizations"
                   ;; [:br]
                   ;; [:span {:style {:font-size 16}} gname]
                   [c-add-orgs-form group]]}
          (for [org orgs]
            ^{:key (:id org)}
            [c-org org group])]]
        [:> ui/GridColumn {:computer 8 :mobile 16}
         [bc/c-profile-segment {:title [:<> "Discounts"
                                        ;; [:br]
                                        ;; [:span {:style {:font-size 16}} gname]
                                        ]}
          (for [discount discounts]
            ^{:key (:id discount)}
            [c-discount discount])]]]])))

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

