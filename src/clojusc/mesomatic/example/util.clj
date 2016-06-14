(ns clojusc.mesomatic.example.util
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [mesomatic.types :as types]
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

(defn get-offers
  ""
  [data]
  (get-in data [:offers]))

(defn get-master-info
  ""
  [state]
  (:master-info state))

(defn get-exec-info
  ""
  [state]
  (:exec-info state))

(defn get-pb-task-name
  ""
  [task]
  (-> task
      (types/pb->data)
      :name))
