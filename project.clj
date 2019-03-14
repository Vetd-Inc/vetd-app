(defproject vetd-app "0.1.0-SNAPSHOT"
  
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.516" :scope "provided"]
                 [org.clojure/core.async "0.4.474"]

                 [org.clojure/java.jdbc "0.7.5"]
                 [org.postgresql/postgresql "42.2.2"]
                 [honeysql "0.9.3"]
                 [migratus "1.2.0"]
                 [district0x/graphql-query "1.0.5"]
                 [district0x/district-graphql-utils "1.0.5"]

                 [com.cognitect.aws/api "0.8.273"]
                 [com.cognitect.aws/endpoints "1.1.11.507"]
                 [com.cognitect.aws/sns "697.2.391.0"]

                 [cheshire "5.8.0"]
                 [clj-http "3.9.1"]
                 [clj-time "0.14.4"]
                 [environ "1.1.0"]

                 [compojure "1.6.1"]
                 [metosin/muuntaja "0.5.0"]
                 [aleph "0.4.6"]
                 [ring/ring-core "1.5.1"]
                 [javax.servlet/servlet-api "2.5"]
                 
                 [re-frame "0.10.5"]
                 [reagent "0.8.1"]
                 [reagent-utils "0.3.2"]
                 [re-frisk "0.4.5"]
                 
                 [re-com/re-com "2.1.0"]
                 [cljsjs/semantic-ui-react "0.84.0-0"]
                 [cljsjs/toastr "2.1.2-1"]
                 [clj-commons/secretary "1.2.4"]
                 [venantius/accountant "0.2.4"]

                 [org.clojure/tools.reader "1.2.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.12"] ;; req'd by migratus

                 [buddy/buddy-hashers "1.3.0"]

                 [expound "0.7.2"]]

  :repl-options {:timeout 120000}
  
  :min-lein-version "2.0.0"
  
  :main ^:skip-aot com.vetd.app.core
  :target-path "target/%s"
  ;; might be overkill, but it works
  :source-paths ["src/clj" "src/cljc" "src/cljs" "src/clj/com/vetd/app"
                 "src/cljc/vetd_app" "src/cljs/admin/vetd_admin"
                 "src/cljs/app/vetd_app"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]

  :figwheel {:css-dirs ["resources/public/assets/app/css/"]}

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-environ "1.1.0"]]

  :clean-targets
  ^{:protect false} [:target-path
                     [:cljsbuild :builds :app :compiler :output-dir]
                     [:cljsbuild :builds :app :compiler :output-to]]

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["cljsbuild" "once" "min-public" "min-full"]]
             :cljsbuild
             {:builds
              {:min-public
               {:source-paths ["src/clj" "src/cljc" "src/cljs" "src/clj/com/vetd/app"
                               "src/cljc/vetd_app" "src/cljs/admin/vetd_admin"
                               "src/cljs/app/vetd_app"]
                :compiler
                {:main "vetd-app.app"
                 :output-dir "target/cljsbuild/public/js/public-out"
                 :output-to "target/cljsbuild/public/js/app.js"
                 :source-map "target/cljsbuild/public/js/app.js.map"
                 :optimizations :advanced
                 :pretty-print false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs ["react/externs/react.js"]}}
               :min-full
               {:source-paths ["src/clj" "src/cljc" "src/cljs" "src/clj/com/vetd/app"
                               "src/cljc/vetd_app" "src/cljs/admin/vetd_admin"
                               "src/cljs/app/vetd_app"]
                :compiler
                {:main "vetd-admin.full"
                 :output-dir "target/cljsbuild/public/js/full-out"
                 :output-to "target/cljsbuild/public/js/full.js"
                 :source-map "target/cljsbuild/public/js/full.js.map"
                 :optimizations :advanced
                 :pretty-print false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs ["react/externs/react.js"]}}}}
             :aot :all
             :uberjar-name "vetd-app.jar"
             :source-paths ["src/clj" "src/cljc" "src/cljs" "src/clj/com/vetd/app"
                            "src/cljc/vetd_app" "src/cljs/admin/vetd_admin"
                            "src/cljs/app/vetd_app"]
             :resource-paths ["env/prod/resources"]}

   ;; production build
   :build {;; This is for env vars that are needed during the actual build.
           ;; (other environments variables will be passed in when the uberjar is run)
           :env {:vetd-env "BUILD"
                 :segment-frontend-write-key "VXTgraXuvEsV7MzqWUlgQgMcu94rjzU3"}}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:env {:vetd-env "DEV"
                        :db-type "postgresql"
                        :db-name "vetd1"
                        :db-host "localhost"
                        :db-port "5432"
                        :db-user "vetd"
                        :db-password "vetd"
                        :hasura-ws-url "ws://localhost:8080/v1alpha1/graphql"
                        :hasura-http-url "http://localhost:8080/v1alpha1/graphql"
                        :segment-frontend-write-key "Ieh9p65FemSOa2s1OngMCWTuVkjjt0Kz"}
                  :jvm-opts ["-Dconf=dev-config.edn"]
                  :dependencies [[binaryage/devtools "0.9.10"]
                                 [nrepl "0.6.0"]
                                 [cider/piggieback "0.4.0"]
                                 [day8.re-frame/re-frame-10x "0.3.3-react16"]
                                 [doo "0.1.10"]
                                 [figwheel-sidecar "0.5.18"]
                                 [pjstadig/humane-test-output "0.8.3"]
                                 [prone "1.6.0"]
                                 [ring/ring-devel "1.6.3"]
                                 [ring/ring-mock "0.3.2"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.23.0"]
                                 [lein-doo "0.1.10"]
                                 [lein-figwheel "0.5.18"]]
                  :cljsbuild
                  {:builds
                   [{:id "dev-full"
                     :source-paths ["src/clj/com/vetd/app" "src/cljc/vetd_app"
                                    "src/cljs/admin/vetd_admin" "src/cljs/app/vetd_app"]
                     :figwheel {:on-jsload "vetd-app.core/mount-components"}
                     :compiler
                     {:main "vetd-admin.full"
                      :asset-path "/js/full-out"
                      :output-to "target/cljsbuild/public/js/full.js"
                      :output-dir "target/cljsbuild/public/js/full-out"
                      :source-map true
                      :optimizations :none
                      :pretty-print true
                      :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
                      :preloads [devtools.preload day8.re-frame-10x.preload]}}]}
                  :doo {:build "test"}
                  :source-paths ["src/clj/com/vetd/app" "src/cljc/vetd_app"
                                 "src/cljs/admin/vetd_admin" "src/cljs/app/vetd_app" "dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]
                                 :init-ns repl-init}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}

   :project/test {:jvm-opts ["-Dconf=test-config.edn"]
                  :resource-paths ["env/test/resources"]
                  :cljsbuild
                  {:builds
                   {:test
                    {:source-paths ["src/cljc" "src/cljs/admin" "src/cljs/app" "test/cljs"]
                     :compiler
                     {:output-to "target/test.js"
                      :main "vetd-app.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}}
   :profiles/dev {}
   :profiles/test {}})
