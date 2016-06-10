(ns clojusc.mesomatic.example.framework
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.tools.logging :as log]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.async.scheduler :as async-scheduler]
            [mesomatic.scheduler :as scheduler :refer [scheduler-driver]]
            [mesomatic.types :as types]
            [clojusc.twig :refer [pprint]]
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

(def framework-info {:name "'Hello, World!' Framework (Clojure)"})
(def rsrcs (util/make-rsrcs :cpus 0.2 :mem 128.0))
(def cmd {:shell false
          :container nil
          ;:environment (util/make-env)
          :value "/usr/local/bin/lein"
          :arguments ["mesomatic" "127.0.0.1:5050" "executor"]})
(def executor-info {:name "'Hello, World!' Executor"
                    :resources rsrcs
                    :command cmd})
(def task-info {:name "'Hello, World!' Task"
                :count 1
                :maxcol 1
                :resources rsrcs
                :command cmd
                })

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
  (log/debug "Got master info:" (pprint (:master-info data)))
  state)

(defmethod handle-msg :resource-offers
  [state data]
  (log/info "Updating offers with" (count (:offers data)) "new offers ...")
  (log/debug "Got offer id:" (get-in data [:offers 0 :id :value]))
  (log/debug "Got offer info:" (pprint (:offers data)))
  (log/debug "Got other data:" (pprint (dissoc data :offers)))
  (log/debug "Got state:" (pprint state))
  (let [offer-id (get-in data [:offers 0 :id])
        framework-id (get-in data [:offers 0 :framework-id])
        agent-id (get-in data [:offers 0 :slave-id])
        task-id (util/get-uuid)
        executor-id (util/get-uuid)
        exec-info (assoc executor-info
                    :executor-id executor-id
                    :framework-id framework-id)
        task (assoc task-info
               :slave-id agent-id
               :slave agent-id
               :task-id [task-id]
               ;:executor exec-info
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
  (log/info "Running framework ...")
  (let [ch (chan)
        sched (async-scheduler/scheduler ch)
        driver (scheduler-driver sched framework-info master)]
    (log/debug "Starting scheduler ...")
    (scheduler/start! driver)
    (log/debug "Reducing over scheduler channel messages ...")
    (a/reduce handle-msg {:driver driver :channel ch} ch)
    (scheduler/join! driver)))
