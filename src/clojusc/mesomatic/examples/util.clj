(ns clojusc.mesomatic.examples.util
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [leiningen.core.main :as lein]
            [clojusc.twig :refer [pprint]])
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
  (-> str
      (string/lower-case)
      (keyword)))

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

;; XXX probably remove this function

(defn get-agent-id
  ""
  [offer]
  (get-in offer [:slave-id]))

;; XXX move this into new payload ns

(defn get-exec-info
  ""
  [state]
  (:exec-info state))

(defn cwd
  ""
  []
  (-> "."
      (java.io.File.)
      (.getAbsolutePath)))

(defn finish
  ""
  [& {:keys [exit-code]}]
  (lein/exit exit-code)
  exit-code)

(defn get-metas
  ""
  [an-ns]
  (->> an-ns
       (ns-publics)
       (map (fn [[k v]] [k (meta v)]))
       (into {})))

(defn get-meta
  "Takes the same form as the general `get-in` function:

      (get-meta 'my.name.space ['my-func :doc])"
  [an-ns rest]
  (-> an-ns
      (get-metas)
      (get-in rest)))

(defn get-docstring
  ""
  [an-ns fn-name]
  (get-meta an-ns [fn-name :doc]))
