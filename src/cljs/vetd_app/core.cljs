(ns vetd-app.core
  (:require [vetd-app.util :as ut]   
            [vetd-app.websockets :as ws]
            [vetd-app.graphql :as graphql]            
            [vetd-app.db-plus :as db+]
            [vetd-app.nav :as nav]
            [vetd-app.blocker :as bl]                        
            [vetd-app.pages.home :as p-home]
            [vetd-app.pages.buyers.b-search :as p-b-search]
            [vetd-app.pages.buyers.b-home :as p-bhome]
            [vetd-app.pages.vendors :as p-vendors]            
            [vetd-app.pages.signup :as p-signup]
            [vetd-app.pages.login :as p-login]
            [vetd-app.buyer-fixtures :as b-fix]
            [vetd-app.vendor-fixtures :as v-fix]
            [vetd-app.public-fixtures :as pub-fix]            
            vetd-app.localstore
            [reagent.core :as r]
            [re-frame.core :as rf]
            [goog.events :as events]
            [secretary.core :as sec]
            [accountant.core :as acct]
            [cognitect.transit :as t]))

(println "START core")

(defonce init-done? (volatile! false))

(rf/reg-event-db
 :init-db
 (fn [_ _]
   {}))

(rf/reg-event-db
 :route-home
 (fn [db [_ query-params]]
   (assoc db
          :page :home
          :search-query (:q query-params "") ;; to make :default-value work
          :query-params query-params)))

(def public-pages #{:login :signup})

(rf/reg-sub
 :page
 (fn [{:keys [logged-in? user page]} _]
   (if (or (and logged-in? user)
           (public-pages page))
     page
     :login)))

(rf/reg-sub
 :active-org
 (fn [{:keys [active-memb-id memberships]} _]
   (->> memberships
        (filter #(-> % :id (= active-memb-id)))
        first
        :org)))

(rf/reg-sub
 :org-id
 :<- [:active-org] 
 (fn [{:keys [id]}] id))

(rf/reg-sub
 :org-name
 :<- [:active-org] 
 (fn [{:keys [oname]}] oname))

(rf/reg-sub
 :user
 (fn [{:keys [user]}] user))

(rf/reg-sub
 :user-id
 :<- [:user] 
 (fn [{:keys [user-id]}] user-id))

(rf/reg-sub
 :user-name
 :<- [:user] 
 (fn [{:keys [uname]}] uname))



(rf/reg-fx
 :nav
 (fn nav-fx [{:keys [path query]}]
   (acct/navigate! path query)))

(defn memberships->home-url
  [membs]
  (if-let [{{:keys [id buyer? vendor?]} :org} (first membs)]
    (str (if buyer?
           "/b/home"
           "/v/home")
         "/")
    "/"))

;; TODO rename
(rf/reg-event-fx
 :nav-home
 (fn [{:keys [db]} _]
   {:nav {:path (-> db :memberships memberships->home-url)}}))

(rf/reg-event-fx
 :nav-if-public
 (fn [{:keys [db]} _]
   (if ((conj public-pages :home) (:page db))
     {:nav {:path (-> db :memberships memberships->home-url)}}
     {})))

(rf/reg-event-fx
 :ws-get-session-user
 [(rf/inject-cofx :local-store [:session-token])] 
 (fn [{:keys [db local-store]} [_ [email pwd]]]
   {:ws-send {:ws (:ws db)
              :payload {:cmd :auth-by-session
                        :return :ws/req-session
                        :session-token (:session-token local-store)}}}))

(rf/reg-event-fx
 :ws/req-session
 (fn [{:keys [db]} [_ {:keys [logged-in? user memberships]}]]
   (if logged-in?
     {:db (assoc db
                 :user user
                 :logged-in? logged-in?
                 :memberships memberships
                 ;; TODO support users with multi-orgs                 
                 :active-memb-id (some-> memberships first :id))
      :dispatch-later [{:ms 100 :dispatch [:nav-if-public]}]}
     {:db (dissoc db :user)
      :dispatch [:nav-login]})))

(def pages
  {:home #'p-home/home-page
   :signup #'p-signup/signup-page
   :login #'p-login/login-page
   :b-home #'p-bhome/c-page
   :b-search #'p-b-search/c-page   
   :vendors #'p-vendors/vendors-page})


(def containers
  {:b-home #'b-fix/container
   :b-search #'b-fix/container   
   :login #'pub-fix/container
   :signup #'pub-fix/container})

#_(defn page []
  [:div#page
   [(headers @(rf/subscribe [:page])
             (constantly [:div]))]
   [(pages @(rf/subscribe [:page])
           (constantly [:div "none"]))]
   #_[bl/blocker]])

(defn page []
  (let [page @(rf/subscribe [:page])]
    [:div#page
     [(containers page (constantly [:div "no container"]))
      [(pages page  (constantly [:div "none"]))]]
     #_[bl/blocker]]))

(defn mount-components []
  (.log js/console "mount-components STARTED")
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app"))
  (.log js/console "mount-components DONE"))

;; -------------------------
;; Routes

(sec/defroute home-path "/" [query-params]
  (do #_(.log js/console "nav home")
      (rf/dispatch [:route-home query-params])))

(sec/defroute buyers-search "/b/search/" [query-params]
  (rf/dispatch [:route-b-search query-params]))

(sec/defroute buyers-home "/b/home/" [query-params]
  (rf/dispatch [:route-b-home query-params]))

(sec/defroute vendors-home "/v/home/" [query-params]
  (do (.log js/console "nav vendors")
      (rf/dispatch [:route-v-home query-params])))

(sec/defroute login-path "/login" [query-params]
  (do #_(.log js/console "nav home")
      (rf/dispatch [:route-login query-params])))

(sec/defroute signup-path "/signup" [query-params]
  (do #_(.log js/console "nav home")
      (rf/dispatch [:route-signup query-params])))

(sec/defroute catchall-path "*" []
  (do (.log js/console "nav catchall")
      (rf/dispatch [:apply-route nil])))

(defn config-acct []
  (acct/configure-navigation! {:nav-handler sec/dispatch!
                               :path-exists? sec/locate-route
                               :reload-same-path? false}))

(defn init! []
  (if @init-done?
    (println "init! SKIPPED")
    (do
      (println "init! START")
      (vreset! init-done? true)
      (rf/dispatch-sync [:init-db])
      #_      (db+/reset-db!)
      (rf/dispatch-sync [:ws-init])
      (config-acct)
      (acct/dispatch-current!)
      (rf/dispatch-sync [:ws-get-session-user])
      (mount-components)
      (println "init! END"))))

#_ (init!)

(defn re-init! []
  (vreset! init-done? false)
  (init!))

(println "END core")
