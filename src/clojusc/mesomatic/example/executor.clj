(ns clojusc.mesomatic.example.executor
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.tools.logging :as log]
            [clojusc.twig :refer [pprint]]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.executor :as executor :refer [executor-driver]]
            [mesomatic.types :as types]
            [clojusc.mesomatic.example.util :as util]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Constants and Data
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; In a real application, most of these would be defined in an appropriate
;;; context, using application confgiration values, values extracted from
;;; passed state, etc. This is done for pedagogical purposes only: in an
;;; attempt to keep things clear and clean for the learning experience. Do
;;; not emulate in production code!

(defn info-map
  ""
  []
  {:executor-id {:value (util/get-uuid)}
   :name "Example Executor (Clojure)"})

(defn cmd-info-map
  ""
  [master-info framework-id]
  (into
    (info-map)
    {:framework-id framework-id
     :command
      {:value "/usr/local/bin/lein"
       :arguments [(format "%s:%s" (:hostname master-info)
                                   (:port master-info))
                   "executor"]
       :shell true}}))

(defn info
  ""
  []
  (types/->pb :CommandInfo (info-map)))

(defn cmd-info
  ""
  [master-info framework-id]
  (types/->pb :CommandInfo (cmd-info-map master-info framework-id)))

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
  (log/info "Running example executor ...")
  (let [ch (chan)
        exec (async-executor/executor ch)
        driver (executor-driver exec)]
    (log/debug "Starting example executor ...")
    (executor/start! driver)
    (log/debug "Reducing over example executor channel messages ...")
    (a/reduce handle-msg {:driver driver} ch)
    (executor/join! driver)))
