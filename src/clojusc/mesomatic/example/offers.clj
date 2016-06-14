(ns clojusc.mesomatic.example.offers
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.example.resources :as resources]
            [clojusc.mesomatic.example.task :as task]
            [clojusc.mesomatic.example.util :as util]))

(defn process-one
  ""
  [state data limits status index offer]
  (let [task-info (task/make-map state data index offer)]
    (log/trace "Got task info:" (pprint task-info))
    task-info))

(defn hit-limits?
  ""
  [limits status]
  (if (or (< (:remaining-cpus status) (:cpus-per-task limits))
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
      (update :remaining-cpus (partial - (:cpus rsrcs)))
      (update :remaining-mem (partial - (:mem rsrcs)))))

(defn loop-offers
  ""
  [state data limits offers]
  (loop [status {:remaining-cpus 0
                 :remaining-mem 0}
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
  (into [] (loop-offers state data limits offers)))

(defn get-ids
  ""
  [offers]
  (map :id offers))
