(ns vetd-app.core-test
  (:require-macros [latte.core :refer [before describe it]])
  (:require [latte.chai :refer [expect]])
  (:refer-clojure :exclude [first get]))

(def cy js/cy)

(describe "Login"
          (before []
                  (.visit cy "http://localhost:8080"))

          (it "logs in" []
              (.. cy (get "#name") (type "Pikachu"))
              (.. cy (get "#submit") (click))
              (.. cy
                  (get "#pokémon")
                  (first)
                  ;; See https://docs.cypress.io/api/commands/should.html#Function
                  (should (fn [pokémon]
                            (expect pokémon :to.have.length 1)
                            (expect pokémon :to.contain "Pikachu"))))))
