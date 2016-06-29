(ns clojusc.mesomatic.examples.standard.task
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [mesomatic.types :as types]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.examples.standard.resources :as resources]
            [clojusc.mesomatic.examples.util :as util]))

(def task-info-map {:name "Example Task %d (Clojure)"
                    :count 1
                    :maxcol 1})

(defn make-map
  ""
  [state data index offer]
  (into task-info-map
        {:name (format (:name task-info-map) index)
         :task-id (util/get-uuid)
         :slave-id (util/get-agent-id offer) ;; maybe no functrion for this ...
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
