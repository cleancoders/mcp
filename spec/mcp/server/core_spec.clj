(ns mcp.server.core-spec
  (:require [mcp.server.core :as sut]
            [speclj.core :refer :all]))

(declare spec)
(declare server)
(declare req)

(defmacro should-respond-invalid-req [resp]
  `(let [resp#  ~resp
         error# (:error resp#)]
     (should= "2.0" (:jsonrpc resp#))
     (should= -32600 (:code error#))
     (should= "Invalid Request" (:message error#))
     (should= "The JSON sent is not a valid JSON-RPC request object" (:data error#))))

(describe "MCP Server core"

  (with spec {:name             "Test Server"
              :server-version   "1.0.0"
              :protocol-version "2024-11-05"})
  (with server (sut/->server @spec))

  (context "message format"

    ; will need to check for non-json at higher level

    (context "rejects bad JSON-RPC"

      (it "junk json"
        (let [req  {:foo :bar}
              resp (sut/handle @server req)]
          (should-respond-invalid-req resp)))

      (it "missing JSON-RPC version"
        (let [req  {}
              resp (sut/handle @server req)]
          (should-respond-invalid-req resp)))

      (it "wrong JSON-RPC version"
        (let [req  {:jsonrpc 1}
              resp (sut/handle @server req)]
          (should-respond-invalid-req resp)))

      #_(it "missing method"
        (let [req  {:jsonrpc "2.0"}
              resp (sut/handle @server req)]
          (should-respond-invalid-req resp)))
      )
    )

  (context "lifecycle"

    (it "rejects clients with a newer version"
      (let [req {:jsonrpc "2.0"
                 :id      1
                 :method  "initialize"
                 :params  {:protocolVersion "2024-11-06"}}
            {:keys [error] :as resp} (sut/handle @server req)]
        (should= "2.0" (:jsonrpc resp))
        (should= 1 (:id resp))
        (should= -32602 (:code error))
        (should= "Unsupported protocol version" (:message error))
        (should= ["2024-11-05"] (:supported (:data error)))))
    )
  )