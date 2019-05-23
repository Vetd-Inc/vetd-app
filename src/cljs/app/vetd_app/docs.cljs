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

(defn get-value-by-term
  [response-prompts term & [field val-type]]
  (let [term-str (util/kw->str term)]
    (get (->> response-prompts
              (filter #(-> % :prompt-term (= term-str)))
              first
              :response-prompt-fields
              (filter #(-> %
                           :prompt-field-fname
                           (= (or field "value"))))
              first)
         (or val-type :sval))))

(defn get-value-by-prompt-id
  [response-prompts prompt-id & [field val-type]]
  (get (->> response-prompts
            (filter #(-> % :prompt-id (= prompt-id)))
            first
            :response-prompt-fields
            (filter #(-> %
                         :prompt-field-fname
                         (= (or field "value"))))
            first)
       (or val-type :sval)))

(defn get-response-prompt-by-prompt-id
  [response-prompts prompt-id]
  (->> response-prompts
       (filter #(-> % :prompt-id (= prompt-id)))
       first))

(defn get-response-field-by-prompt-id
  "A little more info than get-value-by-prompt-id."
  [response-prompts prompt-id & [field]]
  (->> response-prompts
       (filter #(-> % :prompt-id (= prompt-id)))
       first
       :response-prompt-fields
       (filter #(-> %
                    :prompt-field-fname
                    (= (or field "value"))))
       first))

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

(defn get-field-value-from-response-prompt
  "Given a reponse-prompts map, get value for prompt->field->key"
  [response-prompts prompt field k]
  (get (->> response-prompts
            (filter #(-> % :prompt-prompt (= prompt)))
            first
            :response-prompt-fields
            (filter #(-> % :prompt-field-fname (= field)))
            first)
       k))

(defn walk-deref-ratoms
  [frm]
  (clojure.walk/prewalk
   (fn [f]
     (if (instance? reagent.ratom/RAtom f)
       @f
       f))
   frm))

(rf/reg-event-fx
 :save-form-doc
 (fn [{:keys [db]} [_ form-doc]]
   (let [fd (walk-deref-ratoms form-doc)]
     {:ws-send {:payload {:cmd :save-form-doc
                          :return nil
                          :form-doc fd}}})))


;; TODO support multiple response fields (for where list? = true)
#_(defn mk-form-doc-prompt-field-state
  [fields {:keys [id] :as prompt-field}]
  (let [{:keys [sval nval dval jval] :as resp-field} (some-> id fields first)
        resp-field' (merge resp-field
                           {:state (r/atom (or dval nval sval jval
                                               ""))})]
    (assoc prompt-field
           :response
           [resp-field'])))


(defn mk-form-doc-prompt-field-state
  [fields {:keys [id] :as prompt-field}]
  (let [resp-fields (mapv (fn [{:keys [sval nval dval jval] :as resp-field}]
                            (merge resp-field
                                   {:state (r/atom (or dval nval sval jval
                                                       ""))}))
                          (or (not-empty (fields id))
                              [{}]))]
    (assoc prompt-field
           :response
           ;; TODO sort resp-fields by idx??
           (r/atom resp-fields))))

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
  [{:keys [fname ftype fsubtype response] :as prompt-field}]
  (let [value& (some-> response first :state)
        {response-field-id :id prompt-field-id :pf-id} (first response)]
    [:> ui/FormField
     (when-not (= fname "value")
       [:label fname])
     [ui/input {:value @value&
                :on-change (fn [this]
                             (reset! value& (-> this .-target .-value)))
                :attrs {:data-prompt-field (str prompt-field)
                        :data-response-field-id (str "[" response-field-id "]")
                        :data-prompt-field-id (str "[" prompt-field-id "]")}}]]))

(defn c-prompt-field-textarea
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)
        {response-id :id prompt-field-id :pf-id} (first response)]
    [:> ui/FormField
     (when-not (= fname "value")
       [:label fname])
     [:textarea {:value @value&
                 :on-change (fn [this]
                              (reset! value& (-> this .-target .-value)))
                 :data-response-field-id response-id
                 :data-prompt-field-id prompt-field-id}]]))

(defn c-prompt-field-int
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)
        {response-id :id prompt-field-id :pf-id} (first response)]
    [:> ui/FormField
     (when-not (= fname "value")
       [:label fname])
     [ui/input {:value @value&
                :on-change (fn [this]
                             (reset! value& (-> this .-target .-value)))
                :attrs {:type "number"
                        :data-response-field-id response-id
                        :data-prompt-field-id prompt-field-id}}]]))

(defn c-prompt-field-enum
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)
        enum-vals (rf/subscribe [:docs/enums fsubtype])
        {response-id :id prompt-field-id :pf-id} (first response)]
    (fn [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
      [:> ui/FormField
       (when-not (= fname "value")
         [:label fname])
       [:> ui/Dropdown {:value @value&
                        :onChange #(reset! value& (.-value %2))
                        ;; :placeholder "Select Product"
                        :selection true
                        :options @enum-vals
                        :data-response-field-id response-id
                        :data-prompt-field-id prompt-field-id}]])))


