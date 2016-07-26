(ns mesomatic.examples.hello.task
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [mesomatic.types :as types]
            [clojusc.twig :refer [pprint]]
            [mesomatic.examples.common.resources :as resources]
            [mesomatic.examples.util :as util]))

(def task-info-map {:name "Example Task %d (Clojure)"
                    :count 1
                    :maxcol 1})

;;; Task

(defn make-map
  ""
  [state data index offer]
  (into task-info-map
        {:name (format (:name task-info-map) index)
         :task-id (util/get-uuid)
         :slave-id (util/get-agent-id offer)  ;; maybe no functrion for this ...
         :executor (util/get-exec-info state) ;; use new payload ns instead
         :resources (resources/make offer)}))

(defn make
  ""
  [state data index offer]
  (->> offer
       (make-map state data index)
       (types/->pb :TaskInfo)))

(defn get-pb-name
  ""
  [task]
  (-> task
      (types/pb->data)
      :name))


;;; Task Status

(defn make-status-map
  ""
  [executor-id task-id state reason health]
  {:task-id {:value task-id}
     :executor-id executor-id
     :state state
     :reason reason
     :source :source-executor
     :healthy health})

(defn make-status
  ""
  [executor-id task-id state reason health]
  (types/->pb :TaskStatus
    (make-status-map executor-id task-id state reason health)))

(defn status-running
  ""
  [executor-id task-id]
  (make-status
    executor-id
    task-id
    :task-running
    ;; If we don't set a default :reason, Mesos ProtoBufs uses the default of
    ;; 0 which is actually :reason-command-executor-failed, and we don't want
    ;; that being the default. Mesos ProtoBufs sets the optional value of
    ;; reason to 10, which is REASON_SLAVE_DISCONNECTED, so that's what we use
    ;; here. Note that a nil value gets translated into 0, which is
    ;; REASON_COMMAND_EXECUTOR_FAILED, so that is also not used.
    :reason-slave-disconnected
    true))

(defn status-finished
  ""
  [executor-id task-id]
  (make-status
    executor-id
    task-id
    :task-finished
    ;; See the in-line code comment in status-running
    ;; for an explanation of the :reason value below.
    :reason-slave-disconnected
    true))

(defn status-failed
  ""
  [executor-id task-id]
  (make-status
    executor-id
    task-id
    :task-failed
    :reason-command-executor-failed
    false))
