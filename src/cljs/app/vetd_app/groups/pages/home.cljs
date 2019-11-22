(ns vetd-app.groups.pages.home
  (:require [vetd-app.ui :as ui]
            [vetd-app.util :as util]
            [vetd-app.common.components :as cc]
            [vetd-app.buyers.components :as bc]
            [vetd-app.common.fx :as cfx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as s]))

(def init-db
  {:filter {:groups #{}}
   :threads {:data []
             :limit 5
             :loading? true
             ;; ids of threads that are expanded to show the contained messages
             :expanded-ids #{}}
   :recent-rounds {:data []
                   :limit 4
                   :loading? true}})

(rf/reg-event-fx
 :g/nav-home
 (constantly
  {:nav {:path "/c/home"}
   :analytics/track {:event "Navigate"
                     :props {:category "Navigation"
                             :label "Groups Home"}}}))

(rf/reg-event-fx
 :g/route-home
 (fn [{:keys [db]}]
   {:db (assoc db
               :page :g/home
               :page-params {:fields-editing #{}})
    :dispatch [:g/home.filter.reset]}))

(rf/reg-event-fx
 :g/home.filter.add ;; group arg is "filter group" not community
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:groups :filter group] conj value)}))

(rf/reg-event-fx
 :g/home.filter.remove
 (fn [{:keys [db]} [_ group value]]
   {:db (update-in db [:groups :filter group] disj value)}))

(rf/reg-event-fx
 :g/home.filter.reset
 (fn [{:keys [db]}]
   (let [{:keys [memberships active-memb-id]} db]
     {:db (assoc-in db
                    [:groups :filter :groups]
                    (some->> memberships
                             (filter #(-> % :id (= active-memb-id)))
                             first
                             :org
                             :groups
                             (map :id)
                             set))})))

(rf/reg-event-fx
 :g/recent-rounds.data.set
 (fn [{:keys [db]} [_ rounds]]
   {:db (assoc-in db [:groups :recent-rounds :data] rounds)}))

(rf/reg-event-fx
 :g/recent-rounds.limit.add
 (fn [{:keys [db]} [_ num-items]]
   {:db (update-in db [:groups :recent-rounds :limit] + num-items)}))

(rf/reg-event-fx
 :g/recent-rounds.loading?.set
 (fn [{:keys [db]} [_ loading?]]
   {:db (assoc-in db [:groups :recent-rounds :loading?] loading?)}))

(rf/reg-event-fx
 :g/join-request
 (fn [{:keys [db]} [_ group-ids]]
   {:ws-send {:payload {:cmd :g/join-request
                        :return {:handler :g/join-request.return}
                        :group-ids group-ids
                        :buyer-id (util/db->current-org-id db)}}
    :analytics/track {:event "Request to Join"
                      :props {:category "Community"}}}))

(rf/reg-event-fx
 :g/join-request.return
 (constantly
  {:toast {:type "success"
           :title "Request Sent"
           :message "We'll be in touch via email with next steps shortly."}}))

(rf/reg-event-fx
 :g/threads.data.set
 (fn [{:keys [db]} [_ threads]]
   {:db (assoc-in db [:groups :threads :data] threads)}))

(rf/reg-event-fx
 :g/threads.limit.add
 (fn [{:keys [db]} [_ num-items]]
   {:db (update-in db [:groups :threads :limit] + num-items)}))

(rf/reg-event-fx
 :g/threads.loading?.set
 (fn [{:keys [db]} [_ loading?]]
   {:db (assoc-in db [:groups :threads :loading?] loading?)}))

(rf/reg-event-fx
 :g/home.threads.expand
 (fn [{:keys [db]} [_ id]]
   {:db (update-in db [:groups :threads :expanded-ids] conj id)}))

(rf/reg-event-fx
 :g/home.threads.collapse
 (fn [{:keys [db]} [_ id]]
   {:db (update-in db [:groups :threads :expanded-ids] disj id)}))

(rf/reg-event-fx
 :g/threads.create.submit
 (fn [{:keys [db]} [_ {:keys [group-id title message] :as input}]]
   (cfx/validated-dispatch-fx db
                              [:g/threads.create input]
                              #(cond
                                 (s/blank? group-id) [:thread-title "Please select a community to post to."]
                                 (s/blank? title) [:thread-title "Please enter a title for your thread."]
                                 (s/blank? message) [:message "Please enter a message."]
                                 :else nil))))

