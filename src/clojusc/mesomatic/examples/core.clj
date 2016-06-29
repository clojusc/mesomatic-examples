(ns clojusc.mesomatic.examples.core
  "The namespace which holds the entry point for the Mesomatic examples."
  (:require [clojure.core.async :refer [chan <! go] :as a]
            [clojure.tools.logging :as log]
            [leiningen.core.project :as lein-prj]
            [clojusc.twig :as logger]
            [clojusc.mesomatic.examples.standard.executor :as std-executor]
            [clojusc.mesomatic.examples.standard.framework :as std-framework]
            [clojusc.mesomatic.examples.exception-only.framework
             :as excp-framework])
  (:gen-class))

(defn get-config
  "Read the ``mesomatic-examples`` config data from ``project.clj``."
  []
  (:mesomatic-examples (lein-prj/read)))

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
  :aliases {\"mesomatic\" [\"run\" \"-m\" \"mesomatic-examples.core\"]}
  ```
  "
  [master task-type]
  (let [cfg (get-config)]
    (logger/set-level! (:log-namespaces cfg) (:log-level cfg))
    (log/info "Running a mesomatic example!")
    (log/debug "Using master:" master)
    (log/debug "Got task-type:" task-type)
    (condp = task-type
      "executor" (std-executor/run master)
      "framework" (std-framework/run master)
      "exception-framework" (excp-framework/run master))))
