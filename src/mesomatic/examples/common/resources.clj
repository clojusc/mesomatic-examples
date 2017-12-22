(ns mesomatic.examples.common.resources
  (:require
    [clojure.string :as string]
    [clojusc.twig :refer [pprint]]
    [taoensso.timbre :as log]))

(defn make-map
  ""
  [{cpus :cpus mem :mem}]
  [{:name "cpus"
    :scalar (if (nil? cpus) 1 cpus)
    :type :value-scalar}
   {:name "mem"
    :scalar (if (nil? mem) 128 mem)
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
  [offer & {:keys [cpus mem]}]
  ; (-> offer
  ;     (sum)
  ;     (make-map))
  (make-map {:cpus cpus :mem mem}))
