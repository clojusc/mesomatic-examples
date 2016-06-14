(ns clojusc.mesomatic.example.offers
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.example.task :as task]
            [clojusc.mesomatic.example.util :as util]))

(defn process-one
  ""
  [state data limits status offer]
  (log/debug "limits:" limits)
  (log/debug "status:" status)
  (let [task-info (task/make state data offer)]
    (log/trace "Got task info:" (pprint task-info))
    ))

(defn process-all
  ""
  [state data limits offers]
  (let [status {:remaining-cpus nil
                :remaining-mem nil}
        process-fn (partial process-one state data limits status)]
    ;; XXX probbaly going to convert this to a loop + recur
    (map process-fn offers)))
