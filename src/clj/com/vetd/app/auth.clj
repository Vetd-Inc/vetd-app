(ns com.vetd.app.auth
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [buddy.hashers :as bhsh]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]))


;; max is 100k (not 10k) to avoid conflicts during bursts (which shouldn't really happen)
(def last-id (atom (rand-int 10000)))

(def ts2019-01-01 1546300800000)

(defn ms-since-vetd-epoch []
  (- (System/currentTimeMillis) ts2019-01-01))

(def base36
  (into {}
        (map-indexed vector
                     (concat
                      (range 97 123)
                      (range 48 58)))))

(def base36-inv (clojure.set/map-invert base36))

(defn long-floor-div
  [a b]
  (-> a
      (/ b)
      long))

(defn base36->str
  [v]
  (let [x (loop [v' v
                 r []]
            (if (zero? v')
              r
              (let [idx (mod v' 36)
                    v'' (long-floor-div v' 36)]
                (recur v''
                       (conj r (mod v' 36))))))]
    (->> x
         reverse
         (map base36)
         (map char)
         clojure.string/join)))

(defn base36->num
  [s]
  (loop [[head & tail] (reverse s)
         idx 0
         r 0]
    (if (nil? head)
      (int r)
      (let [d (* (base36-inv (int head)) (Math/pow 36 idx))]
        (recur tail
               (inc idx)
               (+ r d))))))

(defn mk-id []
  ;; max is 100k (not 10k) to avoid conflicts during bursts (which shouldn't really happen)
  (let [sub-id (swap! last-id #(-> % inc (mod 100000)))] 
    (-> (ms-since-vetd-epoch)
        (long-floor-div 100)
        (* 10000)
        (+ sub-id))))

(defn mk-id&str []
  (let [id (mk-id)]
    [id (base36->str id)]))

;; this seems secure
(defn mk-session-token []
  (let [base 1000000
        f #(base36->str
            (+ base (rand-int (- Integer/MAX_VALUE base))))]
    (-> (concat (f) (f) (f) (f))
        shuffle
        clojure.string/join)))

(defn select-org-by-name [org-name]
  (-> [[:orgs {:oname org-name}
        [:id :created :idstr :oname :buyer? :vendor? :short-desc :long-desc]]]
      ha/sync-query
      vals
      ffirst))

(defn select-org-by-id [org-id]
  (-> [[:orgs {:id org-id}
        [:id :created :idstr :oname :buyer? :vendor? :short-desc :long-desc]]]
      ha/sync-query
      vals
      ffirst))

(defn insert-org [org-name buyer? vendor?]
  (let [[id idstr] (mk-id&str)]
    [id
     (db/insert! :orgs
                 {:id id
                  :idstr idstr
                  :org_name org-name
                  :is-buyer buyer?
                  :is-vendor vendor?})]))

(defn insert-select-org
  [org-name buyer? vendor?]
  (let [[uuid [n]] (insert-org org-name buyer? vendor?)]
    (when (pos-int? n)
      (select-org-by-id uuid))))

(defn create-or-find-org
  [org-name buyer? vendor?]
  (if (select-org-by-name org-name)
    [false nil]
    [true (insert-select-org org-name buyer? vendor?)]))

(defn select-user-by-email
  [email & fields]
  (-> [[:users {:email email}
        (or (not-empty fields)
            [:id :uname :pwd])]]
      ha/sync-query
      vals
      ffirst))

(defn select-user-by-id
  [id & fields]
  (-> [[:users {:id id}
        (or (not-empty fields)
            [:id :uname :created])]]
      ha/sync-query
      vals
      ffirst))

(defn insert-user
  [uname email pwd]
  (let [uuid (ut/uuid-str)]  
    [uuid
     (db/insert! :users {:id uuid
                         :uname uname
                         :email email
                         :pwd (bhsh/derive pwd)})]))

(defn insert-select-user
  [uname email pwd]
  (let [[uuid [n]] (insert-user uname email pwd)]
    (when (pos-int? n)
      (select-user-by-id uuid))))

#_(insert-select-user "John Test" "jtest@gmail.com" "hello")

(defn valid-creds?
  [email pwd]
  (when-let [{pwd-hsh :pwd :as user} (select-user-by-email email)]
    (when (bhsh/check pwd pwd-hsh)
      user)))

#_ (valid-creds? "pass@word.com" "ppp")

(defn select-memb-by-ids
  [user-id org-id]
  (-> [[:memberships
        {:user-id user-id
         :org-id org-id}
        [:id :user-id :org-id :created]]]
      ha/sync-query
      vals
      ffirst))

(defn select-memb-org-by-user-id
  [user-id]
  (-> [[:memberships {:user-id user-id}
        [:id :user-id :org-id
         [:org [:id :oname :id :created :buyer? :vendor?]]]]]
      ha/sync-query
      :memberships))



(defn select-memb-by-id
  [id]
  (-> [[:memberships
        {:id id}
        [:id :user-id :org-id :created]]]
      ha/sync-query
      :memberships))

(defn insert-memb
  [user-id org-id]
  (let [uuid (ut/uuid-str)]
    [uuid
     (db/insert! :memberships
                 {:id uuid
                  :user_id user-id
                  :org_id org-id})]))

(defn insert-select-memb
  [user-id org-id]
  (let [[id [n]] (insert-memb user-id org-id)]
    (when (pos-int? n)
      (select-memb-by-id id))))

(defn create-or-find-memb
  [user-id org-id]
  (if (select-memb-by-ids user-id org-id)
    [false nil]
    [true (insert-select-memb user-id org-id)]))

(defn create-account
  [{:keys [uname org-name email pwd b-or-v?]}]
  (try
    (if (select-user-by-email email)
      {:email-used? true}
      (let [user (insert-select-user uname email pwd)
            [org-created? org] (create-or-find-org org-name
                                                   (true? b-or-v?)
                                                   (false? b-or-v?))
            [memb-created? memb] (create-or-find-memb (:id user)
                                                      (:id org))]
        {:user-created? true
         :user user
         :org-created? org-created?
         :org org
         :memb-created? memb-created?
         :memb memb}))
    (catch Throwable e
      (log/error e))))

#_(create-account {:uname "John Test7"
                 :org-name "Test Org7"
                 :email "jt4@test7.com"
                 :pwd "hello"
                 :b-or-v? true})

(defn select-session-by-id
  [session-token]
  (-> [[:sessions
        {:token session-token}
        [:user-id]]]
      ha/sync-query
      :sessions
      first))

(defn select-active-session-by-token
  [session-token]
  (-> [[:sessions
        {:token session-token
         :deleted {:_is_null true}}
        [:user-id]]]
      ha/sync-query
      :sessions
      first))

(defn insert-session
  [user-id]
  (let [id (mk-id)]
    [id
     (db/insert! :sessions
                 {:id id
                  :token (mk-session-token)
                  :user_id user-id
                  :created (ut/now-ts)})]))

(defn insert-select-session
  [user-id]
  (let [[id [n]] (insert-session user-id)]
    n
    ;; wat?
#_    (when (pos-int? n)
      (select-session-by-id id))))

(defn login
  [{:keys [email pwd]}]
  (or (when-let [{:keys [id] :as user} (valid-creds? email pwd)]
        (let [memberships (-> id select-memb-org-by-user-id not-empty)]
          (when (->> memberships (keep :org) not-empty)
            {:logged-in? true
             :session-token (-> id insert-select-session :token)
             :user (dissoc user :pwd)
             :memberships memberships})))
      {:logged-in? false
       :login-failed? true}))

(defmethod com/handle-ws-inbound :create-acct
  [m ws-id sub-fn]
  (create-account m))

(defmethod com/handle-ws-inbound :auth-by-creds
  [{:keys [email pwd] :as req} ws-id sub-fn]
  (login req))

(defmethod com/handle-ws-inbound :auth-by-session
  [{:keys [session-token] :as req} ws-id sub-fn]
  (if-let [{:keys [user-id]} (select-active-session-by-token session-token)]
    {:logged-in? true
     :session-token session-token
     :user (select-user-by-id user-id)
     :memberships (select-memb-org-by-user-id user-id)}
    {:logged-in? false}))


;; TODO logout!!!!!!!!!!!!!!!
