(ns clojusc.mesomatic.examples.resources
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojusc.twig :refer [pprint]]))

(defn make-map
  ""
  [data]
  [{:name "cpus"
    :scalar (:cpus data)
    :type :value-scalar}
   {:name "mem"
    :scalar (:mem data)
    :type :value-scalar}])

(defn cpus?
  ""
  [resource]
  (if (= (:name resource) "cpus")
    true
    false))

(defn mem?
  ""
  [resource]
  (if (= (:name resource) "mem")
    true
    false))

(defn update-cpus
  ""
  [data resource]
  (if (cpus? resource)
    (assoc data :cpus (-> resource
                          :scalar
                          (+ (or (:cpus data)))))
    data))

(defn update-mem
  ""
  [data resource]
  (if (mem? resource)
    (assoc data :mem (-> resource
                         :scalar
                         (+ (or (:mem data) 0))))
    data))

(defn trace-totals
  ""
  [data]
  (log/tracef "Totalled %s cpus and %s mem from offer."
              (:cpus data) (:mem data))
  data)

(defn update-cpus-mem
  ""
  [data resource]
  (-> data
      (update-cpus resource)
      (update-mem resource)
      (trace-totals)))

(defn sum
  ""
  ([offer]
    (sum offer {:cpus 0 :mem 0}))
  ([offer init-data]
    (reduce update-cpus-mem
            init-data
            (:resources offer))))

(defn make
  ""
  [offer]
  (-> offer
      (sum)
      (make-map)))
