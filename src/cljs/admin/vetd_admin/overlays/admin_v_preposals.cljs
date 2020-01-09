(ns vetd-admin.overlays.admin-v-preposals
  (:require [vetd-app.ui :as ui]   
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :a/create-preposal-req
 (fn [{:keys [db]} [_ {:keys [to-org-id to-user-id from-org-id from-user-id prod-id] :as prep-req}]]
   {:ws-send {:payload {:cmd :a/create-preposal-req
                        :return nil
                        :prep-req (assoc prep-req
                                         :title (str "ADMIN Preposal Request " (gensym "")))}}}))

(defn search-results
  [orgs q]
  (->> (for [org (:orgs orgs)
             {:keys [user]} (:memberships org)]
         {:text (str (:oname org) " / " (:uname user))
          :key (str (:oname org) " / " (:uname user))
          :value (clj->js {:org-id (:id org)
                           :user-id (:id user)})})
       (filter (fn [{:keys [text]}]
                 (re-find (re-pattern (str "(?i)" q)) text)))))

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
       [:> ui/Dropdown {:value @org-user&
                        :onChange #(reset! org-user& (.-value %2))
                        ;; :onSearchChange #(search-results @orgs& (.-searchQuery %2))
                        :placeholder "Type User/Org Name..."
                        :selection true
                        :search true
                        :style {:flex 1
                                :margin-right 5}
                        :options (search-results @orgs& "")}]
       [:> ui/Dropdown {:value @sel-prod-id&
                        :onChange #(reset! sel-prod-id& (.-value %2))
                        :placeholder "Select Product"
                        :style {:flex 1
                                :margin-right 5}
                        :selection true
                        :options (-> @prods& :products prods->choices)}]
       [:> ui/Button {:on-click #(rf/dispatch [:a/create-preposal-req
                                               {:from-org-id (aget @org-user& "org-id")
                                                :from-user-id (aget @org-user& "user-id")
                                                :to-org-id @org-id&
                                                :to-user-id @user-id&
                                                :prod-id @sel-prod-id&}])
                      :color "blue"}
        "Request PrePosal"]])))

