(ns vetd-app.pages.vendors.v-home
  (:require [vetd-app.util :as ut]
            [vetd-app.flexer :as flx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

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

(rf/reg-event-fx
 :v/nav-home
 (fn [{:keys [db]} _]
   {:nav {:path "/v/home/"}}))

(rf/reg-event-db
 :v/route-home
 (fn [db [_ query-params]]
   (assoc db
          :page :v/home
          :query-params query-params)))

(rf/reg-event-fx
 :v/create-preposal
 (fn [{:keys [db]} prep-def]
   {:ws-send {:payload (merge {:cmd :v/create-preposal
                               :return nil}
                              prep-def)}}))


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
 (fn [{:keys [db]} form-doc]
   (def fd1 (walk-deref-ratoms form-doc))
   (cljs.pprint/pprint (walk-deref-ratoms form-doc))
#_   {:ws-send {:payload {:cmd :save-form-doc
                        :return nil
                        :form-doc (walk-deref-ratoms form-doc)}}}))



(defn mk-form-doc-prompt-field-state
  [field]
  (assoc field
         :response
         {:state (r/atom "default value????")}))

(defn mk-form-doc-prompt-state
  [prompt]
  (-> prompt
      (assoc :response {:note-state (r/atom "")})
      (update :fields
              (partial mapv mk-form-doc-prompt-field-state))))

(defn mk-form-doc-state
  [form-doc]
  (update form-doc
          :prompts
          (partial mapv mk-form-doc-prompt-state)))



(defn c-prompt-field
  [{:keys [fname ftype fsubtype list? response]}]
  (let [value& (:state response)]
    [:div
     fname
     [rc/input-text
      :model value&
      :on-change #(reset! value& %)]]))

(defn c-prompt
  [{:keys [prompt descr fields]}]
  [flx/col #{:preposal}
   [:div prompt]
   [:div descr]   
   (for [f fields]
     ^{:key (str "field" (:id f))}
     [c-prompt-field f])])

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
     [c-prompt p])
   [rc/button
    :label "Submit"
    :on-click #(rf/dispatch [:save-form-doc
                             (prep-preposal-form-doc form-doc)])]])

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        prep-reqs& (rf/subscribe [:gql/sub
                                  {:queries
                                   [[:form-docs {:ftype "preposal"
                                                 :to-org-id @org-id&}
                                     [:id :title
                                      :doc-id :doc-title
                                      [:product [:id :pname]]
                                      [:from-org [:id :oname]]
                                      [:from-user [:id :uname]]
                                      [:to-org [:id :oname]]
                                      [:to-user [:id :uname]]
                                      [:prompts
                                       [:id :prompt :descr
                                        [:fields
                                         [:id :fname :ftype :fsubtype :list?]]]]
                                      [:responses
                                       [:id :prompt-id]]]]]}])]
    (fn []
      (def preq1 @prep-reqs&)
      [:div
       (for [preq (:form-docs @prep-reqs&)]
         ^{:key (str "form" (:id preq))}
         [c-form-maybe-doc (mk-form-doc-state preq)])])))

#_ (cljs.pprint/pprint preq1)
