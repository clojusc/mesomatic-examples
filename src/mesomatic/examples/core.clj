(ns mesomatic.examples.core
  "The namespace which holds the entry point for the Mesomatic examples."
  (:require [clojure.core.async :refer [chan <! go] :as a]
            [clojure.tools.logging :as log]
            [leiningen.core.project :as lein-prj]
            [clojusc.twig :as logger]
            [mesomatic.examples.container.executor :as cntnr-executor]
            [mesomatic.examples.container.framework :as cntnr-framework]
            [mesomatic.examples.hello.executor :as hi-executor]
            [mesomatic.examples.hello.framework :as hi-framework]
            [mesomatic.examples.standard.executor :as std-executor]
            [mesomatic.examples.standard.framework :as std-framework]
            [mesomatic.examples.exception-only.framework :as excp-framework]
            [mesomatic.examples.util :as util])
  (:gen-class))

(defn get-config
  "Read the ``mesomatic-examples`` config data from ``project.clj``."
  []
  (:mesomatic-examples (lein-prj/read)))

(defn usage
  ""
  []
  (println)
  (-> 'mesomatic.examples.core
      (util/get-docstring '-main)
      (println)))

(defn -main
  "It is expected that this function be called from ``lein`` in one of the
  two following manners:

  ```
  $ lein mesomatic 127.0.0.1:5050 <task type>
  ```

  or

  ```
  $ lein mesomatic 127.0.0.1:5050 <task type> <task count>
  ```

  where ``<task-type>`` is one of:

  * ``executor``
  * ``framework``
  * ``container-framework``
  * ``exception-framework``
  * ``hello-framework``

  and ``<task count>`` is an integer representing the number of times a task
  will be run. If a task count is not provided, a default value of `5` is
  used instead.

  That being said, only Mesos should call with the ``executor`` task type;
  calling humans will only call with the ``framework`` task type.

  Note that in order for this to work, one needs to add the following alias to
  the project's ``project.clj``:

  ```clj
  :aliases {\"mesomatic\" [\"run\" \"-m\" \"mesomatic-examples.core\"]}
  ```
  "
  ([flag]
    (case flag
      "--help" (usage)
      "-h" (usage)))
  ([master task-type]
    (-main master task-type 5))
  ([master task-type task-count]
    (let [cfg (get-config)]
      (logger/set-level! (:log-namespaces cfg) (:log-level cfg))
      (log/info "Running a mesomatic example!")
      (log/debug "Using master:" master)
      (log/debug "Got task-type:" task-type)
      (condp = task-type
        "executor" (std-executor/run master)
        "container-executor" (cntnr-executor/run master)
        "container-framework" (cntnr-framework/run master task-count)
        "framework" (std-framework/run master task-count)
        "exception-framework" (excp-framework/run master)
        "hello-executor" (hi-executor/run master)
        "hello-framework" (hi-framework/run master)))))
