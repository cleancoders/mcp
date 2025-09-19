(ns mcp.server.stdio-spec
  (:require [c3kit.apron.utilc :as utilc]
            [mcp.server.core :as server]
            [mcp.server.errors :as errors]
            [mcp.server.stdio :as sut]
            [speclj.core :refer :all])
  (:import (clojure.lang ExceptionInfo)
           (java.io IOException)))

(declare spec)
(declare server)

(describe "stdio"
  (with-stubs)

  (with spec {:name             "Test Server"
              :server-version   "1.0.0"
              :protocol-version "2025-06-18"
              :capabilities     {"experimental/foo" {:handler (fn [_] :handled)}}})
  (with server (server/->server @spec))

  (it "throws a custom error if read-line fails"
    (with-redefs [read-line (fn [] (throw (IOException. "Stdin error")))]
      (let [req     (utilc/->json {:id 1 :method "initialize" :params {}})
            message "Input stream from client severed"]
        (->> @server
             sut/handle-stdio
             with-out-str
             (with-in-str req)
             (should-throw ExceptionInfo message)))))

  (it "stdio handler prints error to stdout when not json"
    (let [req      "blah"
          expected (errors/bad-request "The JSON sent is not a valid JSON-RPC request object")
          out-json (with-in-str req (with-out-str (sut/handle-stdio @server)))]
      (should= expected (utilc/<-json-kw out-json))))

  (it "stdio handler prints error to stdout when not json"
    (with-redefs [server/handle      (stub :server/handle)
                  sut/send-response! (stub :send-response!)]
      (let [req (utilc/->json {:jsonrpc "2.0" :id 1 :method "initialize" :params {}})]
        (with-in-str req (sut/handle-stdio @server))
        (should-have-invoked :server/handle)
        (should-not-have-invoked :send-response!))))

  (it "stdio handler prints error to stdout when jsonrpc missing"
    (let [req      (utilc/->json {:id 1 :method "initialize" :params {}})
          expected (errors/bad-request "The JSON sent is not a valid JSON-RPC request object")
          out-json (with-in-str req (with-out-str (sut/handle-stdio @server)))]
      (should= expected (utilc/<-json-kw out-json))))
  )
