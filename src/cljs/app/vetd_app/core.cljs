(ns vetd-app.core
  (:require [vetd-app.hooks :as hooks]
            [vetd-app.websockets :as ws]
            [vetd-app.pages.home :as p-home]
            [vetd-app.pages.buyers.b-search :as p-b-search]
            [vetd-app.pages.buyers.b-home :as p-bhome]
            [vetd-app.pages.buyers.b-preposals :as p-bpreposals]
            [vetd-app.pages.buyers.b-preposal-detail :as p-bpreposal-detail]
            [vetd-app.pages.vendors.v-home :as p-vhome]
            [vetd-app.pages.signup :as p-signup]
            [vetd-app.pages.login :as p-login]
            [vetd-app.buyer-fixtures :as b-fix]
            [vetd-app.vendor-fixtures :as v-fix]
            [vetd-app.public-fixtures :as pub-fix]            
            vetd-app.local-store
            vetd-app.cookies
            vetd-app.graphql
            [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as sec]
            [accountant.core :as acct]))

(println "START core")

(hooks/reg-hooks! hooks/c-page
                  {:home #'p-home/home-page
                   :pub/signup #'p-signup/signup-page
                   :pub/login #'p-login/login-page
                   :b/home #'p-bhome/c-page
                   :b/search #'p-b-search/c-page
                   :b/preposals #'p-bpreposals/c-page
                   :b/preposal-detail #'p-bpreposal-detail/c-page   
                   :v/home #'p-vhome/c-page})

(hooks/reg-hooks! hooks/c-container
                  {:pub/login #'pub-fix/container
                   :pub/signup #'pub-fix/container
                   :b/home #'b-fix/container
                   :b/search #'b-fix/container
                   :b/preposals #'b-fix/container
                   :v/home #'v-fix/container})


(rf/reg-event-db
 :init-db
 (fn [] {}))

(rf/reg-event-db
 :route-home
 (fn [db [_ query-params]]
   (assoc db
          :page :home
          :search-query (:q query-params "") ;; to make :default-value work
          :query-params query-params)))

(def public-pages #{:pub/login :pub/signup})

(rf/reg-sub
 :page
 (fn [{:keys [logged-in? user page]} _]
   (if (or (and logged-in? user)
           (public-pages page))
     page
     :pub/login)))

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
 (fn [{:keys [id]}] id))

(rf/reg-sub
 :user-name
 :<- [:user] 
 (fn [{:keys [uname]}] uname))



(rf/reg-fx
 :nav
 (fn nav-fx [{:keys [path query]}]
   (acct/navigate! path query)))



(defn ->home-url
  [membs admin?]
  (if admin?
    "/a/home/"
    (if-let [{{:keys [id buyer? vendor?]} :org} (first membs)]
      (if buyer?
        "/b/preposals/"
        "/v/home/")
      "/")))

(rf/reg-event-fx
 :nav-home
 (fn [{:keys [db]} _]
   (let [{:keys [page memberships admin?]} db]
     {:nav {:path (->home-url memberships admin?)}})))

(rf/reg-event-fx
 :nav-if-public
 (fn [{:keys [db]} _]
   (let [{:keys [page memberships admin?]} db]
     (if ((conj public-pages :home) page)
       {:nav {:path (->home-url memberships admin?)}}
       {}))))

(rf/reg-event-fx
 :ws-get-session-user
 [(rf/inject-cofx :local-store [:session-token])] 
 (fn [{:keys [db local-store]} [_ [email pwd]]]
   {:ws-send {:payload {:cmd :auth-by-session
                        :return :ws/req-session
                        :session-token (:session-token local-store)}}}))

(rf/reg-event-fx
 :ws/req-session
 [(rf/inject-cofx :local-store [:session-token])]  
 (fn [{:keys [db local-store]} [_ {:keys [logged-in? user memberships admin?]}]]
   (if logged-in?
     {:db (assoc db
                 :user user
                 :logged-in? logged-in?
                 :memberships memberships
                 :admin? admin?
                 ;; TODO support users with multi-orgs                 
                 :active-memb-id (some-> memberships first :id))
      :cookies {:admin-token (when admin?
                               [(:session-token local-store)
                                {:max-age 3600 :path "/"}])}
      :dispatch-later [{:ms 100 :dispatch [:nav-if-public]}]}
     {:db (dissoc db :user)
      :dispatch [:pub/nav-login]})))

(defn c-page []
  (let [page @(rf/subscribe [:page])]
    [:div#page
     [(hooks/c-container :admin-overlay)
      [(hooks/c-admin page)]]
     [(hooks/c-container page)
      [(hooks/c-page page)]]]))

(defn mount-components []
  (.log js/console "mount-components STARTED")
  (rf/clear-subscription-cache!)
  (r/render [#'c-page] (.getElementById js/document "app"))
  (.log js/console "mount-components DONE"))

;; -------------------------
;; Routes

(sec/defroute home-path "/" [query-params]
  (do #_(.log js/console "nav home")
      (rf/dispatch [:route-home query-params])))

(sec/defroute buyers-search "/b/search/" [query-params]
  (rf/dispatch [:b/route-search query-params]))

(sec/defroute buyers-home "/b/home/" [query-params]
  (rf/dispatch [:b/route-home query-params]))

(sec/defroute buyers-preposals "/b/preposals/" [query-params]
  (rf/dispatch [:b/route-preposals query-params]))

(sec/defroute buyers-preposal-detail "/b/preposals/:id" [id]
  (rf/dispatch [:b/route-preposal-detail id]))

(sec/defroute vendors-home "/v/home/" [query-params]
  (do (.log js/console "nav vendors")
      (rf/dispatch [:v/route-home query-params])))

(sec/defroute login-path "/login" [query-params]
  (do #_(.log js/console "nav home")
      (rf/dispatch [:pub/route-login query-params])))

(sec/defroute signup-path "/signup" [query-params]
  (do #_(.log js/console "nav home")
      (rf/dispatch [:pub/route-signup query-params])))

(sec/defroute catchall-path "*" []
  (do (.log js/console "nav catchall")
      (rf/dispatch [:apply-route nil])))

(defn config-acct []
  (acct/configure-navigation! {:nav-handler sec/dispatch!
                               :path-exists? sec/locate-route
                               :reload-same-path? false}))


(defonce init-done? (volatile! false))

(defn init! []
  (if @init-done?
    (println "init! SKIPPED")
    (do
      (println "init! START")
      (vreset! init-done? true)
      (rf/dispatch-sync [:init-db])
      (rf/dispatch-sync [:ws-init])
      (config-acct)
      (acct/dispatch-current!)
      (rf/dispatch-sync [:ws-get-session-user])
      (mount-components)
      (println "init! END"))))

;; for dev
(defn re-init! []
  (vreset! init-done? false)
  (init!))

(println "END core")
