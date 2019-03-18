(ns vetd-admin.pages.form-templates
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :a/nav-form-templates
 (fn [{:keys [db]} _]
   {:nav {:path "/a/form-templates/"}}))

(rf/reg-event-db
 :a/route-form-templates
 (fn [db [_ query-params]]
   (assoc db
          :page :a/form-templates
          :query-params query-params)))


(defn c-page []
  (let [fts& (rf/subscribe [:gql/q
                            {:queries
                             [[:form-templates
                               [:id
                                :idstr
                                :title
                                :ftype
                                :fsubtype]]]}])
        ft-opts (->> @fts&
                     :form-templates
                     (mapv (fn [{:keys [id title]}]
                             {:key id
                              :value id
                              :text title})))
        ft& (r/atom nil)]
    (fn []
      [:div
       [:> ui/Dropdown {:fluid true
                        :form-templates true
                        :selection true
                        :onChange (fn [_ this] (reset! ft& (.-value this)))
                        :options ft-opts}]
#_       (for [p (:products @prods&)]
         ^{:key (str "form" (:id p))}
         [c-product p])])))

