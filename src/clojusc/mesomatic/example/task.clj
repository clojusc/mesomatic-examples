(ns clojusc.mesomatic.example.task
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.example.resources :as resources]
            [clojusc.mesomatic.example.util :as util]))

(def task-info-map {:name "Example Task"
                    :count 1
                    :maxcol 1})

(defn make-map
  ""
  [state data offer]
  (into task-info-map
        {:id (util/get-uuid)
         :slave-id (util/get-agent-id offer)
         :executor (util/get-exec-info state)
         :resources (resources/make offer)}))

(defn schedule-tasks
  ""
  []
  )
