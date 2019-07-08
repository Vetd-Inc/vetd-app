(ns vetd-app.core-test
  (:require-macros [latte.core :refer [before describe it]])
  (:require [cljs.test :refer-macros [deftest is testing run-tests]])
  ;; (:require [latte.chai :refer [expect]])
  (:refer-clojure :exclude [first get document])
  
  )

(def cy js/cy)

(describe "Login"
          (before []
                  (.visit cy "http://localhost:5080"))

          (it "logs in" []
              (.. cy
                  (get ".ui.input input")
                  (first)
                  (type "a@a.com"))
              (.. cy
                  (get ".ui.input input")
                  (eq 1) 
                  (type "aaaaaaaa"))
              (.. cy
                  (get "button")
                  (contains "Log In")
                  (click))
              (.. cy
                  (get ".ui.input"))
              ;; (.. cy
              ;;     (document)
              ;;     (its "URL")
              ;;     (notEqual "login")
              ;;     ;; See https://docs.cypress.io/api/commands/should.html#Function
              ;;     ;; (should (fn [hey] true))
              ;;     )
              ))
