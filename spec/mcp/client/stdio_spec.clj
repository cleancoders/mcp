(ns mcp.client.stdio-spec
  (:require [c3kit.apron.utilc :as utilc]
            [clojure.java.io :as io]
            [mcp.client.stdio :as sut]
            [mcp.client.core :as core]
            [mcp.server.core :as server]
            [mcp.server.spec-helper :as server-helper]
            [speclj.core :refer :all])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream]))


(declare json-req)
(declare json-resp)
(declare output-stream)

(defn- ->reader [output-stream]
  (io/reader (ByteArrayInputStream. (.toByteArray output-stream))))

(defn- ->input-stream [s]
  (ByteArrayInputStream. (.getBytes s)))

(def server server-helper/test-server)

(describe "client stdio"

  (with output-stream (ByteArrayOutputStream.))

  (it "writes to writer"
    (let [writer (io/writer @output-stream)
          impl (sut/->IOTransport nil writer)]
      (core/send! impl "123")
      (with-open [reader (->reader @output-stream)]
        (should= "123" (.readLine reader)))))

  (it "reads from reader"
    (let [reader (io/reader (->input-stream "hello"))
          impl (sut/->IOTransport reader nil)]
      (should= "hello" (core/read! impl))))

  #_(context "raw-request!"

    (with json-req (utilc/->json (core/build-request 2 "tools/list")))
    (with json-resp (utilc/->json (server/handle server (utilc/<-json-kw @json-req))))

    (it "can handle multiple procedure calls"
      (let [json-req-2 (utilc/->json (core/build-request 3 "tools/list"))
            json-resp-2 (utilc/->json (server/handle server (utilc/<-json-kw json-req-2)))
            input-stream (->input-stream (str @json-resp "\n" json-resp-2 "\n"))
            impl (sut/->IOTransport (io/reader input-stream) (io/writer @output-stream))]
        (should= @json-resp (core/raw-json-request! impl @json-req))
        (should= json-resp-2 (core/raw-json-request! impl json-req-2))
        (with-open [reader (->reader @output-stream)]
          (should= (str @json-req "\n" json-req-2 "\n") (slurp reader)))))
    )

  )