(ns mesomatic.examples.hello.executor
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojusc.twig :refer [pprint]]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.executor :as executor :refer [executor-driver]]
            [mesomatic.types :as types]
            [mesomatic.examples.hello.task :as task]
            [mesomatic.examples.util :as util]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Constants and Data
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; In a real application, most of these would be defined in an appropriate
;;; context, using application confgiration values, values extracted from
;;; passed state, etc. This is done for pedagogical purposes only: in an
;;; attempt to keep things clear and clean for the learning experience. Do
;;; not emulate in production code!

(def lein "/usr/local/bin/lein")

(defn info-map
  ""
  []
  {:executor-id (util/get-uuid)
   :name "'Hello, World!' Executor (Clojure)"})

(defn cmd-info-map
  ""
  [master-info framework-id cwd]
  (into
    (info-map)
    {:framework-id {:value framework-id}
     :command {:value (format "cd %s && %s mesomatic %s:%s hello-executor"
                              cwd
                              lein
                              (:hostname master-info)
                              (:port master-info))
               :shell true}}))

(defn get-executor-id
  ""
  [payload]
  (get-in payload [:executor-info :executor-id :value]))

(defn get-task-id
  ""
  [payload]
  (get-in payload [:task :task-id :value]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Utility functions
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn update-task-success
  ""
  [task-id state payload]
  (let [executor-id (get-executor-id payload)
        driver (:driver state)]
    (executor/send-status-update!
      driver
      (task/status-running executor-id task-id))
    (log/info "Running task" task-id)

    ;; This is where one would perform the requested task:
    ;; ...
    (Thread/sleep (rand-int 500))
    ;; ...
    ;; Task complete.

    (executor/send-status-update!
      driver
      (task/status-finished executor-id task-id))
    (log/info "Finished task " task-id)))

(defn update-task-fail
  ""
  [task-id e state payload]
  (let [executor-id (get-executor-id payload)]
    (log/errorf "Got exception for task %s: %s"
                task-id (pprint e))
    (executor/send-status-update!
      (:driver state)
      (task/status-failed executor-id task-id))
    (log/infof "Task %s failed" task-id)))

(defn run-task
  ""
  [task-id state payload]
  (try
    (update-task-success task-id state payload)
    (catch Exception e
      (update-task-fail task-id e state payload))))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework callbacks
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; Note that these are not callbacks in the node.js or even Twisted (Python)
;;; sense of the word; they are like Erlang OTP callbacks. For more
;;; information on the distinguishing characteristics, take a look at Joe
;;; Armstrong's blog post on Red/Green Callbacks:
;;;  * http://joearms.github.io/2013/04/02/Red-and-Green-Callbacks.html

(defmulti handle-msg (comp :type last vector))

(defmethod handle-msg :registered
  [state payload]
  state)

(defmethod handle-msg :reregistered
  [state payload]
  state)

(defmethod handle-msg :disconnected
  [state payload]
  state)

(defmethod handle-msg :launch-task
  [state payload]
  (let [task-id (get-task-id payload)]
    (log/infof "Launching task %s ..." task-id)
    (log/debug "Task id:" task-id)
    (-> (run-task task-id state payload)
        (Thread.)
        (.start))
    state))

(defmethod handle-msg :kill-task
  [state payload]
  state)

(defmethod handle-msg :framework-message
  [state payload]
  state)

(defmethod handle-msg :shutdown
  [state payload]
  state)

(defmethod handle-msg :error
  [state payload]
  (log/error "Error in executor: " (pprint payload))
  state)

(defmethod handle-msg :default
  [state payload]
  (log/warn "Unhandled message: " (pprint payload))
  state)

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Executor entrypoint
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn run
  ""
  [master]
  (log/infof "Running hello-world executor from %s ..." (util/cwd))
  (let [ch (chan)
        exec (async-executor/executor ch)
        driver (executor-driver exec)]
    (log/debug "Starting example executor ...")
    (executor/start! driver)
    (log/debug "Reducing over example executor channel messages ...")
    (a/reduce handle-msg {:driver driver
                          :ch ch} ch)
    (executor/join! driver)))
