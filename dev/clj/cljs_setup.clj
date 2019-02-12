(ns cljs-setup
  (:require [figwheel-sidecar.repl-api :as fw]))

(defn sf []
  (fw/start-figwheel! "dev-public" "dev-full"))

(defn repl-pub []
  (fw/cljs-repl "dev-public"))

(defn repl-full []
  (fw/cljs-repl "dev-full"))


















































