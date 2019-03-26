(ns vetd-app.docs
  (:require [vetd-app.util :as util]
            [vetd-app.flexer :as flx]
            [vetd-app.ui :as ui]
            [vetd-app.hooks :as hooks]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-sub
 :docs/enums
 (fn [[_ fsubtype]]
   (rf/subscribe [:gql/q
                  {:queries
                   [[:enum-vals {:fsubtype fsubtype}
                     [:id :value :label]]]}]))
 (fn [r _]
   (->> r
        :enum-vals
        (mapv (fn [{:keys [value label]}]
                {:key value
                 :value value
                 :text label})))))

(defn get-field-value
  "Given a reponses map, get value for prompt->field->key"
  [responses prompt field k]
  (-> (group-by (comp :prompt :prompt) responses)
      (get prompt)
      first
      :fields
      (->> (group-by (comp :fname :prompt-field)))
      (get field)
      first
      (get k)))

(defn walk-deref-ratoms
  [frm]
  (clojure.walk/postwalk
   (fn [f]
     (if (instance? reagent.ratom/RAtom f)
       @f
       f))
   frm))

(rf/reg-event-fx
 :save-form-doc
 (fn [{:keys [db]} [_ form-doc]]
   {:ws-send {:payload {:cmd :save-form-doc
                        :return nil
                        :form-doc (walk-deref-ratoms form-doc)}}}))

;; TODO support multiple response fields (for where list? = true)
(defn mk-form-doc-prompt-field-state
  [fields {:keys [id] :as prompt-field}]
  (let [{:keys [sval nval dval] :as resp-field} (some-> id fields first)
        resp-field' (merge resp-field
                           {:state (r/atom (str (or dval nval sval
                                                    "")))})]
    (assoc prompt-field
           :response
           [resp-field'])))

(defn mk-form-doc-prompt-state
  [responses {:keys [id] :as prompt}]
  (let [{:keys [fields notes] :as response} (responses id)
        response' (merge response
                         {:notes-state (r/atom (or notes ""))})
        fields' (group-by :pf-id
                          fields)]
    (-> prompt
        (assoc :response response')
        (update :fields
                (partial mapv
                         (partial mk-form-doc-prompt-field-state
                                  fields'))))))

(defn mk-form-doc-state
  [{:keys [responses] :as form-doc}]
  (let [responses' (->> responses
                        (group-by :prompt-id)
                        (util/fmap first))]
    (update form-doc
            :prompts
            (partial mapv
                     (partial mk-form-doc-prompt-state
                              responses')))))

(defn c-prompt-field-default
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)]
    [:> ui/FormField
     (when-not (= fname "value")
       [:label fname])
     [:> ui/Input {:value @value&
                   :onChange (fn [_ this]
                               (reset! value& (.-value this)))}]]))

(defn c-prompt-field-textarea
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)]
    [:> ui/FormField
     (when-not (= fname "value")
       [:label fname])
     [:> ui/TextArea {:value @value&
                      :onChange (fn [_ this]
                                  (reset! value& (.-value this)))}]]))

(defn c-prompt-field-int
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)]
    [:> ui/FormField
     (when-not (= fname "value")
       [:label fname])
     [:> ui/Input {:value @value&
                   :type "number"
                   :onChange (fn [_ this]
                               (reset! value& (.-value this)))}]]))

(defn c-prompt-field-enum
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)
        enum-vals (rf/subscribe [:docs/enums fsubtype])]
    (fn [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
      [:> ui/FormField
       (when-not (= fname "value")
         [:label fname])
       [:> ui/Dropdown {:value @value&
                        :onChange #(reset! value& (.-value %2))
                        ;; :placeholder "Select Product"
                        :selection true
                        :options @enum-vals}]])))

(defn c-prompt-default
  [{:keys [prompt descr fields]}]
  [:<>
   [:div {:style {:margin-bottom 5}}
    prompt
    (when descr
      [:> ui/Popup {:trigger (r/as-element [:> ui/Icon {:name "info circle"}])
                    :wide true}
       descr])]
   (for [{:keys [idstr ftype fsubtype] :as f} fields]
     ^{:key (str "field" (:id f))}
     [(hooks/c-prompt-field idstr [ftype fsubtype] ftype :default)
      f])])

(defn prep-preposal-form-doc
  [{:keys [id title product to-org to-user from-org from-user doc-id doc-title prompts] :as form-doc}]
  (assoc form-doc
         :doc-title (str "Preposal for "
                         (:pname product)
                         " [ "
                         (:oname to-org)
                         " => "
                         (:oname from-org)
                         " ]")
         :doc-notes ""
         :doc-descr ""
         :doc-dtype "preposal"
         :doc-dsubtype "preposal1"))

(defn c-form-maybe-doc
  [{:keys [id title product from-org from-user doc-id doc-title prompts] :as form-doc}]
  [:> ui/Form {:style {:width 400
                       :margin-bottom 50}}
   [:div
    (or doc-title title)
    [:div.product-name (:pname product)]
    [:div.org-name (:oname from-org)]
    [:div.user-name (:uname from-user)]]
   (for [p (sort-by :sort prompts)]
     ^{:key (str "prompt" (:id p))}
     [(hooks/c-prompt :default) p])
   [:> ui/Button {:color "blue"
                  :fluid true
                  :on-click #(rf/dispatch [:save-form-doc
                                           (prep-preposal-form-doc form-doc)])}
    "Submit"]])


(hooks/reg-hooks! hooks/c-prompt
                  {:default #'c-prompt-default})

(hooks/reg-hooks! hooks/c-prompt-field
                  {:default #'c-prompt-field-default
                   ["s" "multi"] #'c-prompt-field-textarea
                   ["n" "int"] #'c-prompt-field-int
                   "e" #'c-prompt-field-enum})
