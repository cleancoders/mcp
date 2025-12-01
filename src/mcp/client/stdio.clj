(ns mcp.client.stdio
  (:require [c3kit.apron.utilc :as utilc]
            [clojure.java.io :as io]
            [mcp.client.core :as core])
  (:import [java.io InputStream OutputStream]))

(defprotocol Transport
  (send! [this jrpc-payload])
  (read! [this]))

(deftype StdioTransport [^InputStream in ^OutputStream out]
  Transport
  (send! [_ jrpc-payload]
    (with-open [writer (io/writer out)]
      (spit writer jrpc-payload)))
  (read! [_]
    (with-open [reader (io/reader in)]
      (slurp reader))))

(defn raw-request! [transport jrpc-payload]
  (send! transport jrpc-payload)
  (read! transport))

(defn request! [transport edn-rpc-payload]
  (->> edn-rpc-payload
       utilc/->json
       (raw-request! transport)
       utilc/<-json-kw))

(defn request-initialize! [transport client]
  (request! transport (core/->initialize-request client)))

(defn notify-initialized! [transport]
  (send! transport (utilc/->json core/initialized-notification)))

