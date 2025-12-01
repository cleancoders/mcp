(ns mcp.client.stdio-spec
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.utilc :as utilc]
            [clojure.java.io :as io]
            [mcp.client.stdio :as sut]
            [mcp.client.core :as core]
            [mcp.server.core :as server]
            [mcp.server.tool :as tool]
            [mcp.server.spec-helper :as server-helper]
            [speclj.core :refer :all])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream]))

(declare client-info)
(declare client)

(declare request)
(declare response)
(declare json-req)
(declare json-resp)
(declare impl)
(declare input-stream)
(declare output-stream)

(defn- ->reader [output-stream]
  (io/reader (ByteArrayInputStream. (.toByteArray output-stream))))

(defn- ->input-stream [s]
  (ByteArrayInputStream. (.getBytes s)))

(def server server-helper/test-server)

(describe "client stdio"

  (with client-info {:name    "ExampleClient"
                     :title   "Example Client Display Name"
                     :version "1.0.0"})

  (with client (core/->client @client-info))

  (with output-stream (ByteArrayOutputStream.))

  (context "raw-request!"

    (with json-req (utilc/->json (core/build-request 2 "tools/list")))
    (with json-resp (utilc/->json (server/handle server (utilc/<-json-kw @json-req))))
    (with input-stream (->input-stream @json-resp))
    (with impl (sut/->IOTransport @input-stream @output-stream))

    (it "sends request through output-stream"
      (sut/raw-request! @impl @json-req)
      (with-open [reader (->reader @output-stream)]
        (should= @json-req (slurp reader))))

    (it "receives response through input-stream"
      (should= @json-resp (sut/raw-request! @impl @json-req)))
    )

  (context "request!"

    (with request (core/build-request 2 "tools/list"))
    (with response (server/handle server @request))
    (with input-stream (->input-stream (utilc/->json @response)))
    (with impl (sut/->IOTransport @input-stream @output-stream))

    (it "sends request through output-stream as json"
      (sut/request! @impl @request)
      (with-open [reader (->reader @output-stream)]
        (should= (utilc/->json @request) (slurp reader))))

    (it "returns response through input-stream as edn"
      (should= @response (sut/request! @impl @request)))

    (it "throws if server response is not json"
      (let [input-stream (->input-stream "not json")
            impl (sut/->IOTransport input-stream @output-stream)]
        (should-throw (sut/request! impl @request))))
    )

  (context "request-initialize!"

    (with request (core/->initialize-request @client))
    (with response (server/handle server @request))
    (with input-stream (->input-stream (utilc/->json @response)))
    (with impl (sut/->IOTransport @input-stream @output-stream))

    (it "sends request through output-stream"
      (sut/request-initialize! @impl @client)
      (with-open [reader (->reader @output-stream)]
        (should= (utilc/->json @request) (slurp reader))))

    (it "receives response through input-stream"
      (should= @response (sut/request-initialize! @impl @client)))
    )

  (context "notify-initialized!"

    (with json-req core/initialized-notification)
    (with input-stream nil)
    (with impl (sut/->IOTransport @input-stream @output-stream))

    (it "sends request through output-stream"
      (sut/notify-initialized! @impl)
      (with-open [reader (->reader @output-stream)]
        (should= (utilc/->json @json-req) (slurp reader))))

    (it "doesn't expect response"
      (should-be-nil (sut/notify-initialized! @impl)))
    )

  )