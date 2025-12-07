(ns mcp.client.stdio
  (:require [mcp.client.core :as core])
  (:import [java.io Reader Writer]))

(deftype IOTransport [^Reader reader ^Writer writer]
  core/Transport
  (send! [_ jrpc-payload]
    (.write writer ^String jrpc-payload)
    (.newLine writer)
    (.flush writer))
  (read! [_]
    (.readLine reader)))
