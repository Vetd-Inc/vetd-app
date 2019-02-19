(ns vetd-app.docs
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
            [vetd-app.hooks :as hks]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))


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
   (def fd1 (walk-deref-ratoms form-doc))
#_   (cljs.pprint/pprint fd1)
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
                        (ut/fmap first))]
    (update form-doc
            :prompts
            (partial mapv
                     (partial mk-form-doc-prompt-state
                              responses')))))

(defn c-prompt-field-default
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (def pf1 prompt-field)
  (let [value& (some-> response first :state)]
    [:div
     (when-not (= fname "value")
       fname)
     [rc/input-text
      :model value&
      :on-change #(reset! value& %)]]))

(defn c-prompt-field-textarea
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)]
    [:div
     (when-not (= fname "value")
       fname)     
     [rc/input-textarea
      :model value&
      :on-change #(reset! value& %)]]))

(defn c-prompt-field-int
  [{:keys [fname ftype fsubtype list? response] :as prompt-field}]
  ;; TODO support multiple response fields (for where list? = true)
  (let [value& (some-> response first :state)]
    [:div
     (when-not (= fname "value")
       fname)
     [rc/input-text
      :model value&
      :on-change #(reset! value& %)
      :validation-regex #"^\d*$"]]))

(defn c-prompt-default
  [{:keys [prompt descr fields]}]
  [flx/col #{:preposal}
   [:div prompt]
   [:div descr]   
   (for [{:keys [idstr ftype fsubtype] :as f} fields]
     ^{:key (str "field" (:id f))}
     [(hks/c-prompt-field idstr [ftype fsubtype] ftype :default)
      f])])

(defn prep-preposal-form-doc
  [{:keys [id title product to-org to-user from-org from-user doc-id doc-title prompts] :as form-doc}]
  (assoc form-doc
         :doc-title (str "Preposal for "
                         (:pname product)
                         " [ "
                         (:oname from-org)
                         " => "
                         (:oname to-org)
                         " ]")
         :doc-notes ""
         :doc-descr ""
         :doc-dtype "preposal"
         :doc-dsubtype "preposal1"))



(defn c-form-maybe-doc
  [{:keys [id title product from-org from-user doc-id doc-title prompts] :as form-doc}]
  [flx/col #{:preposal}
   [:div (or doc-title title)]
   [flx/row
    [:div (:pname product)]
    [:div (:oname from-org)]
    [:div (:uname from-user)]]
   (for [p prompts]
     ^{:key (str "prompt" (:id p))}
     [(hks/c-prompt :default) p])
   [rc/button
    :label "Submit"
    :on-click #(rf/dispatch [:save-form-doc
                             (prep-preposal-form-doc form-doc)])]])


(hks/reg-hooks! hks/c-prompt
                {:default #'c-prompt-default})

(hks/reg-hooks! hks/c-prompt-field
                {:default #'c-prompt-field-default
                 ["s" "multi"] #'c-prompt-field-textarea
                 ["n" "int"] #'c-prompt-field-int})





#_(defn new-preposal [{:keys [id title product from-org from-user docs] :as preq}]
  (let [pitch& (r/atom {})
        org-user& (r/atom nil)
        pitch& (r/atom "")
        price-val& (r/atom "")
        price-unit& (r/atom :year)
        orgs& (rf/subscribe [:gql/q
                             {:queries
                              [[:orgs {:buyer? true}
                                [:id :oname :idstr :short-desc
                                 [:memberships
                                  [:id
                                   [:user
                                    [:id :idstr :uname]]]]]]]}])]
    (fn []
      [:div "NEW PREPOSAL"
       [:div "Pitch"
        [rc/input-textarea
         :model pitch&
         :on-change #(reset! pitch& %)]]
       "Price Estimate "
       [rc/input-text
        :model price-val&
        :on-change #(reset! price-val& %)
        :validation-regex #"^\d*$"]
       " per "
       [rc/single-dropdown
        :model price-unit&
        :on-change #(reset! price-unit& %)
        :choices [{:id :year :label "year"}
                  {:id :month :label "month"}]]
       [rc/button
        :label "Save"
        :on-click (fn []
                    (let [{:keys [org-id user-id]} @org-user&]
                      (rf/dispatch [:v/create-preposal {:buyer-org-id org-id
                                                        :buyer-user-id user-id
                                                        :pitch @pitch&
                                                        :price-val @price-val&
                                                        :price-unit @price-unit&}])))]])))
