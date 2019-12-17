(ns vetd-app.docs
  (:require [vetd-app.util :as util]
            [vetd-app.ui :as ui]
            [vetd-app.hooks :as hooks]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

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

(rf/reg-sub
 :docs/entities
 (fn [[_ fsubtype]]
   (let [q
         (case fsubtype
           "i-category" [[:categories {:deleted nil}
                          [:id :cname]]])]
     (rf/subscribe [:gql/q {:queries q}])))
 (fn [r [_ fsubtype]]
   (case fsubtype
     "i-category" (->> r
                       :categories
                       (map (fn [{:keys [id cname]}]
                              {:value id
                               :text cname}))
                       (sort-by :text)
                       vec))))

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

(defn get-response-fields-by-prompt-id
  [response-prompts prompt-id]
  (->> response-prompts
       (filter #(-> % :prompt-id (= prompt-id)))
       first
       :response-prompt-fields))

(defn get-response-field-by-prompt-id
  [response-prompts prompt-id & [field]]
  (->> (get-response-fields-by-prompt-id response-prompts prompt-id)
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

(defn walk-prep-for-save-form-doc
  [frm]
  (clojure.walk/prewalk
   (fn [f]
     (cond (instance? reagent.ratom/RAtom f) @f
           (fn? f) nil
           :else f))
   frm))

(rf/reg-event-fx
 :save-form-doc
 (fn [_ [_ form-doc]]
   (let [fd (walk-prep-for-save-form-doc form-doc)]
     {:ws-send {:payload {:cmd :save-form-doc
                          :return {:handler :save-form-doc-return}
                          :form-doc fd}}})))

(rf/reg-event-fx
 :save-form-doc-return
 (fn []
   {:toast {:type "success"
            :title "Changes Saved"}}))

(rf/reg-event-fx
 :remove-prompt&response
 (fn [_ [_ prompt-id response-id form-id doc-id]]
   {:ws-send (remove nil?
                     [(when (and prompt-id form-id)
                        {:payload {:cmd :v/remove-prompt-from-form
                                   :return nil
                                   :prompt-id prompt-id
                                   :form-id form-id}})
                      (when (and response-id doc-id)
                        {:payload {:cmd :v/remove-response-from-doc
                                   :return nil
                                   :response-id response-id
                                   :doc-id doc-id}})])}))

(rf/reg-event-fx
 :propagate-prompt
 (fn [_ [_ form-prompt-ref-id target-form-id]]
   {:ws-send {:payload {:cmd :v/propagate-prompt
                        :return nil
                        :form-prompt-ref-id form-prompt-ref-id
                        :target-form-id target-form-id}}}))

(defn mk-form-doc-prompt-field-state*
  [{:keys [sval nval dval jval] :as resp-field}]
  (merge resp-field
         {:state (r/atom (or dval nval sval jval ""))}))

(defn mk-form-doc-prompt-field-state
  [prompt-field response-fields]
  (let [resp-fields (mapv mk-form-doc-prompt-field-state* response-fields)]
    (assoc prompt-field
           :response
           ;; TODO sort resp-fields by idx??
           (r/atom resp-fields))))

(defn mk-form-doc-prompt-field-states
  [prompt-fields response-fields-by-pf-id]
  (for [{:keys [id] :as prompt-field} prompt-fields]
    (mk-form-doc-prompt-field-state  prompt-field
                                     (or (not-empty (response-fields-by-pf-id id))
                                         [{}]))))

(defn mk-form-doc-prompt-state
  [{:keys [term id] :as prompt} response prompt-actions]
  (let [{:keys [fields notes]} response
        response' (merge response
                         {:notes-state (r/atom (or notes ""))})
        fields' (group-by :pf-id
                          fields)]
    (-> prompt
        (assoc :actions (when prompt-actions
                          (not-empty
                           (merge
                            (prompt-actions term)
                            (prompt-actions id)))))
        (assoc :response response')
        (update :fields
                mk-form-doc-prompt-field-states fields'))))

(defn mk-form-doc-prompt-states
  [prompts responses-by-prompt prompt-actions]
  (-> (for [{:keys [id term] :as prompt} prompts]
        (-> (for [response (responses-by-prompt id)]
              (mk-form-doc-prompt-state prompt
                                        response
                                        prompt-actions))
            not-empty
            (or (mk-form-doc-prompt-state prompt
                                          {}
                                          prompt-actions))))
      flatten))

(defn mk-form-doc-state
  [{:keys [responses] :as form-doc} & [prompt-actions]]
  (let [responses' (group-by :prompt-id
                             responses)]
    (update form-doc :prompts
            mk-form-doc-prompt-states responses' prompt-actions)))

(defn c-prompt-field-default
  [{:keys [fname ftype fsubtype response] :as prompt-field}]
  (let [value& (some-> response first :state)
        {response-field-id :id prompt-field-id :pf-id} (first response)]
    [:<>
     (when-not (= fname "value")
       [:div fname])
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
    [:<>
     (when-not (= fname "value")
       [:label fname])
     [:textarea {:value @value&
                 :on-change (fn [this]
                              (reset! value& (-> this .-target .-value)))
                 :data-prompt-field (str prompt-field)
                 :data-response-field-id response-id
                 :data-prompt-field-id prompt-field-id}]]))

(defn c-prompt-field-int
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)
        {response-id :id prompt-field-id :pf-id} (first response)]
    [:<>
     (when-not (= fname "value")
       [:label fname])
     [ui/input {:value @value&
                :on-change (fn [this]
                             (reset! value& (-> this .-target .-value)))
                :attrs {:type "number"
                        :data-prompt-field (str prompt-field)
                        :data-response-field-id response-id
                        :data-prompt-field-id prompt-field-id}}]]))

(defn c-prompt-field-enum
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)
        enum-vals (rf/subscribe [:docs/enums fsubtype])
        {response-id :id prompt-field-id :pf-id} (first response)]
    (fn [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
      [:<>
       (when-not (= fname "value")
         [:label fname])
       [:> ui/Dropdown {:value @value&
                        :onChange #(reset! value& (.-value %2))
                        :selection true
                        :options (cons {:key "nil"
                                        :text " - - - "
                                        :value nil}
                                       @enum-vals)
                        :data-prompt-field (str prompt-field)
                        :data-response-field-id response-id
                        :data-prompt-field-id prompt-field-id}]])))


(defn c-prompt-field-entity
  [{:keys [fname fsubtype response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [state& (some-> response first :state)
        _ (when-not (map? @state&) (reset! state& {:id nil :text ""})) ;; HACK
        opts& (rf/subscribe [:docs/entities fsubtype])
        {response-id :id prompt-field-id :pf-id} (first response)]
    (fn [{:keys [fname fsubtype response] :as prompt-field}]
      [:<>
       [:span {:data-prompt-field (str prompt-field)
               :data-response-field-id response-id
               :data-prompt-field-id prompt-field-id}]
       (when-not (= fname "value")
         [:label fname])
       [:> ui/Dropdown {:value (:id @state&)
                        :onChange #(reset! state& {:id (.-value %2)
                                                   :text
                                                   (ui/get-text-from-opt-by-value (.-options %2)
                                                                                  (.-value %2))})
                        :onSearchChange #(swap! state&
                                                assoc :text (aget %2 "searchQuery"))
                        :selection true
                        :search true
                        :searchQuery (:text @state&)
                        :selectOnNavigation false
                        :text (:text @state&)
                        :fluid true
                        :options @opts&}]])))

(defn c-prompt-field-list
  [c-prompt-field-fn {:keys [fname ftype fsubtype response] :as prompt-field}]
  (println c-prompt-field-fn)
  (println prompt-field)
  [:<>
   (for [{:keys [id] :as response-field} @response]
     ^{:key (str "resp-field" (or id (hash response-field)))}
     [:> ui/Grid {:style {:margin-top 0
                          :margin-bottom 3}}
      [:> ui/GridRow {:style {:padding-top 0}}
       [:> ui/GridColumn {:width 12}
        [c-prompt-field-fn (assoc prompt-field :response [response-field])]]
       [:> ui/GridColumn {:width 4}
        [:> ui/Label {:as "a"
                      :on-click (fn [& _]
                                  (swap! response
                                         (partial remove #(-> % :id (= id)))))
                      ;; :color "red"
                      :style {:float "right"
                              :margin-top 5}}
         [:> ui/Icon {:name "remove"}]
         "Remove"]]]])
   [:> ui/Label {:as "a"
                 :on-click (fn [& _]
                             (swap! response conj {:id (gensym "new-resp-field")
                                                   :state (r/atom "")}))
                 :color "teal"
                 :style {:margin-bottom 5}}
    [:> ui/Icon {:name "add"}]
    "Add New"]])

(defn c-prompt-field [{:keys [idstr ftype fsubtype list?] :as field}]
  (let [c-prompt-field-fn (hooks/c-prompt-field idstr [ftype fsubtype] ftype :default)]
    (if list?
      (c-prompt-field-list c-prompt-field-fn field)
      [c-prompt-field-fn (update field
                                 :response deref)])))

(defn c-prompt-default [{:keys [id prompt descr fields response actions] :as p} form-id doc-id]
  (let [on-load-fns (atom [])]
    (r/create-class
     {:reagent-render
      (fn [{:keys [id prompt descr fields response actions] :as p} form-id doc-id]
        [:> ui/FormField
         [:label prompt
          (when-not (s/blank? descr)
            [:> ui/Popup {:trigger (r/as-element [:> ui/Icon {:name "info circle"
                                                              :style {:margin-left "5px"}}])
                          :wide true}
             descr])]
         (for [[k f] actions]
           (if (= k :-on-load)
             (do
               (swap! on-load-fns conj (partial f p))
               nil)
             (do
               ^{:key (str "prompt-action" id)}
               [:a {:on-click (partial f p)
                    :style {:margin-left "10px"}} k])))
         (for [{:keys [idstr ftype fsubtype] :as f} fields]
           ^{:key (str "field" (:id f))}
           [c-prompt-field f])])
      :component-did-mount
      (fn [& args]
        (let [fns @on-load-fns]
          (reset! on-load-fns nil)
          (doseq [f fns]
            (try
              (f)
              (catch js/Error e
                (.log js/console e))))))})))

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
  [{:keys [id title product from-org from-user
           doc-id doc-title prompts]
    :as form-doc}
   & [{:keys [show-submit return-save-fn& c-wrapper]}]]
  (let [save-fn #(rf/dispatch [:save-form-doc (prep-form-doc form-doc)])]
    (when return-save-fn&
      (reset! return-save-fn& save-fn))
    (conj (or c-wrapper
              [:> ui/Form {:as "div"
                           :style {:width "100%"
                                   :padding-bottom 14}}])
          [:<>
	         (for [p (sort-by :sort prompts)]
	           ^{:key (str "prompt" (:id p))}
	           [(hooks/c-prompt :default) p id doc-id])
	         (when show-submit
	           [:> ui/Button {:color "blue"
	                          :fluid true
	                          :on-click save-fn}
	            "Save Changes"])])))

(hooks/reg-hooks! hooks/c-prompt
                  {:default #'c-prompt-default})

(hooks/reg-hooks! hooks/c-prompt-field
                  {:default #'c-prompt-field-default
                   ["s" "multi"] #'c-prompt-field-textarea
                   ["n" "int"] #'c-prompt-field-int
                   "e" #'c-prompt-field-enum
                   "i" #'c-prompt-field-entity})


(defn c-missing-prompts
  [{prompts1 :prompts
    :as _prod-prof-form}
   {prompts2 :prompts
    :keys [id]
    :as _form-doc}]
  (let [missing-prompt-ids (clojure.set/difference (->> prompts1 (mapv :id) set)
                                                   (->> prompts2 (mapv :id) set))]
    (when-not (empty? missing-prompt-ids)
      [:div {:style {:margin "20px"
                     :padding "10px"
                     :width "600px"
                     :border "solid 1px green"}}
       "Prompts available for propagation from template:"
       (for [{:keys [prompt ref-id]} (->> prompts1
                                          (filter #(-> % :id missing-prompt-ids))
                                          (sort-by :sort))]
         ^{:key (str "template-prompt" ref-id)}
         [:div {:style {:margin "10px"}} prompt
          [:a {:style {:margin-left "10px"}
               :on-click #(rf/dispatch [:propagate-prompt ref-id id])}
           "add"]])])))

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
