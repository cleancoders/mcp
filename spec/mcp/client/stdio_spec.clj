(ns mcp.client.stdio-spec
  (:require [c3kit.apron.utilc :as utilc]
            [clojure.java.io :as io]
            [mcp.client.stdio :as sut]
            [mcp.client.core :as core]
            [mcp.server.core :as server]
            [mcp.server.tool :as tool]
            [speclj.core :refer :all])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream]))

(declare server-tool)
(declare server-spec)
(declare server)

(declare client-info)
(declare client)

(declare request)
(declare json-resp)
(declare input-stream)
(declare output-stream)

(defn- ->reader [output-stream]
  (io/reader (ByteArrayInputStream. (.toByteArray output-stream))))

(describe "client stdio"

  (with server-tool {:name        "foo"
                     :title       "I'm to foo tool, the fool!"
                     :description "a foolish tool"
                     :handler     (fn [_req] "handled!")
                     :inputSchema {}})
  (with server-spec {:name             "Test Server"
                     :server-version   "1.0.0"
                     :protocol-version "2025-06-18"})
  (with server (-> @server-spec
                   (tool/with-tool @server-tool)
                   server/->server))

  (with client-info {:name    "ExampleClient"
                     :title   "Example Client Display Name"
                     :version "1.0.0"})

  (with client (core/->client @client-info))

  (with output-stream (ByteArrayOutputStream.))

  (context "request!"

    (with request (core/build-request 2 :tools/list))
    (with json-resp (utilc/->json (server/handle @server @request)))
    (with input-stream (ByteArrayInputStream. (.getBytes @json-resp)))

    (it "sends request through output-stream"
      (sut/request! @request @input-stream @output-stream)
      (with-open [reader (->reader @output-stream)]
        (should= (utilc/->json @request) (slurp reader))))

    (it "receives response through input-stream"
      (should= @json-resp (sut/request! @request @input-stream @output-stream)))
    )

  (context "request-initialize!"

    (with request (core/->initialize-request @client))
    (with json-resp (utilc/->json (server/handle @server @request)))
    (with input-stream (ByteArrayInputStream. (.getBytes @json-resp)))

    (it "sends request through output-stream"
      (sut/request-initialize! @client @input-stream @output-stream)
      (with-open [reader (->reader @output-stream)]
        (should= (utilc/->json @request) (slurp reader))))

    (it "receives response through input-stream"
      (should= @json-resp (sut/request-initialize! @client @input-stream @output-stream)))
    )

  (context "notify-initialized!"

    (with request core/initialized-notification)

    (it "sends request through output-stream"
      (sut/notify-initialized! @output-stream)
      (with-open [reader (->reader @output-stream)]
        (should= (utilc/->json @request) (slurp reader))))

    (it "doesn't expect response"
      (should-be-nil (sut/notify-initialized! @output-stream)))
    )

  )