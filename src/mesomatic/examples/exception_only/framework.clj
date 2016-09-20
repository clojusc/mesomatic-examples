(ns mesomatic.examples.exception-only.framework
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.async.scheduler :as async-scheduler]
            [mesomatic.scheduler :as scheduler :refer [scheduler-driver]]
            [mesomatic.types :as types]
            [clojusc.twig :refer [pprint]]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Constants and Data
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; In a real application, most of these would be defined in an appropriate
;;; context, using application confgiration values, values extracted from
;;; passed state, etc. This is done for pedagogical purposes only: in an
;;; attempt to keep things clear and clean for the learning experience. Do
;;; not emulate in production code!

(def framework-info-map {:name "Example Exception Framework (Clojure)"
                         :principal "test-exception-framework-clojure"
                         :checkpoint true})
(def limits
  {:cpus-per-task 1
   :mem-per-task 128})

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

(defmethod handle-msg :registered
  [state payload]
  (log/warn "Preparing to throw exception:")
  (throw (Exception. "Throwing an exception ..."))
  state)

(defmethod handle-msg :disconnected
  [state payload]
  state)

(defmethod handle-msg :resource-offers
  [state payload]
  state)

(defmethod handle-msg :status-update
  [state payload]
  state)

(defmethod handle-msg :disconnected
  [state payload]
  state)

(defmethod handle-msg :offer-rescinded
  [state payload]
  state)

(defmethod handle-msg :framework-message
  [state payload]
  state)

(defmethod handle-msg :slave-lost
  [state payload]
  state)

(defmethod handle-msg :executor-lost
  [state payload]
  state)

(defmethod handle-msg :error
  [state payload]
  state)

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework entrypoint
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn wrap-handler
  ""
  [state payload]
  (try
    (handle-msg state payload)
    (catch Exception e
      (log/error "Got error:" (.getMessage e))
      (scheduler/abort! (:driver state))
      (reduced
        (assoc state :error e)))))

(defn run
  "This is the function that actually runs the framework."
  [master]
  (log/info "Running example exception-framework ...")
  (let [ch (chan)
        sched (async-scheduler/scheduler ch)
        driver (scheduler-driver sched
                                 framework-info-map
                                 master
                                 nil
                                 false)]
    (log/debug "Starting example scheduler ...")
    (scheduler/start! driver)
    (log/debug "Reducing over example scheduler channel messages ...")
    (a/reduce wrap-handler {:driver driver
                            :channel ch
                            :exec-info nil} ch)
    (scheduler/join! driver)))
