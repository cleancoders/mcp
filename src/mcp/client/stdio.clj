(ns mcp.client.stdio
  (:import [java.io Reader Writer]
           (mcp.client.core Transport)))

(deftype IOTransport [^Reader reader ^Writer writer]
  Transport
  (send! [_ jrpc-payload]
    (.write writer ^String jrpc-payload)
    (.newLine writer)
    (.flush writer))
  (read! [_]
    (.readLine reader)))
