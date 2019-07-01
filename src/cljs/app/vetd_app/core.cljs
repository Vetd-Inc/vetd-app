(ns vetd-app.core
  (:require vetd-app.websockets
            vetd-app.graphql
            vetd-app.local-store
            vetd-app.cookies
            vetd-app.analytics
            vetd-app.url
            vetd-app.debounce
            vetd-app.common.fx
            [vetd-app.hooks :as hooks]
            [vetd-app.buyers.fixtures :as b-fix]
            [vetd-app.buyers.pages.search :as p-bsearch]
            [vetd-app.buyers.pages.preposals :as p-bpreposals]
            [vetd-app.buyers.pages.preposal-detail :as p-bpreposal-detail]
            [vetd-app.buyers.pages.product-detail :as p-bproduct-detail]
            [vetd-app.buyers.pages.rounds :as p-brounds]
            [vetd-app.buyers.pages.round-detail :as p-bround-detail]
            [vetd-app.vendors.fixtures :as v-fix]
            [vetd-app.vendors.pages.preposals :as p-vpreposals]
            [vetd-app.vendors.pages.products :as p-vprods]
            [vetd-app.vendors.pages.product-detail :as p-vprod-detail]
            [vetd-app.vendors.pages.profile :as p-vprofile]
            [vetd-app.vendors.pages.rounds :as p-vrounds]
            [vetd-app.vendors.pages.round-product-detail :as p-vround-product-detail]
            [vetd-app.common.fixtures :as pub-fix]
            [vetd-app.common.pages.signup :as p-signup]
            [vetd-app.common.pages.login :as p-login]
            [vetd-app.common.pages.forgot-password :as p-forgot-password]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as sec]
            [accountant.core :as acct]
            [clerk.core :as clerk]))

(println "START core")

