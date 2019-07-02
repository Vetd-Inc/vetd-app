(defproject vetd-app "0.1.0-SNAPSHOT"
  
  :description "Vetd is a buying platform for B2B SaaS products."
  :url "https://app.vetd.com"
  
  :dependencies [[org.clojure/clojure "1.10.1-beta2"]
                 [org.clojure/clojurescript "1.10.516" :scope "provided"]
                 [org.clojure/core.async "0.4.474"]

                 [org.clojure/java.jdbc "0.7.5"]
                 [org.postgresql/postgresql "42.2.2"]
                 [clj-postgresql "0.7.0"]
                 [honeysql "0.9.3"]
                 [migratus "1.2.0"]
                 [district0x/graphql-query "1.0.5"]
                 [district0x/district-graphql-utils "1.0.5"]

                 [com.cognitect.aws/api "0.8.305"]
                 [com.cognitect.aws/endpoints "1.1.11.553"]
                 [com.cognitect.aws/sns "718.2.444.0"]
                 [com.cognitect.aws/s3 "718.2.457.0"]                 

                 [cheshire "5.8.0"]
                 [clj-http "3.9.1"]
                 [clj-time "0.15.0"]
                 [environ "1.1.0"]

                 [compojure "1.6.1"]
                 [metosin/muuntaja "0.5.0"]
                 [aleph "0.4.6"]
                 [ring/ring-core "1.5.1"]
                 [javax.servlet/servlet-api "2.5"]
                 [hiccup "1.0.5"]
                 
                 [re-frame "0.10.5"]
                 [reagent "0.8.1"]
                 [reagent-utils "0.3.2"]
                 [re-frisk "0.4.5"]
                 
                 [re-com/re-com "2.1.0"]
                 [cljsjs/semantic-ui-react "0.87.1-0"]
                 [cljsjs/toastr "2.1.2-1"]
                 [clj-commons/secretary "1.2.4"]
                 [venantius/accountant "0.2.4"]
                 [pez/clerk "1.0.0"]
                 [markdown-to-hiccup "0.6.2"]

                 [org.clojure/tools.reader "1.2.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.12"] ;; req'd by migratus
                 [conormcd/clj-honeycomb "1.0.6.536"]

                 [buddy/buddy-hashers "1.3.0"]

                 [expound "0.7.2"]

                 [image-resizer "0.1.10"]
                 [net.mikera/imagez "0.12.0"]]

  :repl-options {:timeout 120000}
  
  :min-lein-version "2.0.0"
  
  :main ^:skip-aot com.vetd.app.core
  :target-path "target/%s"
  ;; might be overkill, but it works
  :source-paths ["src/clj" "src/cljc" "src/cljs/admin" "src/cljs/app"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-environ "1.1.0"]
            [lein-jsass "0.2.1"]]
  
  :figwheel {:css-dirs ["resources/public/assets/css/"]}

  :jsass {:source "src/scss"
          :target "resources/public/assets/css"}

  :clean-targets
  ^{:protect false} [:target-path
                     [:cljsbuild :builds :app :compiler :output-dir]
                     [:cljsbuild :builds :app :compiler :output-to]]

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile"
                          ["cljsbuild" "once" "min-public" "min-full"]
                          ["jsass" "once"]]
             :dependencies [[nrepl "0.6.0"]
                            [cider/cider-nrepl "0.21.1"]
                            [org.clojure/tools.nrepl "0.2.13"]
                            [com.billpiel/sayid "0.0.17"]]
             :cljsbuild
             {:builds
              {:min-public
               {:source-paths ["src/clj" "src/cljc" "src/cljs/app"]
                :compiler
                {:main "vetd-app.app"
                 :output-dir "target/cljsbuild/public/js/public-out"
                 :output-to "target/cljsbuild/public/js/app.js"
                 :source-map "target/cljsbuild/public/js/app.js.map"
                 :optimizations :advanced
                 :pretty-print false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs ["src/js/externs.js"]}}
               :min-full
               {:source-paths ["src/clj" "src/cljc" "src/cljs/admin" "src/cljs/app"]
                :compiler
                {:main "vetd-admin.full"
                 :output-dir "target/cljsbuild/public/js/full-out"
                 :output-to "target/cljsbuild/public/js/full.js"
                 :source-map "target/cljsbuild/public/js/full.js.map"
                 :optimizations :advanced
                 :pretty-print false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs ["src/js/externs.js"]}}}}
             :aot :all
             :uberjar-name "vetd-app.jar"
             :resource-paths ["env/prod/resources"]}

   ;; production build
   :build { ;; This is for env vars that are needed during the actual build.
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
                                 [ring/ring-mock "0.3.2"]
                                 ;; A ClojureScript wrapper for Mocha.
                                 ;;
                                 ;; Provides (describe ,,,), (it ,,,), etc.
                                 [mocha-latte "0.1.2"]
                                 ;; A ClojureScript wrapper for Chai.
                                 ;;
                                 ;; Provides (expect ,,,).
                                 [chai-latte "0.2.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.23.0"]
                                 [lein-doo "0.1.10"]
                                 [lein-figwheel "0.5.18"]]
                  :cljsbuild
                  {:builds
                   [{:id "dev-full"
                     :source-paths ["src/clj" "src/cljc" "src/cljs/admin" "src/cljs/app"]
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
                      :preloads [devtools.preload day8.re-frame-10x.preload]}}
                    {:id "cypress"
                     :source-paths ["test/cljs/vetd-app"]
                     ;; Note from @eerohele:
                     ;; If you want :optimizations :none for faster
                     ;; compilation, the only solution I could come up with
                     ;; was to serve the Google Closure assets from the
                     ;; application web server (that is, from http://localhost:3000/js/test).
                     ;;
                     ;; If you can live with :optimizations :simple, you
                     ;; can omit the weird :asset-path.
                     :compiler     {:optimizations :none
                                    :main          vetd-app.core-test
                                    :output-to     "main.js"
                                    :output-dir    "resources/public/js/test"
                                    :asset-path    "http://localhost:3000/js/test"}}]}
                  :doo {:build "test"}
                  :source-paths ["dev/clj"]
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
                    {:compiler
                     {:output-to "target/test.js"
                      :main "vetd-app.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}}
   :profiles/dev {}
   :profiles/test {}})
