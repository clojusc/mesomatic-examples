(ns clojusc.mesomatic.example.util
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]
            [clojusc.mesomatic.example.resources :as resources])
  (:import java.util.UUID))

(defn get-uuid
  "A Mesos/protobufs-friendly UUID wrapper."
  []
  (->> (UUID/randomUUID)
       (str)
       (assoc {} :value)))

(defn lower-key
  "Convert a string to a lower-cased keyword."
  [str]
  (comp keyword string/lower-case))

(defn keys->keyword
  "Convert all the keys in a map from strings to lower-cased keywords."
  [m]
  (zipmap
    (map lower-key (keys m))
    (vals m)))

(defn make-env
  "Convert the OS environment variables to a Mesos-ready map."
  []
  (->> (System/getenv)
       (keys->keyword)
       (into [])
       (assoc {} :variables)))

(defn get-error-msg
  ""
  [data]
  (let [msg (get-in data [:status :message])]
    (cond
      (empty? msg) (get-in data [:status :reason])
      :true msg)))

(defn get-framework-id
  ""
  [data]
  (get-in data [:framework-id :value]))

(defn get-agent-id
  ""
  [offer]
  (get-in offer [:slave-id]))

(defn process-offer
  ""
  [master-info limits status offer]
  (log/debug "limits:" limits)
  (log/debug "status:" status)
  (let [rsrcs (resources/sum offer)
        rsrc-data (resources/make-map rsrcs)]
    (log/debugf "Received offer %s with %s cpus and %s mem."
                (:id offer) (:cpus rsrcs) (:mem rsrcs))
    ))

(defn process-offers
  ""
  [offers]
  )

  ; (let [offer-id (get-in data [:offers 0 :id])
  ;       framework-id (get-in data [:offers 0 :framework-id])
  ;       agent-id (get-in data [:offers 0 :slave-id])
  ;       task-id (util/get-uuid)
  ;       exec-info (example-executor/cmd-info (:master state) framework-id)
  ;       task (assoc task-info
  ;              :slave-id agent-id
  ;              :slave agent-id
  ;              :task-id [task-id]
  ;              :executor exec-info
  ;              )
  ;       tasks [(types/map->TaskInfo task)]]


(defn schedule-tasks
  ""
  [state data limits]
  (let [offers (:offers data)
        master-info (:master-info state)
        framework-id (get-framework-id data)
        exec-info (:exec-info state)
        status {:remaining-cpus nil
                :remaining-mem nil}]
    (log/trace "Offers:" (pprint offers))
    (->> offers
         (map #(process-offer master-info limits status %))
         (into []))))
