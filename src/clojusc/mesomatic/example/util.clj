(ns clojusc.mesomatic.example.util
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:import java.util.UUID))

(defn get-uuid
  "A Mesos/protobufs-friendly UUID wrapper."
  []
  (->> (UUID/randomUUID)
       (str)
       (assoc {} :value)))

(defn make-rsrcs
  ""
  [& {:keys [cpus mem]}]
  [{:name "cpus"
    :scalar cpus
    :type :value-scalar}
   {:name "mem"
    :scalar mem
    :type :value-scalar}])

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

(defn cpus-resource?
  ""
  [resource]
  (if (= (:name resource) "cpus")
    true
    false))

(defn mem-resource?
  ""
  [resource]
  (if (= (:name resource) "mem")
    true
    false))

(defn update-cpus
  ""
  [data resource]
  (if (cpus-resource? resource)
    (assoc data :cpus (-> resource
                          (get-in [:scalar :value])
                          (+ (or (:cpus data)))))
    data))

(defn update-mem
  ""
  [data resource]
  (if (mem-resource? resource)
    (assoc data :mem  (-> resource
                          (get-in [:scalar :value])
                          (+ (or (:mem data) 0))))
    data))

(defn update-cpus-mem
  ""
  [data resource]
  (-> data
      (update-cpus resource)
      (update-mem resource)))

(defn sum-resources
  ""
  [offer init-data]
  (reduce update-cpus-mem
          init-data
          (flatten (map :resources offer))))

(defn process-offer
  ""
  [master-info limits status offer]
  (let [init-data {:cpus 0 :mem 0}
        _ (log/debug "master-into:" master-info)
        _ (log/debug "limits:" limits)
        _ (log/debug "status:" status)
        _ (log/debug "offer resources:" (:resources offer))
        rsrcs (sum-resources offer init-data)]
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
  [master-info limits offers]
  (let [status {:remaining-cpus nil
                :remaining-mem nil}]
    (->> offers
         (map #(process-offer master-info limits status %))
         (into []))))
