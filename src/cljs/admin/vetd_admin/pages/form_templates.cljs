(ns vetd-admin.pages.form-templates
  (:require [vetd-app.ui :as ui]
            [vetd-app.flexer :as flx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-event-fx
 :a/nav-form-templates
 (fn [_ [_ idstr]]
   {:nav {:path (str "/a/form-templates" (when idstr (str "/" idstr)))}}))

(rf/reg-event-fx
 :a/create-form-template-prompt
 (fn [_ [_ form-template-id]]
   {:ws-send {:payload {:cmd :a/create-form-template-prompt
                        :form-template-id form-template-id}}}))

(rf/reg-event-fx
 :a/dissoc-template-prompt
 (fn [_ [_ form-template-prompt-id]]
   {:ws-send {:payload {:cmd :a/dissoc-template-prompt
                        :form-template-prompt-id form-template-prompt-id}}}))

(rf/reg-event-fx
 :a/add-existing-form-template-prompt
 (fn [_ [_ form-template-prompt-id prompt-id]]
   {:ws-send {:payload {:cmd :a/add-existing-form-template-prompt
                        :form-template-prompt-id form-template-prompt-id
                        :prompt-id prompt-id}}}))

(rf/reg-event-fx
 :a/delete-form-prompt-field
 (fn [_ [_ prompt-field-id]]
   {:ws-send {:payload {:cmd :a/delete-form-prompt-field
                        :prompt-field-id prompt-field-id}}}))

(rf/reg-event-fx
 :a/create-prompt-field
 (fn [_ [_ prompt-id]]
   {:ws-send {:payload {:cmd :a/create-prompt-field
                        :prompt-id prompt-id}}}))

(rf/reg-event-fx
 :a/update-template-prompt&fields
 (fn [_ [_ changes]]
   {:ws-send (mapv (fn [[k v]]
                     {:payload {:cmd :a/update-any
                                :entity v}})
                   changes)}))

(rf/reg-event-fx
 :a/create-form-from-template
 (fn [_ [_ form-template-id]]
   {:ws-send {:payload {:cmd :a/create-form-from-template
                        :form-template-id form-template-id}}}))

(rf/reg-event-db
 :a/route-form-templates
 (fn [db [_ form-template-idstr]]
   (assoc db
          :page :a/form-templates
          :page-params {:form-template-idstr form-template-idstr})))

(rf/reg-sub
 :form-template-idstr
 :<- [:page-params]
 (fn [{:keys [form-template-idstr]}] form-template-idstr))

(def fsubtypes
  [{:key :n-int
    :value [:n :int]
    :text "Numeric - Integer"}
   {:key :s-single
    :value [:s :single]
    :text "Text - Single Line"}
   {:key :s-multi
    :value [:s :multi]
    :text "Text - Multi Line"}
   {:key :e-price-per
    :value [:e :e-price-per]
    :text "Enum - Price Per"}
   {:key :e-yes-no
    :value [:e :e-yes-no]
    :text "Enum - Yes/No"}
   {:key :d-category
    :value [:d :d-category]
    :text "Dynamic - Category"}])

(defn c-ftype-dropdown [state&]
  [:> ui/Dropdown {:defaultValue @state&
                   :fluid true
                   :selection true
                   :onChange (fn [_ this] (reset! state& (.-value this)))
                   :options fsubtypes}])

(defn c-prompts-dropdown [state&]
  (let [opts (->> @(rf/subscribe [:gql/q
                                  {:queries
                                   [[:prompts
                                     [:id :prompt]]]}])
                  :prompts
                  (mapv (fn [{:keys [id prompt]}]
                          {:key id
                           :value id
                           :text prompt})))]
    [:> ui/Dropdown {:defaultValue @state&
                     :style {:width "400px"
                             :height "40px"}
                     :search true                     
                     :selection true
                     :onChange (fn [_ this] (reset! state& (.-value this)))
                     :options opts}]))

(defn on-change-fn
  [changes& changes-fn value&]
  (fn [_ this]
    (let [new-val (or (.-value this)
                      (.-checked this))]
      (reset! value& new-val)
      (swap! changes& changes-fn))))

(defn c-prompt-field
  [{:keys [id fname descr ftype fsubtype list?] sort' :sort} changes&]
  (let [fname& (r/atom fname)
        descr& (r/atom descr)
        ftype-pair& (r/atom [ftype fsubtype])
        list?& (r/atom list?)
        sort-order& (r/atom sort')
        changes-fn #(assoc % id
                           {:id id
                            :fname @fname&
                            :descr @descr&
                            :list_qm  (boolean @list?&) ; HACK list_qm => list?
                            :sort @sort-order&
                            :ftype (first @ftype-pair&)
                            :fsubtype (second @ftype-pair&)})
        mk-on-change-fn (partial on-change-fn changes& changes-fn)]
    (fn [{:keys [fname descr ftype fsubtype list?] sort' :sort}]
      [ui/nx-accordion-item
       ^{:key (str "template-prompt-field-name" id)}
       [:div.prompt-field-name
        [:> ui/Icon {:name "dropdown"}]
        fname]
       
       ^{:key (str "template-prompt-field-form" id)}
       [:div {:style {:margin "10px"
                      :padding "10px"
                      :background-color "#EFEFEF"
                      :border-left "solid 3px #999999"}}
        [:> ui/FormGroup {:widths "equal"}
         [:> ui/FormField {:inline true}
          [:> ui/Label {:style {:width "200px"}} "Field Name"]
          [:> ui/Input {:defaultValue @fname&
                        :style {:width "300px"}
                        :fluid true
                        :spellCheck false
                        :onChange (mk-on-change-fn fname&)}]]
         [:> ui/FormField {:inline true}
          [:> ui/Label {:style {:width "200px"}} "Type"]
          [c-ftype-dropdown ftype-pair&]]
         [:> ui/FormField {:inline true}
          [:> ui/Label {:style {:width "200px"}} "Sort Order"]
          [:> ui/Input {:defaultValue @sort-order&
                        :spellCheck false
                        :fluid true
                        :onChange (mk-on-change-fn sort-order&)}]]
         [:> ui/FormField {:inline true}
          [:> ui/Label {:style {:width "200px"}} "List?"]
          [:> ui/Checkbox {:defaultChecked @list?&
                           :spellCheck false
                           :fluid true                            
                           :onChange (mk-on-change-fn list?&)}]]]
        [:> ui/FormField {:inline true}
         [:> ui/Label {:style {:width "200px"}} "Description"]
         [:> ui/TextArea {:defaultValue @descr&
                          :style {:width "500px"}
                          :spellCheck false
                          :fluid true                           
                          :onChange (mk-on-change-fn descr&)}]]
        [:> ui/Button {:color "red"
                       :icon true
                       :labelPosition "left"
                       :on-click (fn [e]
                                   (.stopPropagation e)
                                   (rf/dispatch [:a/delete-form-prompt-field id]))}
         [:> ui/Icon {:name "trash alternate"
                      :color "white"
                      :inverted true}]
         "Delete Field"]]])))

(defn c-template-prompt
  [{:keys [fields id rpid prompt term descr form-template-id] sort' :sort}]
  (let [changes& (r/atom {})
        prompt& (r/atom prompt)
        term& (r/atom term)
        descr& (r/atom descr)
        sort-order& (r/atom sort')
        changes-fn #(-> %
                        (assoc id
                               {:id id
                                :prompt @prompt&
                                :descr @descr&
                                :term @term&})
                        (assoc rpid
                               {:id rpid
                                :sort (js/parseInt @sort-order&)}))
        mk-on-change-fn (partial on-change-fn changes& changes-fn)]
    (fn [{:keys [fields id rpid prompt descr form-template-id] sort' :sort}]
      [ui/nx-accordion-item
       ^{:key (str "template-prompt-title" id)}
       [:div.prompt-title
        [:> ui/Icon {:name "dropdown"}]
        prompt
        [:> ui/Button {:color "red"
                       :size "tiny"
                       :icon true
                       :labelPosition "left"
                       :on-click (fn [e]
                                   (.stopPropagation e)
                                   (rf/dispatch [:a/dissoc-template-prompt rpid]))}
         [:> ui/Icon {:name "remove"
                      :color "white"
                      :inverted true}]
         "Remove"]
        (when (not-empty @changes&)
          [:> ui/Button {:style {:width "150px"
                                 :height "30px"
                                 :font-size "small"
                                 :padding 0}
                         :color "green"
                         :icon true
                         :labelPosition "left"
                         :on-click (fn [e]
                                     (.stopPropagation e)
                                     (rf/dispatch [:a/update-template-prompt&fields @changes&])
                                     (reset! changes& {}))}
           [:> ui/Icon {:name "save"
                        :color "white"
                        :inverted true}]
           "Save"])]
       
       ^{:key (str "template-prompt-form" id)}
       [:div
        [:> ui/Form {:as "div"
                     :style {:margin "10px"
                             :padding "10px"
                             :border "solid 1px #666666"}}
         
         [:> ui/FormGroup {:widths "equal"}
          [:> ui/FormField {:inline true}
           [:> ui/Label {:style {:width "200px"}} "Prompt"]
           [:> ui/Input {:defaultValue @prompt&
                         :fluid true
                         :spellCheck false
                         :onChange (mk-on-change-fn prompt&)}]]
          [:> ui/FormField {:inline true}
           [:> ui/Label {:style {:width "200px"}} "Term"]
           [:> ui/Input {:defaultValue @term&
                         :fluid true
                         :spellCheck false
                         :onChange (mk-on-change-fn term&)}]]
          [:> ui/FormField {:inline true}
           [:> ui/Label {:style {:width "200px"}} "Sort Order"]
           [:> ui/Input {:defaultValue @sort-order&
                         :spellCheck false
                         :fluid true                         
                         :onChange (mk-on-change-fn sort-order&)}]]]
         [:> ui/FormField {:inline true}
          [:> ui/Label {:style {:width "200px"}} "Description"]
          [:> ui/TextArea {:defaultValue @descr&
                           :spellCheck false
                           :fluid true                            
                           :onChange (mk-on-change-fn descr&)}]]         
         [ui/nx-sub-accordion
          (for [pf fields]
            ^{:key (str "template-prompt-field" (:id pf))}
            [c-prompt-field pf changes&])]
         [:> ui/Button {:color "green"
                        :icon true
                        :labelPosition "left"
                        :style {:width "150px"
                                :height "30px"
                                :font-size "small"
                                :padding 0}
                        :on-click #(rf/dispatch [:a/create-prompt-field id])}
          [:> ui/Icon {:name "add"
                       :color "white"
                       :inverted true}]
          "Add Field"]]]])))

(defn c-form-template-list []
  (let [fts& (rf/subscribe [:gql/q
                            {:queries
                             [[:form-templates
                               [:id
                                :idstr
                                :title
                                :ftype
                                :fsubtype]]]}])]
    (fn []
      [:div
       (for [{:keys [id idstr title ftype fsubtype]} (:form-templates @fts&)]
         ^{:key (str "template-link" id)}
         [:div {:style {:margin "20px"}}
          [:a {:onClick #(rf/dispatch [:a/nav-form-templates idstr])
               :style {:font-size "large"
                       :cursor "pointer"}}
           (str title " [ " ftype " " fsubtype " ]")]])])))

(defn c-page []
  (fn []
    (let [form-template-idstr @(rf/subscribe [:form-template-idstr])
          form-templates& (when form-template-idstr
                            (rf/subscribe [:gql/sub
                                           {:queries
                                            [[:form-templates
                                              {:idstr form-template-idstr}
                                              [:id :idstr :title
                                               [:prompts
                                                {:ref-deleted nil
                                                 :_order_by {:sort :asc}}
                                                [:id :rpid :prompt :descr
                                                 :sort :form-template-id
                                                 :term
                                                 [:fields
                                                  {:deleted nil
                                                   :_order_by {:sort :asc}}
                                                  [:id
                                                   :fname :descr
                                                   :ftype :fsubtype
                                                   :list? :sort] ]]]]]]}]))
          existing-prompt& (r/atom nil)
          show-confirm-publish? (r/atom false)]
      [:div#admin-form-templates-page
       {:style {:margin "0 0 100px 50px"
                :width "1200px"}}
       (if-not form-template-idstr
         [c-form-template-list]
         (let [{:keys [id title prompts]} (some-> form-templates&
                                                  deref
                                                  :form-templates
                                                  first)]
           [:<>
            [:h1 title]
            [ui/nx-accordion
             (for [p prompts]
               ^{:key (str "template-prompt" (:rpid p))}
               [c-template-prompt p])]
            [:> ui/Button {:style {:width "250px"
                                   :margin "20px 0"}
                           :color "green"
                           :icon true
                           :labelPosition "left"
                           :on-click (fn [e]
                                       (.stopPropagation e)
                                       (rf/dispatch [:a/create-form-template-prompt id]))}
             [:> ui/Icon {:name "plus"
                          :color "white"
                          :inverted true}]
             "Add New Prompt"]
            [flx/row
             [c-prompts-dropdown existing-prompt&]
             [:> ui/Button {:color "blue"
                            :on-click (fn [e]
                                        (.stopPropagation e)
                                        (rf/dispatch [:a/add-existing-form-template-prompt id @existing-prompt&]))}
              "Add Existing Prompt"]]
            [:div {:style {:margin-top 100}}
             [:> ui/Button {:color "purple"
                            :on-click (fn [e]
                                        (.stopPropagation e)
                                        (when (js/confirm "Are you sure you want to publish?")
                                          (rf/dispatch [:a/create-form-from-template id title])))}
              "Publish As Form"]]]))])))
