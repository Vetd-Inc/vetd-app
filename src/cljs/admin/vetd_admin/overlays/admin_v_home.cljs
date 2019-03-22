(ns vetd-admin.overlays.admin-v-home
  (:require [vetd-app.flexer :as flx]
            [vetd-app.ui :as ui]   
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
                          label)))))

(defn prods->choices [prods]
  (for [{:keys [id pname]} prods]
    {:key id
     :text pname
     :value id}))

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
      [:div {:style {:display "flex"}}
       [:> ui/Dropdown {:value @sel-prod-id&
                        :onChange #(reset! org-user& (.-value %2))
                        ;; :onSearchChange #(search-results @orgs& (.-searchQuery %2))
                        :placeholder "User..."
                        :selection true
                        :search true
                        :style {:flex 1}
                        :options (search-results @orgs& "")}]
       ;; todo: this is giving the value as the product ID, is that what we want?
       [:> ui/Dropdown {:value @sel-prod-id&
                        :onChange #(reset! sel-prod-id& (.-value %2))
                        :placeholder "Select Product"
                        :selection true
                        :options (-> @prods& :products prods->choices)}]
       [:> ui/Button {:on-click #(let [{from-org-id :org-id from-user-id :user-id} @org-user&]
                                   (rf/dispatch [:a/create-preposal-req {:from-org-id from-org-id
                                                                         :from-user-id from-user-id
                                                                         :to-org-id @org-id&
                                                                         :to-user-id @user-id&
                                                                         :prod-id @sel-prod-id&}]))
                      :color "teal"}
        "Request Preposal"]])))

