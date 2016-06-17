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
