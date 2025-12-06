(ns mcp.client.stdio-spec
  (:require [c3kit.apron.utilc :as utilc]
            [clojure.java.io :as io]
            [mcp.client.stdio :as sut]
            [mcp.client.core :as core]
            [mcp.server.core :as server]
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
    (with impl (sut/->IOTransport (io/reader @input-stream) (io/writer @output-stream)))

    (it "sends request through output-stream"
      (core/raw-request! @impl @json-req)
      (with-open [reader (->reader @output-stream)]
        (should= (str @json-req "\n") (slurp reader))))

    (it "receives response through input-stream"
      (should= @json-resp (core/raw-request! @impl @json-req)))

    (it "can handle multiple procedure calls"
      (let [json-req-2 (utilc/->json (core/build-request 3 "tools/list"))
            json-resp-2 (utilc/->json (server/handle server (utilc/<-json-kw json-req-2)))
            input-stream (->input-stream (str @json-resp "\n" json-resp-2 "\n"))
            impl (sut/->IOTransport (io/reader input-stream) (io/writer @output-stream))]
        (should= @json-resp (core/raw-request! impl @json-req))
        (should= json-resp-2 (core/raw-request! impl json-req-2))
        (with-open [reader (->reader @output-stream)]
          (should= (str @json-req "\n" json-req-2 "\n") (slurp reader)))))
    )

  (context "request!"

    (with request (core/build-request 2 "tools/list"))
    (with response (server/handle server @request))
    (with input-stream (->input-stream (utilc/->json @response)))
    (with impl (sut/->IOTransport (io/reader @input-stream) (io/writer @output-stream)))

    (it "sends request through output-stream as json"
      (core/request! @impl @request)
      (with-open [reader (->reader @output-stream)]
        (should= (str (utilc/->json @request) "\n") (slurp reader))))

    (it "returns response through input-stream as edn"
      (should= @response (core/request! @impl @request)))

    (it "throws if server response is not json"
      (let [input-stream (->input-stream "not json")
            impl (sut/->IOTransport (io/reader input-stream) (io/writer @output-stream))]
        (should-throw (core/request! impl @request))))
    )

  (context "request-initialize!"

    (with request (core/->initialize-request @client))
    (with response (server/handle server @request))
    (with input-stream (->input-stream (utilc/->json @response)))
    (with impl (sut/->IOTransport (io/reader @input-stream) (io/writer @output-stream)))

    (it "sends request through output-stream"
      (core/request-initialize! @impl @client)
      (with-open [reader (->reader @output-stream)]
        (should= (str (utilc/->json @request) "\n") (slurp reader))))

    (it "receives response through input-stream"
      (should= @response (core/request-initialize! @impl @client)))
    )

  (context "notify-initialized!"

    (with json-req core/initialized-notification)
    (with impl (sut/->IOTransport nil (io/writer @output-stream)))

    (it "sends request through output-stream"
      (core/notify-initialized! @impl)
      (with-open [reader (->reader @output-stream)]
        (should= (str (utilc/->json @json-req) "\n") (slurp reader))))

    (it "doesn't expect response"
      (should-be-nil (core/notify-initialized! @impl)))
    )

  )