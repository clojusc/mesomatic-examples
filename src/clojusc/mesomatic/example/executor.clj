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
  {:executor-id (util/get-uuid)
   :name "Example Executor (Clojure)"})

(defn cmd-info-map
  ""
  [master-info framework-id cwd]
  (into
    (info-map)
    {:framework-id {:value framework-id}
     :command
      {:value (format "echo; cd %s && echo `pwd`;echo" cwd)
       ;:arguments ["-al"]
       ; :value "cd /home/oubiwann/lab/clojure/mesomatic-example && /usr/local/bin/lein"
       ; :arguments ["mesomatic"
       ;             (format "%s:%s" (:hostname master-info)
       ;                            (:port master-info))
       ;            "executor"]
       ;:environment (util/make-env)
       :shell true}}))

(defn info
  ""
  []
  (types/->pb :ExecutorInfo (info-map)))

(defn cmd-info
  ""
  [master-info framework-id cwd]
  (let [exec-info (cmd-info-map master-info framework-id cwd)]
    (log/debug "exec-info:" (pprint exec-info))
    (types/->pb :ExecutorInfo exec-info)))

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
  (log/info "Registered executor: " (pprint payload))
  state)

(defmethod handle-msg :reregistered
  [state payload]
  (log/info "Reregistered executor: " (pprint payload))
  state)

(defmethod handle-msg :disconnected
  [state payload]
  (log/info "Executor has disconnected: " (pprint payload))
  state)

(defmethod handle-msg :launch-task
  [state payload]
  (log/info "Launching task %s ..." (pprint payload))
  (log/debug "Task payload: " (pprint payload))
  state)

(defmethod handle-msg :kill-task
  [state payload]
  (log/info "Killing task: " (pprint payload))
  state)

(defmethod handle-msg :framework-message
  [state payload]
  (log/info "Got framework message: " (pprint payload))
  state)

(defmethod handle-msg :shutdown
  [state payload]
  (log/info "Shutting down executor: " (pprint payload))
  state)

(defmethod handle-msg :error
  [state payload]
  (log/info "Error in executor: " (pprint payload))
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
  (log/infof "Running example executor from %s..." (util/cwd))
  (let [ch (chan)
        exec (async-executor/executor ch)
        driver (executor-driver exec)]
    (log/debug "Starting example executor ...")
    (executor/start! driver)
    (log/debug "Reducing over example executor channel messages ...")
    (a/reduce handle-msg {:driver driver} ch)
    (executor/join! driver)))
