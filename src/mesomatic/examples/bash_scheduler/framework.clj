(ns mesomatic.examples.bash-scheduler.framework
  (:require
    [clojure.core.async :as async]
    [clojusc.twig :refer [pprint]]
    [mesomatic.async.scheduler :as async-scheduler]
    [mesomatic.scheduler :as scheduler :refer [scheduler-driver]]
    [taoensso.timbre :as log]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Constants and Data
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(def framework-info-map {:name "Bash Scheduler (Clojure)"})

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework callbacks
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; Note that these are not callbacks in the node.js or even Twisted (Python)
;;; sense of the word; they are like Erlang OTP callbacks. For more
;;; information on the distinguishing characteristics, take a look at Joe
;;; Armstrong's blog post on Red/Green Callbacks:
;;;  * http://joearms.github.io/2013/04/02/Red-and-Green-Callbacks.html

(defmulti handle-msg
  "This is a custom multimethod for handling messages that are received on the
  async scheduler channel.

  Note that:

  * though the methods are associated with types whose names match the
    scheduler API, these functions and those are quite different and do not
    accept the same parameters
  * each handler's callback (below) only takes two parameters:
     1. state that gets passed to successive calls (if returned by the handler)
     2. the payload that is sent to the async channel by the scheduler API
  * as such, if there is something in a message which you would like to persist
    or have access to in other functions, you'll need to assoc it to state."
  (comp :type last vector))

(defmethod handle-msg :default
  [state payload]
  (log/info "Scheduler message from Mesos: " (pprint payload))
  state)

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework entrypoint
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn run
  "This is the function that actually runs the framework."
  [master]
  (log/info "Running example framework ...")
  (let [ch (async/chan)
        sched (async-scheduler/scheduler ch)
        driver (scheduler-driver sched
                                 framework-info-map
                                 master
                                 nil
                                 false)]
    (log/debug "Starting example scheduler ...")
    (scheduler/start! driver)
    (log/debug "Reducing over example scheduler channel messages ...")
    (async/reduce handle-msg
                  {:driver driver :channel ch}
                  ch)
    (scheduler/join! driver)))
