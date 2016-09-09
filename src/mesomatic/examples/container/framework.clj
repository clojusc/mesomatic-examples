(ns mesomatic.examples.container.framework
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.async.scheduler :as async-scheduler]
            [mesomatic.scheduler :as scheduler :refer [scheduler-driver]]
            [mesomatic.types :as types]
            [clojusc.twig :refer [pprint]]
            [mesomatic.examples.container.executor :as example-executor]
            [mesomatic.examples.standard.offers :as offers]
            [mesomatic.examples.standard.task :as task]
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

(def framework-info-map {:name "Example Container Framework (Clojure)"
                         :principal "test-framework-clojure"
                         :checkpoint true})
(def limits
  "Note that :max-tasks gets set via an argument passed to the `run` function."
  {:cpus-per-task 1
   :mem-per-task 128
   :max-tasks nil})

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Payload utility functions
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; These are intended to make the callbacks below easier to read, while
;;; providing a little buffer around data and implementation: if (when) the
;;; Mesos messaging API/data structure changes (again), only the functions
;;; below will need to be changed (you won't have to dig through the rest of
;;; the code looking for data structures to update).

(defn get-framework-id
  ""
  [payload]
  (get-in payload [:framework-id :value]))

(defn get-offers
  ""
  [payload]
  (get-in payload [:offers]))

(defn get-error-msg
  ""
  [payload]
  (let [msg (get-in payload [:status :message])]
    (cond
      (empty? msg) (name (get-in payload [:status :reason]))
      :true msg)))

(defn get-master-info
  ""
  [payload]
  (:master-info payload))

(defn get-offer-id
  ""
  [payload]
  (:offer-id payload))

(defn get-status
  ""
  [payload]
  (:status payload))

(defn get-state
  ""
  [payload]
  (name (get-in payload [:status :state])))

(defn healthy?
  ""
  [payload]
  (get-in payload [:status :healthy]))

(defn get-executor-id
  ""
  [payload]
  (get-in payload [:executor-id :value]))

(defn get-slave-id
  ""
  [payload]
  (get-in payload [:slave-id :value]))

(defn get-message
  ""
  [payload]
  (:message payload))

(defn get-bytes
  ""
  [payload]
  (.toStringUtf8 (get-in payload [:status :data])))

(defn log-framework-msg
  ""
  [framework-id executor-id slave-id payload]
  (let [bytes (String. (:data payload))
        log-type? (partial string/includes? bytes)]
    (cond
      (log-type? "TRACE") (log/trace bytes)
      (log-type? "DEBUG") (log/debug bytes)
      (log-type? "INFO") (log/info bytes)
      (log-type? "WARN") (log/warn bytes)
      (log-type? "ERROR") (log/error bytes)
      :else (log/infof
              "Framework %s got message from executor %s (slave=%s): %s"
              framework-id executor-id slave-id bytes))))

(defn get-task-state
  ""
  [payload]
  (get-in payload [:status :state]))

(defn get-task-id
  ""
  [payload]
  (get-in payload [:status :task-id :value]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; State utility functions
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;;
;;; These are intended to make the callbacks below easier to read, while
;;; providing a little buffer around data and implementation of our own state
;;; data structure.

(defn get-driver
  ""
  [state]
  (:driver state))

(defn get-channel
  ""
  [state]
  (:channel state))

(defn get-exec-info
  ""
  [state]
  (:exec-info state))

(defn get-max-tasks
  ""
  [state]
  (get-in state [:limits :max-tasks]))

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; General utility functions
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn do-unhealthy-status
  ""
  [state-name state payload]
  (log/debug "Doing unhealthy check ...")
  (do
    (log/errorf "%s - %s"
                state-name
                (get-error-msg payload))
    (a/close! (get-channel state))
    (scheduler/stop! (get-driver state))
    (util/finish :exit-code 127)
    state))

(defn check-task-finished
  ""
  [state payload]
  (if (= (get-task-state payload) :task-finished)
    (let [task-count (inc (:launched-tasks state))
          new-state (assoc state :launched-tasks task-count)]
      (log/debug "Incremented task-count:" task-count)
      (log/info "Tasks finished:" task-count)
      (if (>= task-count (get-max-tasks state))
        (do
          (scheduler/stop! (get-driver state))
          (util/finish :exit-code 0)
          new-state)
        new-state))
    state))

(defn check-task-abort
  ""
  [state payload]
  (if (or (= (get-task-state payload) :task-lost)
          (= (get-task-state payload) :task-killed)
          (= (get-task-state payload) :task-failed))
    (let [status (:status payload)]
      (log/errorf (str "Aborting because task %s is in unexpected state %s "
                       "with reason %s from source %s with message '%s'")
                  (get-task-id payload)
                  (:state status)
                  (:reason status)
                  (:source status)
                  (:message status))
      (scheduler/abort! (get-driver state))
      (util/finish :exit-code 127)
      state)
    state))


(defn do-healthy-status
  ""
  [state payload]
  (log/debug "Doing healthy check ...")
  (-> state
      (check-task-finished payload)
      (check-task-abort payload)))

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
  (let [master-info (get-master-info payload)
        framework-id (get-framework-id payload)
        exec-info (example-executor/docker-info-map
                    master-info framework-id (util/cwd))]
    (log/info "Registered with framework id:" framework-id)
    (log/trace "Got master info:" (pprint master-info))
    (log/trace "Got state:" (pprint state))
    (log/trace "Got exec info:" (pprint exec-info))
    (assoc state :exec-info exec-info
                 :master-info master-info
                 :framework-id {:value framework-id})))

