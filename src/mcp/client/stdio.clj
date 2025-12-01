(ns mcp.client.stdio
  (:require [c3kit.apron.utilc :as utilc]
            [clojure.java.io :as io]
            [mcp.client.core :as core])
  (:import [java.io InputStream OutputStream]))

(defn raw-request! [jrpc-payload ^InputStream in ^OutputStream out]
  (with-open [writer (io/writer out)]
    (spit writer jrpc-payload))
  (with-open [reader (io/reader in)]
    (slurp reader)))

(defn request! [edn-rpc-payload ^InputStream in ^OutputStream out]
  (-> edn-rpc-payload
      utilc/->json
      (raw-request! in out)
      utilc/<-json-kw))

(defn request-initialize! [client ^InputStream in ^OutputStream out]
  (request! (core/->initialize-request client) in out))

(defn notify-initialized! [^OutputStream out]
  (with-open [writer (io/writer out)]
    (spit writer (utilc/->json core/initialized-notification))))

