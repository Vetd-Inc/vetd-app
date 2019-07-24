(ns vetd-app.common.components
  (:require [vetd-app.ui :as ui]
            [clojure.string :as s]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn c-loader [{:keys [props]}]
  [:div.spinner props [:i] [:i]])

(defn c-avatar-initials
  [user-name]
  (let [parts (s/split user-name " ")]
    [:div.avatar.avatar-initials (->> (select-keys parts [0 (dec (count parts))])
                                      vals
                                      (map first)
                                      (apply str))]))