(defmethod handle-msg :disconnected
  [state payload]
  (log/infof "Framework %s disconnected." (get-framework-id payload))
  state)

(defmethod handle-msg :resource-offers
  [state payload]
  (log/info "Handling :resource-offers message ...")
  (log/trace "Got state:" (pprint state))
  (let [offers-data (get-offers payload)
        offer-ids (offers/get-ids offers-data)
        tasks (offers/process-all state payload offers-data)
        driver (get-driver state)]
    (log/trace "Got offers data:" offers-data)
    (log/trace "Got offer IDs:" (map :value offer-ids))
    (log/trace "Got other payload:" (pprint (dissoc payload :offers)))
    (log/debug "Created tasks:"
               (string/join ", " (map task/get-pb-name tasks)))
    (log/tracef "Got payload for %d task(s): %s"
                (count tasks)
                (pprint (into [] (map pprint tasks))))
    (log/info "Launching tasks ...")
    (scheduler/accept-offers
      driver
      offer-ids
      [{:type :operation-launch
        :tasks tasks}])
    (assoc state :offers offers-data :tasks tasks)))

(defmethod handle-msg :status-update
  [state payload]
  (let [status (get-status payload)
        state-name (get-state payload)]
    (log/infof "Handling :status-update message with state '%s' ..."
               state-name)
    (log/trace "Got state:" (pprint state))
    (log/trace "Got status:" (pprint status))
    (log/trace "Got status info:" (pprint payload))
    (scheduler/acknowledge-status-update (get-driver state) status)
    (if-not (healthy? payload)
      (do-unhealthy-status state-name state payload)
      (do-healthy-status state payload))))

(defmethod handle-msg :disconnected
  [state payload]
  (log/infof "Framework %s disconnected." (get-framework-id payload))
  state)

(defmethod handle-msg :offer-rescinded
  [state payload]
  (let [framework-id (get-framework-id state)
        offer-id (get-offer-id payload)]
    (log/infof "Offer %s rescinded from framework %s."
               offer-id (get-framework-id payload))
    state))

(defmethod handle-msg :framework-message
  [state payload]
  (let [framework-id (get-framework-id state)
        executor-id (get-executor-id payload)
        slave-id (get-slave-id payload)]
    (log-framework-msg framework-id executor-id slave-id payload)
    state))

(defmethod handle-msg :slave-lost
  [state payload]
  (let [slave-id (get-slave-id payload)]
    (log/error "Framework %s lost connection with slave %s."
               (get-framework-id payload)
               slave-id)
    state))

(defmethod handle-msg :executor-lost
  [state payload]
  (let [executor-id (get-executor-id payload)
        slave-id (get-slave-id payload)
        status (get-status payload)]
    (log/errorf (str "Framework lost connection with executor %s (slave=%s) "
                     "with status code %s.")
                executor-id slave-id status)
    state))

(defmethod handle-msg :error
  [state payload]
  (let [message (get-message payload)]
    (log/error "Got error message: " message)
    (log/debug "Data:" (pprint payload))
    state))

(defmethod handle-msg :default
  [state payload]
  (log/warn "Unhandled message: " (pprint payload))
  state)

;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
;;; Framework entrypoint
;;; >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

(defn run
  "This is the function that actually runs the framework."
  [master task-count]
  (log/info "Running example container framework ...")
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
    (a/reduce handle-msg {:driver driver
                          :channel ch
                          :exec-info nil
                          :launched-tasks 0
                          :limits (assoc limits :max-tasks task-count)} ch)
    (scheduler/join! driver)))
