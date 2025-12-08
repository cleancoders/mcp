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
    (let [reader (io/reader (->input-stream "hello\nthere"))
          impl (sut/->IOTransport reader nil)]
      (should= "hello" (core/read! impl))
      (should= "there" (core/read! impl))))

  )