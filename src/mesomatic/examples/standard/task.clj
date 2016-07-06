(ns mesomatic.examples.standard.task
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [mesomatic.types :as types]
            [clojusc.twig :refer [pprint]]
            [mesomatic.examples.standard.resources :as resources]
            [mesomatic.examples.util :as util]))

(def task-info-map {:name "Example Task %d (Clojure)"
                    :count 1
                    :maxcol 1})

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

(defn status-running [executor-id task-id]
  (types/->pb :TaskStatus
    {:task-id {:value task-id}
     :executor-id executor-id
     :state :task-running
     ;; If we don't set a default :reason, Mesos ProtoBufs uses the default of
     ;; 0 which is actually :reason-command-executor-failed, and we don't want
     ;; that being the default. Mesos ProtoBufs sets the optional value of
     ;; reason to 10, which is REASON_SLAVE_DISCONNECTED, so that's what we use
     ;; here. Note that a nil value gets translated into 0, which is
     ;; REASON_COMMAND_EXECUTOR_FAILED, so that is also not used.
     :reason :reason-slave-disconnected
     :healthy true}))

(defn status-finished [executor-id task-id]
  (types/->pb :TaskStatus
    {:task-id {:value task-id}
     :executor-id executor-id
     :state :task-finished
     ;; See the in-line code comment in status-running
     ;; for an explanation of the :reason value below.
     :reason :reason-slave-disconnected
     :healthy true}))

(defn status-failed [executor-id task-id]
  (types/->pb :TaskStatus
    {:task-id {:value task-id}
     :executor-id executor-id
     :reason :reason-command-executor-failed
     :state :task-failed
     :healthy false}))
