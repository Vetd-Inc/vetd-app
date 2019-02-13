(ns vetd-admin.pages.a-home
  (:require [vetd-app.util :as ut]   
            [reagent.core :as r]
            [re-frame.core :as rf]))


(defn c-page []
  (let [orgs& (rf/subscribe [:gql/q
                             {:queries
                              [[:orgs 
                                [:id :oname :idstr :short-desc
                                 [:memberships 
                                  [:id
                                   [:user
                                    [:id :idstr :uname]]]]]]]}])])
  (fn []
    [:div "ADMIN HOME"]))
