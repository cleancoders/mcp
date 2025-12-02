(ns mcp.client
  (:require [c3kit.apron.corec :as ccc]
            [clojure.java.io :as io]
            [clojure.java.process :as process]
            [mcp.client.core :as core]
            [mcp.client.stdio :as stdio]))

(defn -main [& _args]
  (let [client (core/->client {:name    "ExampleClient"
                               :title   "Example Client Display Name"
                               :version "1.0.0"})
        server-proc (process/start
                      {:in :pipe
                       :out :pipe
                       :err :inherit}
                      "clojure" "-Mserve")
        writer (io/writer (process/stdin server-proc))
        reader (io/reader (process/stdout server-proc))
        impl (stdio/->IOTransport reader writer)]
    (ccc/->inspect (stdio/request-initialize! impl client))
    (ccc/->inspect (stdio/notify-initialized! impl))
    (ccc/->inspect (stdio/request! impl (core/build-request 2 "tools/list")))))