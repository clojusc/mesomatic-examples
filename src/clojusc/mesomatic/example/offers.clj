(ns clojusc.mesomatic.example.offers
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.example.task :as task]
            [clojusc.mesomatic.example.util :as util]))

(defn process-offer
  ""
  [state data limits status offer]
  (log/debug "limits:" limits)
  (log/debug "status:" status)
  (let [task-info (task/make-map state data offer)]
    (log/debug "Got task info:" (pprint task-info))
    ))

(defn process
  ""
  [state data limits offers]
  (let [status {:remaining-cpus nil
                :remaining-mem nil}
        process-fn (partial process-offer state data limits status)]
    ;; XXX probbaly going to convert this to a loop + recur
    (map process-fn offers)))