(defn c-prompt-field-list
  [c-prompt-field-fn {:keys [fname ftype fsubtype response] :as prompt-field}]
  [:div "LIST"
   (for [{:keys [id] :as response-field} @response]
     ^{:key (str "resp-field" id)}
     [:> ui/FormGroup
      [c-prompt-field-fn (assoc prompt-field
                                :response [response-field])]
      [:> ui/Button {:color "red"
	             :on-click (fn [& _]
                                 (swap! response
                                        (partial remove #(-> % :id (= id)))))
                     :icon true}
       [:> ui/Icon {:name "remove"}]]])
   [:> ui/Button {:color "green"
	          :on-click (fn [& _]
                              (swap! response conj {:id (gensym "new-resp-field")
                                                    :state (r/atom "")}))
                  :icon true}
    [:> ui/Icon {:name "add"}]]])

(defn c-prompt-field [{:keys [idstr ftype fsubtype list?] :as field}]
  (let [c-prompt-field-fn (hooks/c-prompt-field idstr [ftype fsubtype] ftype :default)]
    (if list?
      (c-prompt-field-list c-prompt-field-fn field)
      [c-prompt-field-fn (update field
                                 :response deref)])))

(defn c-prompt-default
  [{:keys [prompt descr fields]}]
  [:div {:style {:margin "10px 0 40px 0"}}
   [:div {:style {:margin-bottom "5px"}}
    prompt
    (when descr
      [:> ui/Popup {:trigger (r/as-element [:> ui/Icon {:name "info circle"}])
                    :wide true}
       descr])]
   (for [{:keys [idstr ftype fsubtype] :as f} fields]
     ^{:key (str "field" (:id f))}
     [c-prompt-field f]
     #_[(hooks/c-prompt-field idstr [ftype fsubtype] ftype :default)
      f])])

(defn prep-form-doc
  [{:keys [id ftype fsubtype title product to-org to-user from-org from-user doc-id doc-title prompts] :as form-doc}]
  (assoc form-doc
         :doc-title (cond
                      (= ftype "preposal") (str "Preposal for "
                                                (:pname product)
                                                " [ "
                                                (:oname to-org)
                                                " => "
                                                (:oname from-org)
                                                " ]")
                      :else (str title " doc"))
         :doc-notes ""
         :doc-descr ""
         :doc-dtype ftype
         :doc-dsubtype fsubtype))

(defn c-form-maybe-doc
  [{:keys [id title product from-org from-user doc-id doc-title prompts] :as form-doc} & [{:keys [show-submit return-save-fn& c-wrapper]}]]
  (let [save-fn #(rf/dispatch [:save-form-doc
                               (prep-form-doc form-doc)])]
    (when return-save-fn&
      (reset! return-save-fn& save-fn))
    (conj (or c-wrapper
              [:> ui/Form {:as "div"
                           :style {:width 400
                                   :margin-bottom 50}}])
          [:div
	         (or doc-title title)
	         (when product
	           [:div.product-name (:pname product)])
	         (when from-org
	           [:div.org-name (:oname from-org)])
	         (when from-user
	           [:div.user-name (:uname from-user)])
	         (for [p (sort-by :sort prompts)]
	           ^{:key (str "prompt" (:id p))}
	           [(hooks/c-prompt :default) p])
	         (when show-submit
	           [:> ui/Button {:color "blue"
	                          :fluid true
	                          :on-click save-fn}
	            "Submit"])])))


(hooks/reg-hooks! hooks/c-prompt
                  {:default #'c-prompt-default})

(hooks/reg-hooks! hooks/c-prompt-field
                  {:default #'c-prompt-field-default
                   ["s" "multi"] #'c-prompt-field-textarea
                   ["n" "int"] #'c-prompt-field-int
                   "e" #'c-prompt-field-enum})


;; "data" can have term->field->value's
;;          and/or prompt-id->field->value's
;; E.g.,
;; {:terms {:product/goal {:value "We need everything."}
;;          :product/budget {:value 2400
;;                           :period "Annual"}
;;          :product/someterm {:value "Hello sir.\nWhat's up?"}}
;;  :prompt-ids {126786722 {:value "Justanother Value"}}}
(rf/reg-event-fx
 :save-doc ; one of ftype or update-doc-id are required
 (fn [{:keys [db]} [_ {:keys [dtype update-doc-id round-id return]} data]]
   {:ws-send
    {:payload
     (merge {:cmd :save-doc
             :return {:handler :save-doc-return}
             :data data
             :dtype dtype
             :update-doc-id update-doc-id
             :round-id round-id
             :from-org-id (util/db->current-org-id db)}
            (when return
              {:return return}))}}))

;; used as a default return handler for :save-doc if
;; caller of :save-doc doesn't define a return handler
(rf/reg-event-fx
 :save-doc-return
 (constantly
  {:toast {:type "success"
           :title "Form Saved"
           :message "Your form has been saved."}}))
