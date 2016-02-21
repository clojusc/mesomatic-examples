(ns mesomatic-hello.executor
  ""
  (:require [clojure.core.async :as a :refer [chan <! go]]
            [clojure.tools.logging :as log]
            [mesomatic.async.executor :as async-executor]
            [mesomatic.executor :as executor :refer [executor-driver]]
            [mesomatic.types :as types]))

(defn run
  ""
  [master]
  (log/info "Running executor ..."))