(rf/reg-event-fx
 :g/threads.create
 (fn [{:keys [db]} [_ {:keys [group-id title message] :as input}]]
   {:ws-send {:payload {:cmd :g/threads.create
                        :return {:handler :g/threads.create.return}
                        :group-id group-id
                        :title title
                        :message message
                        :user-id (-> db :user :id)
                        :org-id (-> db :org-id)}}
    :analytics/track {:event "Create Thread"
                      :props {:category "Discussions"}}}))

(rf/reg-event-fx
 :g/threads.create.return
 (fn [{:keys [db]}]
   {:toast {:type "success"
            :title "Discussion thread posted!"}
    :dispatch [:stop-edit-field "new-thread"]}))

(rf/reg-event-fx
 :g/threads.reply.submit
 (fn [{:keys [db]} [_ {:keys [thread-id text after-valid-fn] :as input}]]
   (cfx/validated-dispatch-fx db
                              [:g/threads.reply input]
                              #(cond
                                 (s/blank? text) [:reply-text "Please enter a message."]
                                 :else nil))))

(rf/reg-event-fx
 :g/threads.reply
 (fn [{:keys [db]} [_ {:keys [thread-id text after-valid-fn] :as input}]]
   (do (when after-valid-fn (after-valid-fn))
       {:ws-send {:payload {:cmd :g/threads.reply
                            :return {:handler :g/threads.reply.return}
                            :text text
                            :thread-id thread-id
                            :user-id (-> db :user :id)
                            :org-id (-> db :org-id)}}
        :analytics/track {:event "Reply To Thread"
                          :props {:category "Discussions"}}})))

(rf/reg-event-fx
 :g/threads.reply.return
 (fn [{:keys [db]}]
   {:toast {:type "success"
            :title "Reply posted!"}}))

;;;; Subscriptions
(rf/reg-sub
 :g/home
 (fn [{:keys [groups]}] groups))

(rf/reg-sub
 :g/home.filter
 :<- [:g/home]
 (fn [{:keys [filter]}] filter))

(rf/reg-sub
 :g/home.threads
 :<- [:g/home]
 (fn [{:keys [threads]}] threads))

(rf/reg-sub
 :g/home.threads.data
 :<- [:g/home.threads]
 (fn [{:keys [data]}] data))

(rf/reg-sub
 :g/home.threads.limit
 :<- [:g/home.threads]
 (fn [{:keys [limit]}] limit))

(rf/reg-sub
 :g/home.threads.loading?
 :<- [:g/home.threads]
 (fn [{:keys [loading?]}] loading?))

(rf/reg-sub
 :g/home.threads.expanded-ids
 :<- [:g/home.threads]
 (fn [{:keys [expanded-ids]}] expanded-ids))

(rf/reg-sub
 :g/home.recent-rounds
 :<- [:g/home]
 (fn [{:keys [recent-rounds]}] recent-rounds))

(rf/reg-sub
 :g/home.recent-rounds.data
 :<- [:g/home.recent-rounds]
 (fn [{:keys [data]}] data))

(rf/reg-sub
 :g/home.recent-rounds.limit
 :<- [:g/home.recent-rounds]
 (fn [{:keys [limit]}] limit))

(rf/reg-sub
 :g/home.recent-rounds.loading?
 :<- [:g/home.recent-rounds]
 (fn [{:keys [loading?]}] loading?))

