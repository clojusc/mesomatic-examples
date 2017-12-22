(ns mesomatic.examples.core
  "The namespace which holds the entry point for the Mesomatic examples."
  (:require
    [clojure.core.async :refer [chan <! go] :as a]
    [clojusc.twig :as logger]
    [leiningen.core.project :as lein-prj]
    [mesomatic.examples.bash-scheduler.framework :as bash-scheduler]
    [mesomatic.examples.container-cmd.executor :as cntnrcmd-executor]
    [mesomatic.examples.container-cmd.framework :as cntnrcmd-framework]
    [mesomatic.examples.container.executor :as cntnr-executor]
    [mesomatic.examples.container.framework :as cntnr-framework]
    [mesomatic.examples.exception-only.framework :as excp-framework]
    [mesomatic.examples.hello.executor :as hi-executor]
    [mesomatic.examples.hello.framework :as hi-framework]
    [mesomatic.examples.scheduler-only.framework :as scheduler-only-framework]
    [mesomatic.examples.standard.executor :as std-executor]
    [mesomatic.examples.standard.framework :as std-framework]
    [mesomatic.examples.util :as util]
    [taoensso.timbre :as log]
    [trifl.docs :as docs])
  (:gen-class))

(defn get-config
  "Read the `mesomatic-examples` config data from `project.clj`."
  []
  (:mesomatic-examples (lein-prj/read)))

(defn -main
  "
  Usage:
  ```
    lein mesomatic MASTER TASK-TYPE [TASK-COUNT]
    lein mesomatic [-h | --help]
  ```

  `TASK-TYPE` is one of the several options checked in this function's
  `case` statement (see the source code for the available options). Note that
  some of these task types are executors -- these are only intended to be
  called directly by Mesos.

  `TASK-COUNT` is an integer representing the number of times a task
  will be run. If a task count is not provided, a default value of `5` is
  used instead.

  Examples:
  ```
    $ lein mesomatic 127.0.0.1:5050 scheduler-only-framework
  ```

  or

  ```
    $ lein mesomatic 172.17.0.3:5050 framework 2
  ```"
  ([]
    (docs/print-docstring #'-main))
  ([_flag]
    (docs/print-docstring #'-main))
  ([master task-type]
    (-main master task-type 5))
  ([master task-type task-count]
    (let [cfg (get-config)]
      (logger/set-level! (:log-namespaces cfg) (:log-level cfg))
      (log/info "Running a mesomatic example ...")
      (log/debug "Using master:" master)
      (log/debug "Got task-type:" task-type)
      (case (keyword task-type)
        :bash-scheduler-framework (bash-scheduler/run master)
        :container-cmd-executor (cntnrcmd-executor/run master)
        :container-cmd-framework (cntnrcmd-framework/run master task-count)
        :container-executor (cntnr-executor/run master)
        :container-framework (cntnr-framework/run master task-count)
        :exception-framework (excp-framework/run master)
        :executor (std-executor/run master)
        :framework (std-framework/run master task-count)
        :hello-executor (hi-executor/run master)
        :hello-framework (hi-framework/run master)
        :scheduler-only-framework (scheduler-only-framework/run master)))))
