(ns mcp.server.trace
  (:require [c3kit.apron.time :as time]
            [c3kit.apron.utilc :as utilc]
            [clojure.java.io :as io]))

(defn ->event [type correlation-id data]
  {:type           type
   :correlation-id correlation-id
   :timestamp      (time/now)
   :data           data})

(defn- ensure-parent-dirs! [path]
  (when-let [parent (.getParentFile (io/file path))]
    (.mkdirs parent)))

(defn ->file-sink [path]
  (ensure-parent-dirs! path)
  (fn [event]
    (spit path (str (utilc/->json event) "\n") :append true)))

(defn noop-sink [_event]
  nil)

(defn trace! [config type correlation-id data]
  (when-let [trace-config (:trace config)]
    (when (:enabled? trace-config)
      (let [sink  (:sink trace-config)
            event (->event type correlation-id data)]
        (sink event)))))