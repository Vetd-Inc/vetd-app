(ns vetd-app.pages.vendors
  (:require [vetd-app.util :as ut]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

(rf/reg-sub
 :preposal-reqs
  (fn [db _]
    (:preposal-reqs db)))

(rf/reg-sub
 :preposals  ;; TODO => preposals-out
  (fn [db _]
    (:preposals db)))

(rf/reg-sub
 :vendor-profile
  (fn [db _]
    (:vendor-profile db)))

(rf/reg-event-fx
 :route-v-home
 (fn [{:keys [db]} [_ id query-params]]
   (let [{:keys [org-id]} db]
     {:db (assoc db
                 :page :vendors
                 :org-id id
                 :query-params query-params)
      :ws-send  (if org-id
                  [{:payload {:cmd :path
                              :return :preposal-reqs
                              :q [:preposal_reqs
                                  [:id :created
                                   {:buyers [:org-name]}]
                                  {:vendor-id org-id}]}}
                   {:payload {:cmd :path
                              :return :preposals
                              :q [:preposals
                                  [:id :pitch :created
                                   {:buyers [:org-name]}]
                                  {:vendor-id org-id}]}}
                   {:payload {:cmd :path
                              :return :profile
                              :q [:profiles
                                  [:short-desc :long-desc]
                                  {:vendor-id org-id}]}}]
                  [])})))

(rf/reg-event-fx
 :ws/preposal-reqs
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   {:db (assoc db
               :preposal-reqs
               results)}))

(rf/reg-event-fx
 :ws/preposals
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   {:db (assoc db
               :preposals
               results)}))

(rf/reg-event-fx
 :ws/profile
 (fn [{:keys [db]} [_ results {:keys [return]}]]
   {:db (assoc db
               :vendor-profile
               results)}))

(rf/reg-event-fx
 :send-preposal
 (fn [{:keys [db]} [_ id txt]]
   {:ws-send {:payload {:cmd :send-preposal
                        :return :send-preposal
                        :prep-req-id id
                        :txt txt}}}))

(rf/reg-event-fx
 :save-profile
 (fn [{:keys [db]} [_ txt]]
   {:ws-send {:payload {:cmd :save-profile
                        :return :save-profile
                        :vendor-id  (:org-id db)
                        :long-desc txt}}}))

(defn prep-req [{:keys [id created buyers]}]
  (let [{:keys [org-name]} buyers
        txt (r/atom "")]
    (fn []
      [:div
       [:span org-name]
       " "
       [:span (str created)]
       [:div
        [rc/input-textarea
         :model txt
         :on-change #(reset! txt %)]]
       [rc/button
        :label "Send Preposal"
        :on-click #(rf/dispatch [:send-preposal id @txt])]])))

(defn preposal [{:keys [buyers created pitch]}]
  (fn [{:keys [buyers created pitch]}]
    (let [{:keys [org-name]} buyers]
      [:div
       [:span org-name]
       " "
       [:span (str created)]
       [:div pitch]])))

(defn profile [{:keys [short-desc long-desc]}]
  (let [txt (r/atom (or long-desc ""))]
    (fn []
      [:div
       [:div
        [rc/input-textarea
         :model txt
         :on-change #(reset! txt %)]]
       [rc/button
        :label "Save Profile"
        :on-click #(rf/dispatch [:save-profile @txt])]])))

(defn vendors-page []
  (let [prqs @(rf/subscribe [:preposal-reqs])
        prs @(rf/subscribe [:preposals])
        vp @(rf/subscribe [:vendor-profile])]
    [:div
     [:div
      "Preposals"
      (for [pr prs]
        ^{:key (:id pr)} [preposal pr])]     
     [:div
      "Preposal Requests"
      (for [prq prqs]
        ^{:key (:id prq)} [prep-req prq])]
     [:div
      "Vendor Profile "
      [profile vp]]]))
