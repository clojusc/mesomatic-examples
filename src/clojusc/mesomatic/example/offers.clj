(ns clojusc.mesomatic.example.offers
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.example.task :as task]
            [clojusc.mesomatic.example.util :as util]))

(defn process-one
  ""
  [state data limits status index offer]
  (log/debug "limits:" limits)
  (log/debug "status:" status)
  (let [task-info (task/make state data index offer)]
    (log/trace "Got task info:" (pprint task-info))
    task-info))

(defn hit-limits?
  ""
  [limits status]
  (if (or (< (:remaining-cpus status) (:cpus-per-task limits))
          (< (:remaining-mem status) (:mem-per-task limits)))
    true
    false))

(defn process-all
  ""
  [state data limits offers]
  (loop [status {:remaining-cpus nil
                 :remaining-mem nil}
         index 1
         offers offers
         tasks []]
    (if-not (or (hit-limits? limits status) (empty? offers))
      (recur
        ;; XXX update remaining resources
        status
        (inc index)
        (rest offers)
        (conj tasks
              (process-one state data limits status index (first offers))))
      tasks)))
