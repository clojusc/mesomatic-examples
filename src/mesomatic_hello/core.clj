(ns mesomatic-hello.core
  "The namespace which holds the entry point for the hello-world demo."
  (:require [clojure.core.async :refer [chan <! go] :as a]
            [clojure.tools.logging :as log]
            [twig.core :as twig]
            [mesomatic.allocator :as alloc]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.async.scheduler :as async-scheduler]
            [mesomatic.executor :as executor]
            [mesomatic.scheduler :as scheduler]
            [mesomatic.types :as types]
            [mesomatic-hello.executor :as mmh-exec]
            [mesomatic-hello.scheduler :as mmh-sched])
  (:gen-class))


(defn run-exec
  ""
  []
  (log/info "Running executor ...")
  )

(defn run-sched
  ""
  []
  (log/info "Running scheduler ...")
  )

(defn run-framework
  ""
  []
  (log/info "Running framework ...")
  )

(defn -main
  "It is expected that this function be called from ``lein`` in the following
  manner:

  ```
  $ lein mesomatic 127.0.0.1:5050 <task type>
  ```

  where ``<task-type>`` is one of:

  * ``executor``
  * ``scheduler``
  * ``framework``
  "
  [host-port task-type]
  (twig/set-level! '[mesomatic-hello] :debug)
  (log/debug "Got host-port:" host-port)
  (log/debug "Got task-type:" task-type)
  (condp = task-type
    "executor" (run-exec)
    "scheduler" (run-sched)
    "framework" (run-framework)))
