(defproject mesomatic-hello "0.1.0-dev"
  :description "Clojure and Mesos: A Mesomatic 'Hello World'"
  :url "https://github.com/oubiwann/mesomatic-hello"
  :license {:name "Eclipse Public License"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [twig "0.1.4"]
                 [spootnik/mesomatic "0.27.0-r0"]
                 [spootnik/mesomatic-async "0.27.0-r0"]]
  :plugins [[lein-codox "0.9.1"]
            [lein-simpleton "1.3.0"]]
  :aliases {"mesomatic" ["run" "-m" "mesomatic-hello.core"]}
  :codox {:project {:name "mesomatic-hello"
                    :description "A 'Hello, World' for Clojure and Mesos using Mesomatic"}
          :namespaces [#"^mesomatic-hello\."]
          :output-path "docs/build"
          :doc-paths ["docs"]
          :metadata {:doc/format :markdown
                     :doc "Documentation forthcoming"}})
