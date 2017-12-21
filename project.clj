(defproject mesomatic-examples "0.1.0-SNAPSHOT"
  :description "Mesos Examples Using Mesomatic"
  :url "https://github.com/clojusc/mesomatic-examples"
  :license {
    :name "Eclipse Public License"
    :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [
    [clojusc/twig "0.2.6"]
    [com.stuartsierra/component "0.3.1"]
    [leiningen-core "2.7.1"]
    [org.clojure/clojure "1.8.0"]
    [spootnik/mesomatic "1.0.1-r0"]
    [spootnik/mesomatic-async "1.0.1-r0"]]
  :plugins [
    [lein-codox "0.10.1"]
    [lein-simpleton "1.3.0"]]
  :mesomatic-examples {
    :log-namespaces [mesomatic.examples]
    :log-level :trace}
  :codox {
    :project {
      :name "mesomatic-examples"
      :description "A Clojure/Meosmatic port of the Mesos Java examples"}
    :namespaces [#"^mesomatic.examples\."]
    :output-path "docs/build"
    :doc-paths ["docs"]
    :metadata {
      :doc/format :markdown
      :doc "Documentation forthcoming"}}
  :profiles {
    :dev {
      :source-paths ["dev-resources/src"]
      :repl-options {:init-ns mesomatic.examples.dev}}
    :test {
      :exclusions [org.clojure/clojure]
      :dependencies [
        [clojusc/ltest "0.3.0-SNAPSHOT"]]
      :plugins [
        [jonase/eastwood "0.2.5"]
        [lein-ancient "0.6.15"]
        [lein-bikeshed "0.5.0"]
        [lein-kibit "0.1.6"]
        [lein-ltest "0.3.0-SNAPSHOT"]
        [venantius/yagni "0.1.4"]]}
    :ubercompile {
      :aot :all}
    :custom-repl {
      :repl-options {
        :init-ns mesomatic.examples.dev
        :prompt ~#(str "\u001B[35m[\u001B[34m"
                       %
                       "\u001B[35m]\u001B[33m λ\u001B[m=> ")}}}
  :aliases {
    "mesomatic" ^{:doc (str "Command line interface for mesomatic-examples. "
                            "For more info run `lein mesomatic --help`\n")}
                ^:pass-through-help
                ["run" "-m" "mesomatic.examples.core"]
    "repl" ["with-profile" "+custom-repl,+test" "repl"]
    "ubercompile" ["with-profile" "+ubercompile" "compile"]
    "check-deps" ["with-profile" "+test" "ancient" "check" ":all"]
    "lint" ["with-profile" "+test" "kibit"]
    "test" ["with-profile" "+test" "ltest"]
    "build-check" ["with-profile" "+test" "do"
      ;["check-deps"]
      ["lint"]
      ["ubercompile"]
      ["clean"]
      ["uberjar"]
      ["clean"]
      ["test"]]})
