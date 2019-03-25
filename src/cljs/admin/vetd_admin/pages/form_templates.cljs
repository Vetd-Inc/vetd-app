(ns vetd-admin.pages.form-templates
  (:require [vetd-app.ui :as ui]
            [vetd-app.non-exclusive-accordion :as nxa]
            [vetd-app.flexer :as flx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

;; TODO
;; add new prompt
;; add existing prompt
;; remove prompt

;; add prompt field
;; delete prompt field

;; save

(rf/reg-event-fx
 :a/nav-form-templates
 (fn [{:keys [db]} _]
   {:nav {:path "/a/form-templates/"}}))

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
    :text "Enum - Yes/No"}])

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

(defn c-prompt-field
  [{:keys [id fname descr ftype fsubtype list?] sort' :sort}]
  (let [fname& (r/atom fname)
        descr& (r/atom descr)
        ftype-pair& (r/atom [ftype fsubtype])
        list?& (r/atom list?)
        sort-order& (r/atom sort')]
    (fn [{:keys [fname descr ftype fsubtype list?] sort' :sort}]
      [nxa/accordion-item
       [:div.prompt-field-name [nxa/dropdown-icon] fname]
       [:div
        [:> ui/Form {:style {:margin "10px"
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
                         :onChange (fn [_ this] (reset! fname& (.-value this)))}]]
          [:> ui/FormField {:inline true}
           [:> ui/Label {:style {:width "200px"}} "Type"]
           [c-ftype-dropdown ftype-pair&]]
          [:> ui/FormField {:inline true}
           [:> ui/Label {:style {:width "200px"}} "Sort Order"]
           [:> ui/Input {:defaultValue @sort-order&
                         :spellCheck false
                         :fluid true
                         :onChange (fn [_ this] (reset! sort-order& (.-value this)))}]]
          [:> ui/FormField {:inline true}
           [:> ui/Label {:style {:width "200px"}} "List?"]
           [:> ui/Checkbox {:defaultValue @list?&
                            :spellCheck false
                            :fluid true                            
                            :onChange (fn [_ this] (reset! list?& (.-value this)))}]]]
         [:> ui/FormField {:inline true}
          [:> ui/Label {:style {:width "200px"}} "Description"]
          [:> ui/TextArea {:defaultValue @descr&
                           :style {:width "500px"}
                           :spellCheck false
                           :fluid true                           
                           :onChange (fn [_ this] (reset! descr& (.-value this)))}]]
          

         [:> ui/Button {:color "red"
                        :icon true
                        :labelPosition "left"
                        :on-click #(rf/dispatch [:a/delete-form-prompt-field id])}
          [:> ui/Icon {:name "trash alternate"
                       :color "white"
                       :inverted true}]
          "DELETE FIELD"]]]])))

(defn c-template-prompt
  [{:keys [fields id rpid prompt descr form-template-id] sort' :sort}]
  (let [prompt& (r/atom prompt)
        descr& (r/atom descr)
        sort-order& (r/atom sort')]
    (fn [{:keys [fields id rpid prompt descr form-template-id] sort' :sort}]
      [nxa/accordion-item
       [:div.prompt-title [nxa/dropdown-icon] prompt
        [:> ui/Button {:style {:width "150px"
                               :height "30px"
                               :font-size "small"
                               :padding 0}
                       :color "red"
                       :icon true
                       :labelPosition "left"
                       :on-click #(rf/dispatch [:a/dissoc-template-prompt rpid])}
         [:> ui/Icon {:name "remove"
                      :color "white"
                      :inverted true}]
         "Remove"]] 
       [:div
        [:> ui/Form {:style {:margin "10px"
                             :padding "10px"
                             :border "solid 1px #666666"}}
         
         [:> ui/FormGroup {:widths "equal"}
          [:> ui/FormField {:inline true}
           [:> ui/Label {:style {:width "200px"}} "Prompt"]
           [:> ui/Input {:defaultValue @prompt&
                         :fluid true
                         :spellCheck false
                         :onChange (fn [_ this] (reset! prompt& (.-value this)))}]]
          [:> ui/FormField {:inline true}
           [:> ui/Label {:style {:width "200px"}} "Description"]
           [:> ui/TextArea {:defaultValue @descr&
                            :spellCheck false
                            :fluid true                            
                            :onChange (fn [_ this] (reset! descr& (.-value this)))}]]
          [:> ui/FormField {:inline true}
           [:> ui/Label {:style {:width "200px"}} "Sort Order"]
           [:> ui/Input {:defaultValue @sort-order&
                         :spellCheck false
                         :fluid true                         
                         :onChange (fn [_ this] (reset! sort-order& (.-value this)))}]]]
         [nxa/sub-accordion
          (for [pf fields]
            [c-prompt-field pf])]
         [:> ui/Button {:color "green"
                        :icon true
                        :labelPosition "left"
                        :style {:width "150px"
                                :height "30px"
                                :font-size "small"
                                :padding 0}
                        
                                        ;:on-click #(rf/dispatch [:v/save-prompt&field {}])
                        }
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
         [:a {:href (str "./" idstr)
              :style {:font-size "large"}}
          (str title " [ " ftype " " fsubtype " ]")])])))

(defn c-page []
  (fn []
    (let [form-template-idstr @(rf/subscribe [:form-template-idstr])
          form-templates& (when form-template-idstr
                            (rf/subscribe [:gql/q
                                           {:queries
                                            [[:form-templates
                                              {:idstr form-template-idstr}
                                              [:id :idstr :title
                                               [:prompts
                                                {:_order_by {:sort :asc}}
                                                [:id :rpid :prompt :descr
                                                 :sort :form-template-id
                                                 [:fields
                                                  [:id
                                                   :fname :descr
                                                   :ftype :fsubtype
                                                   :list? :sort] ]]]]]]}]))
          existing-prompt& (r/atom nil)]
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
            [:div {:style {:font-size "x-large"}} title]
            [nxa/accordion
             (for [p prompts]
               ^{:key (str "template-prompt" (:rpid p))}
               [c-template-prompt p])]
            [:> ui/Button {:color "green"
                           :icon true
                           :labelPosition "left"
                           :on-click #(rf/dispatch [:a/create-form-template-prompt id])}
             [:> ui/Icon {:name "plus"
                          :color "white"
                          :inverted true}]
             "Add New Prompt"]
            [flx/row
             [c-prompts-dropdown existing-prompt&]
             [:> ui/Button {:color "purple"
                            :on-click #(rf/dispatch [:a/add-existing-form-template-prompt id @existing-prompt&])
                            }
              "Add Existing Prompt"]]]))])))
