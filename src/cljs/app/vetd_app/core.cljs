(ns vetd-app.core
  (:require vetd-app.websockets
            vetd-app.graphql
            vetd-app.local-store
            vetd-app.cookies
            vetd-app.analytics
            vetd-app.debounce
            vetd-app.signup
            [vetd-app.hooks :as hooks]
            [vetd-app.buyers.fixtures :as b-fix]
            [vetd-app.buyers.pages.signup :as p-bsignup]
            [vetd-app.buyers.pages.search :as p-bsearch]
            [vetd-app.buyers.pages.preposals :as p-bpreposals]
            [vetd-app.buyers.pages.preposal-detail :as p-bpreposal-detail]
            [vetd-app.buyers.pages.product-detail :as p-bproduct-detail]
            [vetd-app.vendors.fixtures :as v-fix]
            [vetd-app.vendors.pages.signup :as p-vsignup]
            [vetd-app.vendors.pages.home :as p-vhome]
            [vetd-app.vendors.pages.products :as p-vprods]
            [vetd-app.common.fixtures :as pub-fix]
            [vetd-app.common.pages.login :as p-login]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as sec]
            [accountant.core :as acct]
            [clerk.core :as clerk]))

(println "START core")

(hooks/reg-hooks! hooks/c-page
                  {:login #'p-login/login-page
                   :b/signup #'p-bsignup/c-page
                   :b/search #'p-bsearch/c-page
                   :b/preposals #'p-bpreposals/c-page
                   :b/preposal-detail #'p-bpreposal-detail/c-page
                   :b/product-detail #'p-bproduct-detail/c-page
                   :v/signup #'p-vsignup/c-page
                   :v/home #'p-vhome/c-page
                   :v/products #'p-vprods/c-page})

(hooks/reg-hooks! hooks/c-container
                  {:login #'pub-fix/container
                   :b/signup #'pub-fix/container
                   :b/search #'b-fix/container
                   :b/preposals #'b-fix/container
                   :b/preposal-detail #'b-fix/container
                   :b/product-detail #'b-fix/container
                   :v/signup #'pub-fix/container
                   :v/home #'v-fix/container
                   :v/products #'v-fix/container})


(rf/reg-event-db
 :init-db
 (constantly
  {:preposals-filter {:selected-categories #{}}}))

(def public-pages #{:login :b/signup :v/signup})

(rf/reg-sub
 :page
 (fn [{:keys [page]}] page))

(rf/reg-sub
 :page-params
 (fn [{:keys [page-params]}] page-params))

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
    "/a/search/"
    (if-let [active-memb (first membs)]
      (if (-> active-memb :org :buyer?)
        "/b/preposals/"
        "/v/home/")
      "/login")))

(rf/reg-event-fx
 :nav-home
 (fn [{:keys [db]} _]
   (let [{:keys [memberships admin?]} db]
     {:nav {:path (->home-url memberships admin?)}})))

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


;;;; Routes
(sec/defroute home-path "/" []
  (rf/dispatch [:nav-home]))

(sec/defroute buyers-search-root "/b/search/" []
  (rf/dispatch [:b/route-search]))

(sec/defroute buyers-search "/b/search/:search-term" [search-term]
  (rf/dispatch [:b/route-search search-term]))

(sec/defroute buyers-home "/b/home/" [query-params]
  (rf/dispatch [:b/route-home query-params]))

(sec/defroute buyers-preposals "/b/preposals/" [query-params]
  (rf/dispatch [:b/route-preposals query-params]))

(sec/defroute buyers-preposal-detail "/b/preposals/:idstr" [idstr]
  (rf/dispatch [:b/route-preposal-detail idstr]))

(sec/defroute buyers-product-detail "/b/products/:idstr" [idstr]
  (rf/dispatch [:b/route-product-detail idstr]))

(sec/defroute vendors-home "/v/home/" [query-params]
  (do (.log js/console "nav vendors")
      (rf/dispatch [:v/route-home query-params])))

(sec/defroute vendors-products "/v/products/" [query-params]
  (do (.log js/console "nav vendors")
      (rf/dispatch [:v/route-products query-params])))

(sec/defroute login-path "/login" [query-params]
  (rf/dispatch [:route-login query-params]))

(sec/defroute buyers-signup-path "/b/signup" [query-params]
  (rf/dispatch [:b/route-signup query-params]))

(sec/defroute vendors-signup-path "/v/signup" [query-params]
  (rf/dispatch [:v/route-signup query-params]))

(sec/defroute catchall-path "*" []
  (do (.log js/console "nav catchall")
      (rf/dispatch [:apply-route nil])))

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
                 :logged-in? true
                 :memberships memberships
                 :admin? admin?
                 ;; TODO support users with multi-orgs
                 :active-memb-id (some-> memberships first :id))
      :cookies {:admin-token (when admin?
                               [(:session-token local-store)
                                {:max-age 3600 :path "/"}])}
      :after-req-session nil}
     {:after-req-session nil})))

(defn config-acct []
  (acct/configure-navigation!
   {:nav-handler (fn [path]
                   (r/after-render clerk/after-render!)
                   (sec/dispatch! path)
                   (clerk/navigate-page! path))
    :path-exists? sec/locate-route
    :reload-same-path? false}))

;; additional init that must occur after :ws/req-session
(rf/reg-fx
 :after-req-session
 (fn []
   (clerk/initialize!)
   (config-acct)
   (acct/dispatch-current!)
   (mount-components)))

(defonce init-done? (volatile! false))

(defn init! []
  (if @init-done?
    (println "init! SKIPPED")
    (do
      (println "init! START")
      (vreset! init-done? true)
      (rf/dispatch-sync [:init-db])
      (rf/dispatch-sync [:ws-init])
      (rf/dispatch-sync [:ws-get-session-user])
      (println "init! END"))))

;; for dev
(defn re-init! []
  (vreset! init-done? false)
  (init!))

(println "END core")
