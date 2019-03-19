(ns vetd-admin.pages.form-templates
  (:require [vetd-app.ui :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :a/nav-form-templates
 (fn [{:keys [db]} _]
   {:nav {:path "/a/form-templates/"}}))

(rf/reg-event-db
 :a/route-form-templates
 (fn [db [_ url-form-template-idstr]]
   (assoc db
          :page :a/form-templates
          :query-params {:form-template-idstr url-form-template-idstr})))

(defn c-prompt-field
  [{:keys [fname descr ftype fsubtype list?] sort' :sort}]
  (let [fname& (r/atom fname)
        descr& (r/atom descr)
        ftype& (r/atom ftype)
        fsubtype& (r/atom fsubtype)
        list?& (r/atom list?)
        sort-order& (r/atom sort')]
    (fn [{:keys [fname descr ftype fsubtype list?] sort' :sort}]
      [:div
       [:> ui/Form {:style {:margin "10px"
                            :padding "10px"
                            :background-color "#EFEFEF"
                            :border-left "solid 3px #999999"}}
        [:> ui/FormField {:inline true}
         [:> ui/Label {:style {:width "200px"}} "Field Name"]
         [:> ui/Input {:defaultValue @fname&
                       :style {:width "300px"}
                       :spellCheck false
                       :onChange (fn [_ this] (reset! fname& (.-value this)))}]]
        [:> ui/FormField {:inline true}
         [:> ui/Label {:style {:width "200px"}} "Description"]
         [:> ui/TextArea {:defaultValue @descr&
                          :spellCheck false
                          :onChange (fn [_ this] (reset! descr& (.-value this)))}]]
        [:> ui/FormField {:inline true}
         [:> ui/Label {:style {:width "200px"}} "Sort Order"]
         [:> ui/Input {:defaultValue @sort-order&
                       :spellCheck false
                       :onChange (fn [_ this] (reset! sort-order& (.-value this)))}]]
        [:> ui/FormField {:inline true}
         [:> ui/Label {:style {:width "200px"}} "List?"]
         [:> ui/Checkbox {:defaultValue @list?&
                          :spellCheck false
                          :onChange (fn [_ this] (reset! list?& (.-value this)))}]]
        [:> ui/Button {:color "teal"
                       :fluid true
                                        ;                       :on-click #(rf/dispatch [:v/save-prompt&field {}])
                       }
         "Save Product"]
        [:> ui/Button {:color "red"
                       :fluid true
                                        ;:on-click #(rf/dispatch [:v/delete-product id])
                       }
         "DELETE  Product"]]])))

(defn c-template-prompt
  [{:keys [fields id rpid prompt descr form-template-id] sort' :sort}]
  (let [prompt& (r/atom prompt)
        descr& (r/atom descr)
        sort-order& (r/atom sort')]
    (fn [{:keys [fields id rpid prompt descr form-template-id] sort' :sort}]
      [:div
       [:> ui/Form {:style {:margin "10px"
                            :padding "10px"
                            :border "solid 1px #666666"}}
        [:> ui/FormField {:inline true}
         [:> ui/Label {:style {:width "200px"}} "Prompt"]
         [:> ui/Input {:defaultValue @prompt&
                       :style {:width "300px"}
                       :spellCheck false
                       :onChange (fn [_ this] (reset! prompt& (.-value this)))}]]
        [:> ui/FormField {:inline true}
         [:> ui/Label {:style {:width "200px"}} "Description"]
         [:> ui/TextArea {:defaultValue @descr&
                          :spellCheck false
                          :onChange (fn [_ this] (reset! descr& (.-value this)))}]]
        [:> ui/FormField {:inline true}
         [:> ui/Label {:style {:width "200px"}} "Sort Order"]
         [:> ui/Input {:defaultValue @sort-order&
                       :spellCheck false
                       :onChange (fn [_ this] (reset! sort-order& (.-value this)))}]]
        (for [pf fields]
          [c-prompt-field pf])
        [:> ui/Button {:color "teal"
                       :fluid true
                                        ;:on-click #(rf/dispatch [:v/save-prompt&field {}])
                       }
         "Save Product"]
        [:> ui/Button {:color "red"
                       :fluid true
                                        ;:on-click #(rf/dispatch [:v/delete-product id])
                       }
         "DELETE  Product"]]
       [:div (str fields)]])))

(defn c-form-template-list []
  (let [fts& (rf/subscribe [:gql/q
                            {:queries
                             [[:form-templates
                               [:id
                                :idstr
                                :title
                                :ftype
                                :fsubtype]]]}])])
  (fn []
    [:div
     (for [{:keys [id idstr]} (:form-templates @fts&)]
       ^{:key (str "template-link" id)}
       [:a {:href (str "./" idstr)}])]))

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
                              :text title})))]
    (fn []
      (let [prompts& (when url-form-template-id
                       (rf/subscribe [:gql/q
                                      {:queries
                                       [[:form-templates
                                         {:idstr url-form-template-idstr}
                                         [:id :idstr
                                          [:prompts
                                           [:id :rpid :prompt :descr
                                            :sort :form-template-id
                                            [:fields
                                             [:fname :descr
                                              :ftype :fsubtype
                                              :list? :sort] ]]]]]]}]))]
        (def p1 (when prompts& @prompts&))
         
        [:div {:style {:margin-left "100px"
                       :width "700px"}}
         (when-not url-form-template-id
           [:> ui/Dropdown {:fluid true
                            :selection true
                            :onChange (fn [_ this] (reset! ft-id& (.-value this)))
                            :options ft-opts}])
         (for [p (some-> prompts& deref :form-templates
                         first :prompts)]
           ^{:key (str "template-prompt" (:rpid p))}
           [c-template-prompt p])]))))


#_(cljs.pprint/pprint p1)
