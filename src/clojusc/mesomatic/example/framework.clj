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
            [clojusc.mesomatic.example.util :as util]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Constants
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; In a real application, most of these would be defined in an appropriate
;;; context, using application confgiration values, values extracted from
;;; passed state, etc. This is done for pedagogical purposes only: in an
;;; attempt to keep things clear and clean for the learning experience. Do
;;; not emulate in production code!

(def framework-info {:name "Example Framework (Clojure)"})
(def task-info {:name "Example Task"
                :count 1
                :maxcol 1
                :resources (util/make-rsrcs :cpus 0.2 :mem 128.0)})

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
  (log/info "Registered with framework id:" (get-in data [:framework-id :value]))
  (log/trace "Got master info:" (pprint (:master-info data)))
  state)

(defmethod handle-msg :resource-offers
  [state data]
  (log/info "Updating offers with" (count (:offers data)) "new offers ...")
  (log/debug "Got offer id:" (get-in data [:offers 0 :id :value]))
  (log/trace "Got offer info:" (pprint (:offers data)))
  (log/trace "Got other data:" (pprint (dissoc data :offers)))
  (log/trace "Got state:" (pprint state))
  (let [offer-id (get-in data [:offers 0 :id])
        framework-id (get-in data [:offers 0 :framework-id])
        agent-id (get-in data [:offers 0 :slave-id])
        task-id (util/get-uuid)
        exec-info (example-executor/cmd-info (:master state) framework-id)
        task (assoc task-info
               :slave-id agent-id
               :slave agent-id
               :task-id [task-id]
               :executor exec-info
               )
        tasks [(types/map->TaskInfo task)]]
    (log/debug "Built tasks:" (pprint tasks))
    (log/infof "Launching tasks with offer-id '%s'..." (:value offer-id))
    (scheduler/launch-tasks! (:driver state) offer-id tasks)
    ;(assoc state :offers (:offers data) :tasks tasks)))
    (assoc state :offers (:offers data))))

(defn get-error-msg
  ""
  [data]
  (let [msg (get-in data [:status :message])]
    (cond
      (empty? msg) (get-in data [:status :reason])
      :true msg)))

(defmethod handle-msg :status-update
  [state data]
  (log/debug "Got status info:" (pprint data))
  (if-not (get-in data [:status :healhty])
    (do
      (log/errorf "%s - %s"
                  (name (get-in data [:status :state]))
                  (name (get-error-msg data)))
      (log/debug (pprint (keys state)))
      (a/close! (:channel state))
      (scheduler/stop! (:driver state))))
  state)

(defmethod handle-msg :executor-lost
  [state data]
  (log/error "Could not communicate with specified executor")
  (log/debug "State:" (pprint state))
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
        driver (scheduler-driver sched framework-info master)]
    (log/debug "Starting example scheduler ...")
    (scheduler/start! driver)
    (log/debug "Reducing over example scheduler channel messages ...")
    (a/reduce handle-msg {:driver driver :channel ch :master master} ch)
    (scheduler/join! driver)))
