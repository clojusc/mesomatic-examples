(ns clojusc.mesomatic.hello.executor
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.tools.logging :as log]
            [twig.core :refer [pprint]]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.executor :as executor :refer [executor-driver]]
            [mesomatic.types :as types]))

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

(defmethod handle-msg :launch-task
  [this data]
  (log/info "Launching task %s ..." (pprint data))
  (log/debug "Task data: " (pprint data))
  this)

(defmethod handle-msg :default
  [this data]
  (log/warn "Unhandled message: " (pprint data))
  this)

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Executor entrypoint
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn run
  ""
  [master]
  (log/info "Running executor ...")
  (let [ch (chan)
        exec (async-executor/executor ch)
        driver (executor-driver exec)]
    (log/debug "Starting executor ...")
    (executor/start! driver)
    (log/debug "Reducing over executor channel messages ...")
    (a/reduce handle-msg {:driver driver} ch)
    (executor/join! driver)))
