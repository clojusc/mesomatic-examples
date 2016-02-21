(ns mesomatic-hello.framework
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.tools.logging :as log]
            [twig.core :refer [pprint]]
            [mesomatic.async.scheduler :as async-scheduler]
            [mesomatic.scheduler :as scheduler :refer [scheduler-driver]]
            [mesomatic.types :as types]))

(defmulti handle-msg (comp :type last vector))

(defmethod handle-msg :registered
  [this data]
  (log/info "Registered with framework id:" (get-in data [:framework-id :value]))
  (log/debug "Got master info:" (pprint (:master-info data)))
  this)

(defmethod handle-msg :resource-offers
  [this data]
  (log/info "Updating offers with" (count (:offers data)) "new offers ...")
  (log/debug "Got offer info:" (pprint (:offers data)))
  (assoc this :offers (:offers data)))

(defn run
  ""
  [master]
  (log/info "Running framework ...")
  (let [ch (chan)
        sched (async-scheduler/scheduler ch)
        framework-info {:name "Hello World Framework (Clojure)"}
        driver (scheduler-driver sched framework-info master)]
    (log/debug "Starting scheduler ...")
    (scheduler/start! driver)
    (log/debug "Reducing over channel messages ...")
    (a/reduce handle-msg {:driver driver} ch)
    (scheduler/join! driver)))
