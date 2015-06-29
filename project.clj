(defproject gtfve "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cljsjs/google-maps "3.18-1"]
                 [cljsjs/react "0.12.2-5"]
                 [liberator "0.12.2"]
                 [garden "1.2.5"]
                 [secretary "1.2.1"]
                 [clj-time "0.9.0"]
                 [com.datomic/datomic-pro "0.9.5130"
                  :exclusions [org.apache.httpcomponents/httpclient joda-time]]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [cljs-ajax "0.3.10"]
                 [org.clojure/clojurescript "0.0-3058" :scope "provided"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.3"]
                 [prone "0.8.0"]
                 [compojure "1.3.2"]
                 [selmer "0.8.0"]
                 [sablono "0.3.4"]
                 [org.omcljs/om "0.8.8"]
                 [environ "1.0.0"]]

  :repositories [["my.datomic.com" {:url "https://my.datomic.com/repo"
                                    :username [:env/datomic_username]
                                    :password [:env/datomic_password]}]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-environ "1.0.0"]
            [lein-ring "0.9.1"]
            [lein-asset-minifier "0.2.2"]]

  :ring {:handler gtfve.handler/app
         :uberwar-name "gtfve.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "gtfve.jar"

  :main gtfve.server

  :clean-targets ^{:protect false} ["resources/public/js"]

  :minify-assets
  {:assets
    {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        ;;:externs       ["react/externs/react.js"]
                                        :asset-path   "js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns gtfve.dev
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.3.2"]
                                  [org.clojure/tools.nrepl "0.2.7"]
                                  [leiningen "2.5.1"]
                                  [figwheel "0.2.5-SNAPSHOT"]
                                  [weasel "0.6.0-SNAPSHOT"]
                                  [com.cemerick/piggieback "0.1.6-SNAPSHOT"]
                                  [pjstadig/humane-test-output "0.6.0"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.2.5-SNAPSHOT"]
                             [lein-garden "0.2.6"]
                             [cider/cider-nrepl "0.9.0-SNAPSHOT"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :nrepl-port 7002
                              :css-dirs ["resources/public/css"]
                              :ring-handler gtfve.handler/app}

                   :garden {:builds [{:id "main"
                                      :source-paths ["src/clj/styles"]
                                      :stylesheet gtfve.styles.core/main
                                      :compiler {:output-to "resources/public/css/main.css"
                                                 :pretty-print? true}}]}

                   :env {:dev? true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "gtfve.dev"
                                                         :source-map true}}
}
}}

             :uberjar {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs"]
                                              :compiler
                                              {:optimizations :advanced
                                               :pretty-print false}}}}}

             :production {:ring {:open-browser? false
                                 :stacktraces?  false
                                 :auto-reload?  false}
                          :cljsbuild {:builds {:app {:compiler {:main "gtfve.prod"}}}}
                          }})
