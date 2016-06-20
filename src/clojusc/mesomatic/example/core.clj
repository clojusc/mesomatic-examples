(ns clojusc.mesomatic.example.core
  "The namespace which holds the entry point for the example."
  (:require [clojure.core.async :refer [chan <! go] :as a]
            [clojure.tools.logging :as log]
            [leiningen.core.project :as lein-prj]
            [clojusc.twig :as logger]
            [clojusc.mesomatic.example.exception-framework :as ex-framework]
            [clojusc.mesomatic.example.executor :as executor]
            [clojusc.mesomatic.example.framework :as framework])
  (:gen-class))

(defn get-config
  "Read the ``mesomatic-example`` config data from ``project.clj``."
  []
  (:mesomatic-example (lein-prj/read)))

(defn -main
  "It is expected that this function be called from ``lein`` in the following
  manner:

  ```
  $ lein mesomatic 127.0.0.1:5050 <task type>
  ```

  where ``<task-type>`` is one of:

  * ``executor``
  * ``framework``

  That being said, only Mesos should call with the ``executor`` task type;
  calling humans will only call with the ``framework`` task type.

  Note that in order for this to work, one needs to add the following alias to
  the project's ``project.clj``:

  ```clj
  :aliases {\"mesomatic\" [\"run\" \"-m\" \"mesomatic-example.core\"]}
  ```
  "
  [master task-type]
  (let [cfg (get-config)]
    (logger/set-level! (:log-namespaces cfg) (:log-level cfg))
    (log/info "Running mesomatic-example!")
    (log/debug "Using master:" master)
    (log/debug "Got task-type:" task-type)
    (condp = task-type
      "executor" (executor/run master)
      "framework" (framework/run master)
      "exception-framework" (ex-framework/run master))))
