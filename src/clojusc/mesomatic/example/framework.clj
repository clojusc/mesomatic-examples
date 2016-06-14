(ns clojusc.mesomatic.example.framework
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.tools.logging :as log]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.async.scheduler :as async-scheduler]
            [mesomatic.scheduler :as scheduler :refer [scheduler-driver]]
            [mesomatic.types :as types]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.example.executor :as example-executor]
            [clojusc.mesomatic.example.offers :as offers]
            [clojusc.mesomatic.example.task :as task]
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

(def framework-info-map {:name "Example Framework (Clojure)"})
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

(defmulti handle-msg (comp :type last vector))

(defmethod handle-msg :registered
  [state data]
  (let [master-info (:master-info data)
        framework-id (util/get-framework-id data)
        exec-info (example-executor/cmd-info-map master-info framework-id)]
    (log/info "Registered with framework id:" framework-id)
    (log/trace "Got master info:" (pprint master-info))
    (log/trace "Got state info:" (pprint state))
    (assoc state :exec-info exec-info :master-info master-info)))

(defmethod handle-msg :disconnected
  [state data]
  (log/infof "Framework %s disconnected." (util/get-framework-id data))
  state)

(defmethod handle-msg :resource-offers
  [state data]
  (log/info "Hanlding :resource-offers message ...")
  (log/trace "Got state:" (pprint state))
  (let [offers-data (util/get-offers data)
        tasks (offers/process-all state data limits offers-data)]
    (log/trace "Got offers data:" offers-data)
    (log/trace "Got other data:" (pprint (dissoc data :offers)))
    (log/debug "Created tasks:" tasks)
    (assoc state :offers offers-data :tasks (into [] tasks))))

(defmethod handle-msg :status-update
  [state data]
  (log/info "Hanlding :status-update message ...")
  (log/debug "Got status info:" (pprint data))
  (if-not (get-in data [:status :healhty])
    (do
      (log/errorf "%s - %s"
                  (name (get-in data [:status :state]))
                  (name (util/get-error-msg data)))
      (log/debug (pprint (keys state)))
      (a/close! (:channel state))
      (scheduler/stop! (:driver state))))
  state)

(defmethod handle-msg :disconnected
  [state data]
  (log/infof "Framework %s disconnected." (util/get-framework-id data))
  state)

(defmethod handle-msg :offer-rescinded
  [state data offer-id]
  (log/infof "Offer %s rescinded from framework %s."
             offer-id (util/get-framework-id data))
  state)

(defmethod handle-msg :framework-message
  [state data executor-id slave-id bytes]
  (log/infof "Framework %s (executor=%s, slave=%s) got message: %s"
             (util/get-framework-id data)
             executor-id slave-id bytes)
  state)

(defmethod handle-msg :slave-lost
  [state data slave-id]
  (log/error "Framework %s lost connection with slave %s."
             (util/get-framework-id data)
             slave-id)
  state)

(defmethod handle-msg :executor-lost
  [state data executor-id slave-id status]
  (log/error "Framework %s lost connection with executor %s (slave=%s): %s"
             (util/get-framework-id data)
             executor-id slave-id status)
  state)

(defmethod handle-msg :error
  [state data message]
  (log/error "Got error message: " message)
  (log/debug "Data:" (pprint data))
  state)

(defmethod handle-msg :default
  [state data]
  (log/warn "Unhandled message: " (pprint data))
  state)

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework entrypoint
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn run
  "This is the function that actually runs the framework."
  [master]
  (log/info "Running example framework ...")
  (let [ch (chan)
        sched (async-scheduler/scheduler ch)
        driver (scheduler-driver sched framework-info-map master)]
    (log/debug "Starting example scheduler ...")
    (scheduler/start! driver)
    (log/debug "Reducing over example scheduler channel messages ...")
    (a/reduce handle-msg {:driver driver :channel ch :exec-info nil} ch)
    (scheduler/join! driver)))
