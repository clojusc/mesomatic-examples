(ns clojusc.mesomatic.hello.util
  ""
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

(defn make-env
  ""
  []
  (->> (System/getenv)
       (#(for [[k v] %] {:name (name k) :value (str v)}))
       (into [])
       (assoc {} :variables)))
