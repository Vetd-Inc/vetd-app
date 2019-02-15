(ns vetd-admin.overlays.admin-v-home
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]   
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :a/create-preposal-req
 (fn [{:keys [db]} [_ {:keys [to-org-id to-user-id from-org-id from-user-id prod-id] :as prep-req}]]
   {:ws-send {:payload {:cmd :a/create-preposal-req
                        :return nil
                        :prep-req prep-req}}}))

(defn search-results
  [orgs q]
  (->> (for [org (:orgs orgs)
             {:keys [user]} (:memberships org)]
         {:label (str (:oname org) " / " (:uname user))
          :org-id (:id org)
          :user-id (:id user)})
       (filter (fn [{:keys [label]}]
                 (re-find (re-pattern (str "(?i)" q))
                          label))))
  #_[{:label "hello" :value 44}])

(defn prods->choices [prods]
  (for [{:keys [id pname]} prods]
    {:id id :label pname}))

(defn c-overlay []
  (let [org-id& (rf/subscribe [:org-id])
        user-id& (rf/subscribe [:user-id])        
        model& (r/atom {})
        sel-prod-id& (r/atom nil)
        org-user& (r/atom nil)
        orgs& (rf/subscribe [:gql/q
                             {:queries
                              [[:orgs {:buyer? true}
                                [:id :oname :idstr
                                 [:memberships
                                  [:id
                                   [:user
                                    [:id :idstr :uname]]]]]]]}])
        prods& (rf/subscribe [:gql/q
                              {:queries
                               [[:products {:vendor-id @org-id&}
                                     [:id :pname :idstr]]]}])]
    (fn []
      [flx/row
       [rc/typeahead
        :data-source (partial search-results @orgs&)
        :model model&
        :render-suggestion (fn [{:keys [label]}]
                             [:span label])
        :suggestion-to-string #(:label %)
        :rigid? true
        :on-change #(reset! org-user& %)]
       [rc/single-dropdown
        :choices (-> @prods& :products prods->choices)
        :model sel-prod-id&
        :on-change #(reset! sel-prod-id& %)]
       #_(str @prods&)
       [rc/button
        :label "Request Preposal"
        :on-click #(let [{from-org-id :org-id from-user-id :user-id} @org-user&]
                     (rf/dispatch [:a/create-preposal-req {:from-org-id from-org-id
                                                           :from-user-id from-user-id
                                                           :to-org-id @org-id&
                                                           :to-user-id @user-id&
                                                           :prod-id @sel-prod-id&}]))]])))

