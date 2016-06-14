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
         [offer & remaining-offers] offers
         tasks []]
    ;; XXX - start
    (log/debug "Offer:" (pprint offer))
    (log/debug "Resources:" (pprint (:resources offer)))
    ;; XXX - end
    (if (or (hit-limits? limits status) (empty? remaining-offers))
      tasks
      (recur
        ;; XXX update remaining resources
        status
        (inc index)
        remaining-offers
        (conj tasks
              (process-one state data limits status index offer))))))