;;;; Components
(defn c-group-filter
  [group]
  (let [filter& (rf/subscribe [:g/home.filter])]
    (fn [{:keys [id idstr gname orgs] :as group}]
      (let [num-orgs (count orgs)]
        [:> ui/Checkbox
         {:checked (-> @filter& :groups (contains? id) boolean)
          :on-click (fn [_ this]
                      (rf/dispatch [(if (.-checked this)
                                      :g/home.filter.add
                                      :g/home.filter.remove)
                                    :groups
                                    id]))
          :label {:children
                  (r/as-element
                   [:div {:style {:position "relative"
                                  :top -1
                                  :left 4}}
                    [:h4 {:style {:margin "0 0 5px 0"
                                  :padding "0 0 0 0"}}
                     gname]
                    [:a {:on-click (fn [e]
                                     (do (.stopPropagation e)
                                         (rf/dispatch [:g/nav-detail idstr])))}
                     [:> ui/Icon {:name "group"}]
                     (str " " num-orgs " organization" (when-not (= num-orgs 1) "s") " ")]])}}]))))


(defn c-stack-item
  [product top-products]
  (let [{product-id :id
         product-idstr :idstr
         :keys [pname short-desc logo vendor discounts
                agg-group-prod-rating agg-group-prod-price]} product
        orgs-using-count (->> top-products
                              (filter #(= (:product-id %) product-id))
                              first
                              :count-stack-items)
        mean (->> agg-group-prod-rating
                  (filter #(= (:product-id %) product-id))
                  util/rating-avg-map
                  :mean)]
    [:> ui/Item {:on-click #(rf/dispatch [:b/nav-product-detail product-idstr])}
     [bc/c-product-logo logo]
     [:> ui/ItemContent
      [:> ui/ItemHeader {:class "shorten"}
       pname]
      [:> ui/ItemExtra {:class "product-tags shorten"}
       [bc/c-categories product]]]
     [:div.community {:on-click #(rf/dispatch
                                  [:dispatch-stash.push :product-detail-loaded
                                   [:do-fx
                                    {:dispatch-later
                                     [{:ms 200
                                       :dispatch [:scroll-to :product/community]}]}]])}
      [:div.metric 
       "Used by " orgs-using-count " "
       [:> ui/Popup
        {:position "bottom right"
         :offset 10
         :content "Click to see organizations that are using this product."
         :trigger (r/as-element [:> ui/Icon {:name "group"}])}]]
      [:div.metric
       (let [median-prices (map :median-price agg-group-prod-price)]
         (if (seq median-prices)
           (str "$" ;; get the mean from all the member'd groups' medians
                (util/decimal-format (/ (apply + median-prices) (count median-prices)))
                " / year")
           "No pricing data."))]
      [:> ui/Rating {:rating mean
                     :maxRating 5
                     :size "large"
                     :disabled true}]]]))

(defn c-popular-stack
  [top-products]
  (let [group-ids& (rf/subscribe [:group-ids])
        filter& (rf/subscribe [:g/home.filter])]
    (fn [top-products]
      (let [top-products-ids (map :product-id top-products)
            selected-group-ids (:groups @filter&)
            products-unordered @(rf/subscribe
                                 [:gql/q
                                  {:queries
                                   [[:products {:id top-products-ids}
                                     [:id :pname :idstr :logo :score
                                      [:vendor
                                       [:id :oname :idstr :short-desc]]
                                      [:categories {:ref-deleted nil
                                                    :_limit 1}
                                       [:id :idstr :cname]]
                                      [:agg-group-prod-rating {:group-id selected-group-ids}
                                       [:group-id :product-id
                                        :count-stack-items :rating]]
                                      [:agg-group-prod-price {:group-id selected-group-ids}
                                       [:group-id :median-price]]
                                      [:discounts {:id selected-group-ids
                                                   :ref-deleted nil}
                                       [:group-discount-descr :gname]]]]]}])]
        [:div.secondary-list
         [:h1 "Popular Products"]
         [:> ui/ItemGroup {:class "results"}
          (when-not (= :loading products-unordered)
            (let [products (->> products-unordered
                                :products
                                (sort-by #(.indexOf top-products-ids (:id %))))]
              (if (seq products)
                (doall
                 (for [product products]
                   ^{:key (:id product)}
                   [c-stack-item product top-products]))
                "No popular products yet.")))]]))))

(defn c-round
  [round]
  (let [{:keys [id idstr created status title buyer products]} round]
    [:> ui/Item {:on-click #(rf/dispatch [:b/nav-round-detail idstr])}
     [:> ui/ItemContent
      [:> ui/ItemHeader title]
      [:> ui/ItemExtra {:class "product-tags"
                        :style {:margin-bottom 6}}
       [:> ui/Icon {:name (case status
                            "initiation" "wpforms" ;; this status is hidden anyways
                            "in-progress" "chart bar"
                            "complete" "check")
                    :style {:margin-right 4}
                    :title (case status
                             ;; initiation status is hidden anyways
                             "initiation" "Not yet initiated"
                             "in-progress" "In Progress"
                             "complete" "Complete")}]
       (util/relative-datetime (.getTime (js/Date. created))
                               {:trim-day-of-week? true})
       " for " (:oname buyer)]
      [:div.product-list (s/join ", " (map :pname products))]]]))

(defn c-load-more
  [{:keys [event]}]
  [:> ui/Button {:on-click #(when event
                              (rf/dispatch event))
                 :color "cleargrey"
                 :fluid true}
   "View More"])

(defn c-recent-rounds
  [selected-group-ids]
  (let [data& (rf/subscribe [:g/home.recent-rounds.data])
        limit& (rf/subscribe [:g/home.recent-rounds.limit])
        loading?& (rf/subscribe [:g/home.recent-rounds.loading?])]
    (fn [selected-group-ids]
      (let [groups->rounds @(rf/subscribe [:gql/sub
                                           {:queries
                                            [[:groups {:id selected-group-ids
                                                       :deleted nil}
                                              [:id
                                               [:recent-rounds {:_limit (inc @limit&)}
                                                [:group-id :round-id]]]]]}])
            recent-round-ids (if (= :loading groups->rounds)
                               nil
                               (->> groups->rounds
                                    :groups
                                    (map :recent-rounds)
                                    flatten
                                    (map :round-id)))
            rounds-details @(rf/subscribe
                             [:gql/q
                              {:queries
                               [[:rounds {:_where {:id {:_in (map str recent-round-ids)}}
                                          :_limit @limit&
                                          :_order_by {:created :desc}}
                                 [:id :idstr :created :status :title
                                  [:buyer
                                   [:oname]]
                                  [:products {:ref-deleted nil}
                                   [:pname]]]]]}])
            _ (when-not (or (= :loading groups->rounds)
                            (= :loading rounds-details))
                (do (rf/dispatch [:g/recent-rounds.data.set (:rounds rounds-details)])
                    (rf/dispatch [:g/recent-rounds.loading?.set false])))]
        (if (or (and (= :loading groups->rounds)
                     (empty? @data&))
                @loading?&)
          [cc/c-loader]
          [:div.secondary-list {:style {:margin-bottom 14}}
           [:h1 "Recent VetdRounds"]
           [:> ui/ItemGroup {:class "results"}
            (if (seq @data&)
              [:<>
               (doall
                (for [round @data&]
                  ^{:key (:id round)}
                  [c-round round]))
               (when (or (> (count recent-round-ids) @limit&)
                         (= :loading rounds-details))
                 [c-load-more {:event [:g/recent-rounds.limit.add 4]}])]
              "No recent VetdRounds.")]])))))

(def ftype->icon
  {:round-init-form-completed "vetd vetd-colors"
   :round-winner-declared "trophy yellow"
   :stack-update-rating "star yellow"
   :stack-add-items "grid layout grey"
   :preposal-request "clipboard outline"
   :buy-request "cart"
   :complete-vendor-profile-request "wpforms grey"
   :complete-product-profile-request "wpforms grey"})

(defn event-data->message
  [ftype data]
  (case ftype
    :round-init-form-completed (let [{:keys [buyer-org-name title]} data]
                                 [:span buyer-org-name " started a VetdRound called "
                                  [:em title]])
    :round-winner-declared (let [{:keys [buyer-org-name product-name]} data]
                             [:span buyer-org-name " chose " [:em product-name]
                              " as the winner of their VetdRound"])
    :stack-update-rating (let [{:keys [buyer-org-name product-name rating]} data]
                           [:span buyer-org-name " rated "
                            [:em product-name] " with " rating " star" (when-not (= rating 1) "s")])
    :stack-add-items (let [{:keys [buyer-org-name product-name]} data]
                       [:span buyer-org-name " added " [:em product-name] " to their Stack"])
    :preposal-request (let [{:keys [buyer-org-name product-name]} data]
                        [:span buyer-org-name " requested an estimate for " [:em product-name]])
    :buy-request (let [{:keys [buyer-org-name product-name]} data]
                   [:span buyer-org-name " decided to buy " [:em product-name]])
    :complete-vendor-profile-request (let [{:keys [buyer-org-name vendor-name]} data]
                                       [:span buyer-org-name " requested the complete profile of "
                                        [:em vendor-name]])
    :complete-product-profile-request (let [{:keys [buyer-org-name product-name]} data]
                                        [:span buyer-org-name " requested the complete profile of "
                                         [:em product-name]])
    "Unknown event."))

(defn event-data->click-event
  [ftype {:keys [product-id round-id buyer-org-id vendor-name] :as data}]
  (case ftype
    :round-init-form-completed [:b/nav-round-detail (util/base31->str round-id)]
    :round-winner-declared [:b/nav-round-detail (util/base31->str round-id)]
    :stack-update-rating [:b/nav-product-detail (util/base31->str product-id)]
    :stack-add-items (if (= buyer-org-id @(rf/subscribe [:org-id]))
                       [:b/nav-stack]
                       [:b/nav-stack-detail (util/base31->str buyer-org-id)])
    :preposal-request [:b/nav-product-detail (util/base31->str product-id)]
    :buy-request [:b/nav-product-detail (util/base31->str product-id)]
    ;; TODO this is a stopgap till we have vendor pages
    :complete-vendor-profile-request [:b/nav-search vendor-name]
    :complete-product-profile-request [:b/nav-product-detail (util/base31->str product-id)]
    "Unknown event."))

(defn c-feed-event
  [{:keys [id ftype journal-entry-created data]} group-name]
  [:> ui/FeedEvent (if-let [event (event-data->click-event (keyword ftype) data)]
                     {:on-click #(rf/dispatch event)}
                     {:class "no-click"})
   [:> ui/FeedLabel
    [:> ui/Icon {:name (ftype->icon (keyword ftype))}]]
   [:> ui/FeedContent
    [:> ui/FeedSummary (event-data->message (keyword ftype) data)]
    [:> ui/FeedDate
     (str (util/relative-datetime (.getTime (js/Date. journal-entry-created))) " in " group-name)]]])

(defn c-feed
  [groups]
  (let [org-ids (->> groups (map :orgs) flatten (map :id) distinct)
        org-id->group-name (->> (for [{:keys [gname orgs]} groups]
                                  (map (fn [org] [(:id org) gname]) orgs))
                                (apply concat)
                                (into {}))
        ;; when adding an ftype, be sure to update:
        ;;   event-data->click-event
        ;;   event-data->message
        ;;   ftype->icon
        supported-ftypes ["round-init-form-completed"
                          "round-winner-declared"
                          "stack-update-rating"
                          "stack-add-items"
                          "preposal-request"
                          "buy-request"
                          "complete-vendor-profile-request"
                          "complete-product-profile-request"]
        feed-events& (rf/subscribe
                      [:gql/sub
                       {:queries
                        [[:feed-events {:org-id org-ids
                                        :ftype supported-ftypes
                                        :_order_by {:journal-entry-created :desc}
                                        :_limit 15
                                        :deleted nil}
                          [:id :journal-entry-created :ftype :data]]]}])]
    (if (= :loading @feed-events&)
      [cc/c-loader]
      (let [feed-events (:feed-events @feed-events&)]
        [bc/c-profile-segment {:title [:<> [:> ui/Icon {:name "feed"}] "Recent Activity"]}
         (if (seq feed-events)
           [:> ui/Feed
            (for [event feed-events]
              ^{:key (:id event)}
              [c-feed-event event (org-id->group-name (-> event :data :buyer-org-id))])]
           [:p {:style {:padding-bottom 15}}
            "No recent activity."])]))))

(defn c-message
  [{:keys [id text created user org] :as message}]
  (let [{:keys [uname]} user
        {:org-id id
         :keys [oname]} org]
    [:> ui/FeedEvent {:class "no-click"}
     [:> ui/FeedLabel
      [cc/c-avatar-initials uname]]
     [:> ui/FeedContent
      [:> ui/FeedSummary (util/text->hiccup text)]
      [:> ui/FeedDate
       (str (util/relative-datetime (.getTime (js/Date. created))
                                    {:trim-day-of-week? true})
            " by " uname " (" oname ")")]]]))

(defn c-thread
  [thread expanded?]
  (let [reply-text& (atom "")
        reply-text-ref& (r/atom nil)
        bad-input& (rf/subscribe [:bad-input])]
    (fn [{:keys [id title created messages] :as thread} expanded?]
      (let [ ;; the root message of the thread is the message that the thread was started with
            root-message (first messages)
            root-uname (:uname (:user root-message))
            root-oname (:oname (:org root-message))]
        [:<>
         [:> ui/FeedEvent {:on-click #(rf/dispatch [(if expanded?
                                                      :g/home.threads.collapse
                                                      :g/home.threads.expand) id])}
          [:> ui/FeedLabel
           [cc/c-avatar-initials root-uname]]
          [:> ui/FeedContent
           [:> ui/FeedSummary (when expanded? {:style {:font-weight "bold"}})
            title]
           (when expanded? (-> messages first :text util/text->hiccup))
           [:> ui/FeedDate
            (str (util/relative-datetime (.getTime (js/Date. created))) " by " root-uname " (" root-oname ")")]]]
         (when expanded?
           [:> ui/Feed {:class "nested"}
            (doall
             (for [message (rest messages)]
               ^{:key (:id message)}
               [c-message message]))
            [:> ui/Form {:as "div"
                         :style {:padding-top 14
                                 :padding-bottom 10}}
             [:> ui/FormField {:error (= @bad-input& :reply-text)}
              [:> ui/TextArea {:placeholder "Reply to this thread..."
                               :spellCheck true
                               :ref (fn [this] (reset! reply-text-ref& (r/dom-node this)))
                               :onChange (fn [_ this]
                                           (reset! reply-text& (.-value this))
                                           (rf/dispatch [:bad-input.reset]))}]]
             [:> ui/Button
              {:color "teal"
               :on-click (fn [] (rf/dispatch [:g/threads.reply.submit
                                              {:thread-id id
                                               :text @reply-text&
                                               :after-valid-fn #(do (aset @reply-text-ref& "value" "")
                                                                    (reset! reply-text& ""))}]))}
              "Post Reply"]]])]))))

(defn c-threads
  [groups]
  (let [data& (rf/subscribe [:g/home.threads.data])
        limit& (rf/subscribe [:g/home.threads.limit])
        loading?& (rf/subscribe [:g/home.threads.loading?])
        expanded-ids& (rf/subscribe [:g/home.threads.expanded-ids])
        fields-editing& (rf/subscribe [:fields-editing])
        bad-input& (rf/subscribe [:bad-input])
        target-group-id& (r/atom (:id (first groups)))
        thread-title& (r/atom "")
        message& (r/atom "")]
    (fn [groups]
      (let [threads @(rf/subscribe
                      [:gql/sub
                       {:queries
                        [[:threads {:_where {:group_id {:_in (map (comp str :id) groups)}}
                                    :_limit @limit&
                                    :_order_by {:created :desc}}
                          [:id :created :title :user-id :org-id :group-id
                           [:messages {:deleted nil}
                            [:id :created :text
                             [:user
                              [:id :uname]]
                             [:org
                              [:id :oname]]]]]]]}])
            _ (when-not (= :loading threads)
                (do (rf/dispatch [:g/threads.data.set (:threads threads)])
                    (rf/dispatch [:g/threads.loading?.set false])))]
        (if @loading?&
          [cc/c-loader]
          [bc/c-profile-segment {:title [:<>
                                         (if (@fields-editing& "new-thread")
                                           [:> ui/Label {:on-click #(rf/dispatch [:stop-edit-field "new-thread"])
                                                         :as "a"
                                                         :style {:float "right"}}
                                            "Cancel"]
                                           [:> ui/Label {:on-click #(rf/dispatch [:edit-field "new-thread"])
                                                         :as "a"
                                                         :color "teal"
                                                         :style {:float "right"}}
                                            [:> ui/Icon {:name "add"}]
                                            "New Thread"])
                                         [:> ui/Icon {:name "discussions"}] "Discussions"]}
           [:div {:style {:width "100%"}}
            (when (@fields-editing& "new-thread")
              [:> ui/Form {:as "div"
                           :style {:padding-bottom 15}}
               (if (> (count groups) 1)
                 [:> ui/FormField {:error (= @bad-input& :target-group-id)}
                  [:> ui/Dropdown {:options (for [{:keys [id gname]} groups]
                                              {:key id
                                               :text gname
                                               :value id})
                                   :default-value @target-group-id&
                                   :placeholder "Post to which community..."
                                   :search false
                                   :selection true
                                   :multiple false
                                   :selectOnBlur false
                                   :selectOnNavigation true
                                   :closeOnChange true
                                   :allowAdditions false
                                   :onChange (fn [_ this] (reset! target-group-id& (.-value this)))}]]
                 (do (reset! target-group-id& (:id (first groups)))
                     nil))
               [:> ui/FormField {:error (= @bad-input& :thread-title)}
                [:> ui/Input
                 {:placeholder "New thread title..."
                  :spellCheck true
                  :autoFocus (not (> (count groups) 1))
                  :on-change (fn [_ this] (reset! thread-title& (.-value this)))}]]
               [:> ui/FormField {:error (= @bad-input& :message)}
                [:> ui/TextArea {:placeholder "Message..."
                                 :spellCheck true
                                 :onChange (fn [_ this] (reset! message& (.-value this)))}]]
               
               [:> ui/Button
                {:color "teal"
                 :on-click (fn [] (rf/dispatch [:g/threads.create.submit {:group-id @target-group-id&
                                                                          :title @thread-title&
                                                                          :message @message&}]))}
                "Post Thread"]])
            (if (seq @data&)
              [:<>
               [:> ui/Feed
                (doall
                 (for [{:keys [id] :as thread} @data&]
                   ^{:key id}
                   [c-thread thread (@expanded-ids& id)]))]
               (when (or (>= (count @data&) @limit&)
                         (= :loading threads))
                 [:div {:style {:padding-bottom 14}}
                  [c-load-more {:event [:g/threads.limit.add 5]}]])]
              (when-not (@fields-editing& "new-thread")
                [:p {:style {:padding-bottom 15}}
                 "No discussion threads yet."]))]])))))

(defn c-join-group-form
  [current-group-ids popup-open?&]
  (let [value& (r/atom [])
        options& (r/atom []) ; options from search results + current values
        search-query& (r/atom "")
        groups->options (fn [groups]
                          (for [{:keys [id gname]} groups]
                            {:key id
                             :text gname
                             :value id}))
        group-ids& (rf/subscribe [:group-ids])]
    (fn [current-group-ids popup-open?&]
      (let [groups& (rf/subscribe
                     [:gql/q
                      {:queries
                       [[:groups {:_where
                                  {:_and ;; while this trims search-query, the Dropdown's search filter doesn't...
                                   [{:gname {:_ilike (str "%" (s/trim @search-query&) "%")}}
                                    {:id {:_nin (concat ;; not sure why these have to be strings
                                                 (map str @group-ids&)
                                                 ;; hide these groups (pertains to prod db)
                                                 ["100" ;; Universal Discounts
                                                  "1811043562864" ;; Test 1
                                                  "2181817440407" ;; Sandbox Demo
                                                  ])}}
                                    {:deleted {:_is_null true}}]}
                                  :_limit 100
                                  :_order_by {:gname :asc}}
                         [:id :gname]]]}])
            _ (when-not (= :loading @groups&)
                (let [options (->> @groups&
                                   :groups
                                   groups->options ; now we have options from gql sub
                                   ;; (this dumbly actually keeps everything, but that seems fine)
                                   (concat @options&) ; keep options for the current values
                                   distinct
                                   (remove (comp (partial contains? current-group-ids) :value)))]
                  (when-not (= @options& options)
                    (reset! options& options))))]
        [:> ui/Form {:as "div"
                     :class "popup-dropdown-form"}
         [:> ui/Dropdown {:loading (= :loading @groups&)
                          :options @options&
                          :placeholder "Search groups..."
                          :search true
                          :selection true
                          :multiple true
                          :selectOnBlur false
                          :selectOnNavigation true
                          :closeOnChange true
                          :allowAdditions true
                          :additionLabel "Hit 'Enter' to Write In "
                          :onAddItem (fn [_ this]
                                       (->> this
                                            .-value
                                            vector
                                            ui/as-dropdown-options
                                            (swap! options& concat)))
                          :onSearchChange (fn [_ this] (reset! search-query& (aget this "searchQuery")))
                          :onChange (fn [_ this] (reset! value& (.-value this)))}]
         [:> ui/Button
          {:color "teal"
           :disabled (empty? @value&)
           :on-click #(do (reset! popup-open?& false)
                          (rf/dispatch [:g/join-request (js->clj @value&)]))}
          "Request to Join"]]))))

(defn c-join-group-button
  [current-group-ids]
  (let [popup-open?& (r/atom false)]
    (fn [current-group-ids]
      [:> ui/Popup
       {:position "bottom left"
        :on "click"
        :open @popup-open?&
        :onOpen #(reset! popup-open?& true)
        :onClose #(reset! popup-open?& false)
        :hideOnScroll false
        :flowing true
        :content (r/as-element [c-join-group-form current-group-ids popup-open?&])
        :trigger (r/as-element
                  [:> ui/Button {:color "lightblue"
                                 :icon true
                                 :labelPosition "left"
                                 :fluid true}
                   "Join a Community"
                   [:> ui/Icon {:name "user plus"}]])}])))

(defn c-page []
  (let [org-id& (rf/subscribe [:org-id])
        group-ids& (rf/subscribe [:group-ids])
        groups& (rf/subscribe [:gql/sub
                               {:queries
                                [[:groups {:id @group-ids&
                                           :deleted nil}
                                  [:id :idstr :gname
                                   [:orgs
                                    [:id]]
                                   ;; TODO put in separate gql and do single query for all selected-group-ids
                                   [:top-products {:_order_by {:count-stack-items :desc}
                                                   :_limit 10}
                                    [:group-id :product-id :count-stack-items]]]]]}])
        filter& (rf/subscribe [:g/home.filter])]
    (fn []
      (if (= :loading @groups&)
        [cc/c-loader]
        [:div.container-with-sidebar
         [:div.sidebar
          [:> ui/Segment {:class "detail-container profile group-filter"}
           [:h1.title  "Communities"]
           (for [group (:groups @groups&)]
             ^{:key (:id group)}
             [c-group-filter group])]
          [:> ui/Segment {:class "detail-container profile"}
           [c-join-group-button (->> @groups& :groups (map :id))]]]
         (let [selected-groups (->> (:groups @groups&)
                                    (filter (comp (:groups @filter&) :id)))]
           [:div.inner-container
            [:> ui/Grid {:stackable true}
             [:> ui/GridRow
              [:> ui/GridColumn {:computer 9 :mobile 16
                                 :style {:padding-right 7}}
               [c-threads selected-groups]
               [c-feed selected-groups]]
              [:> ui/GridColumn {:computer 7 :mobile 16}
               [c-recent-rounds (:groups @filter&)]
               [c-popular-stack (->> selected-groups
                                     (map :top-products)
                                     ;; needs distinct ?
                                     flatten)]]]]])]))))
