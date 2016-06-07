(ns clojusc.mesomatic.hello.core
  "The namespace which holds the entry point for the hello-world demo."
  (:require [clojure.core.async :refer [chan <! go] :as a]
            [clojure.tools.logging :as log]
            [twig.core :as logger]
            [leiningen.core.project :as lein-prj]
            [clojusc.mesomatic.hello.executor :as mmh-executor]
            [clojusc.mesomatic.hello.framework :as mmh-framework])
  (:gen-class))

(defn get-config
  "Read the ``mesomatic-hello`` config data from ``project.clj``."
  []
  (:mesomatic-hello (lein-prj/read)))

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
  :aliases {\"mesomatic\" [\"run\" \"-m\" \"mesomatic-hello.core\"]}
  ```
  "
  [master task-type]
  (logger/set-level! '[clojusc.mesomatic.hello] (:log-level (get-config)))
  (log/info "Running mesomatic-hello!")
  (log/debug "Using master:" master)
  (log/debug "Got task-type:" task-type)
  (condp = task-type
    "executor" (mmh-executor/run master)
    "framework" (mmh-framework/run master)))