(hooks/reg-hooks! hooks/c-page
                  {:login #'p-login/c-page
                   :signup #'p-signup/c-page
                   :forgot-password #'p-forgot-password/c-page
                   :b/search #'p-bsearch/c-page
                   :b/preposals #'p-bpreposals/c-page
                   :b/preposal-detail #'p-bpreposal-detail/c-page
                   :b/product-detail #'p-bproduct-detail/c-page
                   :b/rounds #'p-brounds/c-page
                   :b/round-detail #'p-bround-detail/c-page
                   :v/preposals #'p-vpreposals/c-page
                   :v/products #'p-vprods/c-page
                   :v/product-detail #'p-vprod-detail/c-page
                   :v/profile #'p-vprofile/c-page
                   :v/rounds #'p-vrounds/c-page
                   :v/round-product-detail #'p-vround-product-detail/c-page})

(hooks/reg-hooks! hooks/c-container
                  {:login #'pub-fix/container
                   :signup #'pub-fix/container
                   :forgot-password #'pub-fix/container
                   :b/search #'b-fix/container
                   :b/preposals #'b-fix/container
                   :b/preposal-detail #'b-fix/container
                   :b/product-detail #'b-fix/container
                   :b/rounds #'b-fix/container
                   :b/round-detail #'b-fix/appendable-container
                   :v/preposals #'v-fix/container
                   :v/products #'v-fix/container
                   :v/product-detail #'v-fix/container
                   :v/profile #'v-fix/container
                   :v/rounds #'v-fix/container
                   :v/round-product-detail #'v-fix/container})


(rf/reg-event-db
 :init-db
 (constantly
  {:search-term ""
   :preposals-filter p-bpreposals/default-preposals-filter
   :rounds-filter {:selected-statuses #{}}
   :loading? {:products #{}} ; entities (by ID) that are in a loading?=true state (for UI display)
   :round-products-order []}))

(def public-pages #{:login :signup :forgot-password})

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
    "/a/search"
    (if-let [active-memb (first membs)]
      (if (-> active-memb :org :buyer?)
        "/b/search"
        "/v/preposals")
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

;; Common
(sec/defroute home-path "/" []
  (rf/dispatch [:nav-home]))

(sec/defroute login-path "/login" []
  (rf/dispatch [:route-login]))

(sec/defroute signup-path "/signup/:type" [type]
  (rf/dispatch [:route-signup type]))

(sec/defroute forgot-password-path "/forgot-password/" []
  (rf/dispatch [:route-forgot-password]))
(sec/defroute forgot-password-prefill-path "/forgot-password/:email-address" [email-address]
  (rf/dispatch [:route-forgot-password email-address]))

;; Link - special links for actions such as reset password, or account verification
(sec/defroute link-path "/l/:k" [k]
  (rf/dispatch [:read-link k]))

;; Buyers
(sec/defroute buyers-search-root "/b/search" []
  (rf/dispatch [:b/route-search]))
(sec/defroute buyers-search "/b/search/:search-term" [search-term]
  (rf/dispatch [:b/route-search search-term]))

(sec/defroute buyers-preposals "/b/preposals" [query-params]
  (rf/dispatch [:b/route-preposals query-params]))
(sec/defroute buyers-preposal-detail "/b/preposals/:idstr" [idstr]
  (rf/dispatch [:b/route-preposal-detail idstr]))

(sec/defroute buyers-product-detail "/b/products/:idstr" [idstr]
  (rf/dispatch [:b/route-product-detail idstr]))

(sec/defroute buyers-rounds "/b/rounds" [query-params]
  (rf/dispatch [:b/route-rounds query-params]))
(sec/defroute buyers-round-detail "/b/rounds/:idstr" [idstr]
  (rf/dispatch [:b/route-round-detail idstr]))

;; Vendors
(sec/defroute vendors-preposals "/v/preposals" [query-params]
  (rf/dispatch [:v/route-preposals query-params]))

(sec/defroute vendors-products "/v/products" [query-params]
  (rf/dispatch [:v/route-products query-params]))
(sec/defroute vendors-product-detail "/v/products/:idstr" [idstr]
  (rf/dispatch [:v/route-product-detail idstr]))

(sec/defroute vendors-profile "/v/profile" [query-params]
  (rf/dispatch [:v/route-profile query-params]))

(sec/defroute vendors-rounds-path "/v/rounds" [query-params]
  (rf/dispatch [:v/route-rounds query-params]))
(sec/defroute vendors-round-product-detail "/v/rounds/:round-idstr/products/:product-idstr"
  [round-idstr product-idstr]
  (rf/dispatch [:v/route-round-product-detail round-idstr product-idstr]))

;; catch-all
(sec/defroute catch-all-path "*" []
  (do (.log js/console "nav catch-all")
      (rf/dispatch [:nav-home])))

(rf/reg-event-fx
 :ws-get-session-user
 [(rf/inject-cofx :local-store [:session-token])] 
 (fn [{:keys [local-store]}]
   {:ws-send {:payload {:cmd :auth-by-session
                        :return :ws/req-session
                        :session-token (:session-token local-store)}}}))

(rf/reg-event-fx
 :ws/req-session
 [(rf/inject-cofx :local-store [:session-token])]  
 (fn [{:keys [db local-store]} [_ {:keys [logged-in? user memberships admin?]}]]
   (if logged-in?
     (let [org-id (some-> memberships first :org-id)] ; TODO support users with multi-orgs
       {:db (assoc db  
                   :logged-in? true
                   :user user
                   :memberships memberships
                   :active-memb-id (some-> memberships first :id)
                   :org-id org-id
                   :admin? admin?)
        :cookies {:admin-token (when admin? [(:session-token local-store)
                                             {:max-age 3600 :path "/"}])}
        :analytics/identify {:user-id (:id user)
                             :traits {:name (:uname user)
                                      :displayName (:uname user)                                      
                                      :email (:email user)
                                      ;; only for MailChimp integration
                                      :fullName (:uname user)
                                      :userStatus (if (some-> memberships first :buyer?) "Buyer" "Vendor")}}
        :analytics/group {:group-id org-id
                          :traits {:name (some-> memberships first :org :oname)}}
        :after-req-session nil})
     {:after-req-session nil})))

(defn config-acct []
  (acct/configure-navigation!
   {:nav-handler (fn [path]
                   (r/after-render clerk/after-render!)
                   (sec/dispatch! path)
                   (clerk/navigate-page! path))
    :path-exists? sec/locate-route
    :reload-same-path? false}))

(defonce additional-init-done? (volatile! false))

;; additional init that must occur after :ws/req-session
(rf/reg-fx
 :after-req-session
 (fn []
   (when-not @additional-init-done?
     (vreset! additional-init-done? true)
     (clerk/initialize!)
     (config-acct)
     (acct/dispatch-current!)
     (mount-components))))

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

(println "END core")
