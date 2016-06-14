(ns clojusc.mesomatic.example.task
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [mesomatic.types :as types]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.example.resources :as resources]
            [clojusc.mesomatic.example.util :as util]))

(def task-info-map {:name "Example Task %d (Clojure)"
                    :count 1
                    :maxcol 1})

(defn make-map
  ""
  [state data index offer]
  (into task-info-map
        {:name (format (:name task-info-map) index)
         :task-id (util/get-uuid)
         :slave-id (util/get-agent-id offer)
         :executor (util/get-exec-info state)
         :resources (resources/make offer)}))

(defn make
  ""
  [state data index offer]
  (->> offer
       (make-map state data index)
       (types/->pb :TaskInfo)))

(defn schedule-tasks
  ""
  []
  )
