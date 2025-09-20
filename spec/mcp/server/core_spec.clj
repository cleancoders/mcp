(ns mcp.server.core-spec
  (:require [mcp.server.core :as sut]
            [mcp.server.spec-helper :as server-helper]
            [speclj.core :refer :all]))

(declare spec)
(declare server)
(declare req)

(describe "MCP Server core"

  (with spec {:name             "Test Server"
              :server-version   "1.0.0"
              :protocol-version "2025-06-18"
              :capabilities     {"experimental/foo" {:handler (fn [_req] :handled)}}})
  (with server (sut/->server @spec))

  (with req server-helper/init-req)

  (context "message format"

    (context "rejects bad JSON-RPC"

      (it "junk json"
        (let [req  {:foo :bar}
              resp (sut/handle @server req)]
          (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      (it "missing JSON-RPC version"
        (let [resp (sut/handle @server (dissoc @req :jsonrpc))]
          (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object" (:id @req))))

      (it "wrong JSON-RPC version"
        (let [req  (assoc @req :jsonrpc "foo")
              resp (sut/handle @server req)]
          (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object" (:id req))))

      (it "missing method"
        (let [resp (sut/handle @server (dissoc @req :method))]
          (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object" (:id @req))))

      (it "empty method"
        (let [resp (sut/handle @server (assoc @req :method ""))]
          (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object" (:id @req))))

      (it "invalid id"
        (let [resp (sut/handle @server (assoc @req :id true))]
          (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      ; will need to enforce parameter schema
      )

    ; format edge cases
    ; string id
    ; block requests
    ; make params optional
    )

  #_(context "request handlers"

      (it "respond after initialization"
        (sut/handle @server server-helper/init-req))
      )
  )