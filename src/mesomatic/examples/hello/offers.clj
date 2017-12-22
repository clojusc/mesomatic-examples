(ns mesomatic.examples.hello.offers
  (:require
    [clojure.string :as string]
    [clojusc.twig :refer [pprint]]
    [mesomatic.examples.common.resources :as resources]
    [mesomatic.examples.hello.task :as task]
    [mesomatic.examples.util :as util]
    [taoensso.timbre :as log]))

(defn process-one
  ""
  [state data index offer]
  (let [task-info (task/make-map state data index offer)]
    task-info))

(defn loop-offers
  ""
  [state data offers]
  (loop [index 1
         [offer & remaining-offers] offers ; head|tail
         tasks [(process-one state data index offer)]]
    (let [rsrcs (resources/sum offer)]
      (if (> index 1)
        (do
          (log/debug "Offers processed.")
          tasks)
        (do
          (log/debug "Processing offers ...")
          (recur
            (inc index)
            remaining-offers
            (conj tasks
                  (process-one state data index offer))))))))

(defn process-all
  ""
  [state data offers]
  (into [] (loop-offers
             state
             data
             offers)))

(defn get-ids
  ""
  [offers]
  (map :id offers))
