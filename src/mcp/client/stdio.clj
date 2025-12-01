(ns mcp.client.stdio
  (:require [c3kit.apron.utilc :as utilc]
            [clojure.java.io :as io]
            [mcp.client.core :as core])
  (:import [java.io InputStream OutputStream]))

(defn send! [jrpc-payload ^OutputStream out]
  (with-open [writer (io/writer out)]
    (spit writer jrpc-payload)))

(defn read! [^InputStream in]
  (with-open [reader (io/reader in)]
    (slurp reader)))

(defn raw-request! [jrpc-payload ^InputStream in ^OutputStream out]
  (send! jrpc-payload out)
  (read! in))

(defn request! [edn-rpc-payload ^InputStream in ^OutputStream out]
  (-> edn-rpc-payload
      utilc/->json
      (raw-request! in out)
      utilc/<-json-kw))

(defn request-initialize! [client ^InputStream in ^OutputStream out]
  (request! (core/->initialize-request client) in out))

(defn notify-initialized! [^OutputStream out]
  (send! (utilc/->json core/initialized-notification) out))

