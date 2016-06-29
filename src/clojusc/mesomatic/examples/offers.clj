(ns clojusc.mesomatic.examples.offers
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.examples.resources :as resources]
            [clojusc.mesomatic.examples.task :as task]
            [clojusc.mesomatic.examples.util :as util]))

(defn process-one
  ""
  [state data limits status index offer]
  (let [task-info (task/make-map state data index offer)]
    (log/trace "Got task info:" (pprint task-info))
    task-info))

(defn hit-limits?
  ""
  [limits status]
  (if (or (>= (:launched-tasks status) (:total-tasks limits))
          (< (:remaining-cpus status) (:cpus-per-task limits))
          (< (:remaining-mem status) (:mem-per-task limits)))
    (do
      (log/debug "Hit resource limit.")
      true)
      false))

(defn quit-loop?
  ""
  [limits status offers]
  (or (hit-limits? limits status)
      (empty? offers)))

(defn update-status
  ""
  [status rsrcs]
  (log/debug "Updating status with rsrcs:" rsrcs)
  (-> status
      (update :launched-tasks inc)
      (update :remaining-cpus (partial - (:cpus rsrcs)))
      (update :remaining-mem (partial - (:mem rsrcs)))))

(defn loop-offers
  ""
  [state data limits offers]
  (loop [status {:remaining-cpus 0
                 :remaining-mem 0
                 :launched-tasks 0}
         index 1
         [offer & remaining-offers] offers
         tasks [(process-one state data limits status index offer)]]
    (let [rsrcs (resources/sum offer)
          status (update-status status rsrcs)]
      (if (quit-loop? limits status remaining-offers)
        (do
          (log/debug "Quitting loop ...")
          tasks)
        (do
          (log/debug "Iterating offers ...")
          (recur
            ;; XXX update remaining resources
            status
            (inc index)
            remaining-offers
            (conj tasks
                  (process-one state data limits status index offer))))))))

(defn process-all
  ""
  [state data limits offers]
  (into [] (loop-offers
             state
             data
             (assoc limits :total-tasks (:total-tasks state))
             offers)))

(defn get-ids
  ""
  [offers]
  (map :id offers))
