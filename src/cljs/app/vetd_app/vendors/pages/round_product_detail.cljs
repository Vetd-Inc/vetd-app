(ns vetd-app.vendors.pages.round-product-detail
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.common.components :as cc]
            [vetd-app.docs :as docs]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as rc]))


(rf/reg-event-fx
 :v/nav-round-product-detail
 (fn [_ [_ round-idstr product-idstr]]
   {:nav {:path (str "/v/rounds/" round-idstr
                     "/products/" product-idstr)}}))

(rf/reg-event-fx
 :v/route-round-product-detail
 (fn [{:keys [db]} [_ round-idstr product-idstr]]
   {:db (assoc db
               :page :v/round-product-detail
               :page-params {:round-idstr round-idstr
                             :product-idstr product-idstr})}))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        round-idstr& (rf/subscribe [:round-idstr])
        product-idstr& (rf/subscribe [:product-idstr])        
        round-product& (rf/subscribe [:gql/sub
                                      {:queries
                                       [[:round-product {:round-id (util/base31->num @round-idstr&)
                                                         :product-id (util/base31->num @product-idstr&)}
                                         [[:vendor-response-form-docs
                                           [:id :title :doc-id :doc-title
                                            :ftype :fsubtype
                                            [:doc-from-org [:id :oname]]
                                            [:doc-to-org [:id :oname]]
                                            [:prompts {:ref-deleted nil
                                                       :_order_by {:sort :asc}}
                                             [:id :idstr :prompt :descr #_:sort ;; TODO sort
                                              [:fields {:deleted nil
                                                        :_order_by {:sort :asc}}
                                               [:id :idstr :fname :ftype
                                                :fsubtype :list? #_:sort]]]]
                                            [:responses
                                             {:ref-deleted nil}
                                             [:id :prompt-id :notes
                                              [:fields {:deleted nil}
                                               [:id :pf-id :idx :sval :nval :dval :jval]]]]]]]]]}])]
    (fn []
      (if (= :loading @round-product&)
        [cc/c-loader]
        [:div
         (let [form-doc (-> @round-product& :round-product first :vendor-response-form-docs first)]
           ^{:key (str "form-doc" (:id form-doc))}
           [docs/c-form-maybe-doc
            (docs/mk-form-doc-state form-doc)
            {:show-submit true}])]))))
