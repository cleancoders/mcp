(ns mcp.server.core-spec
  (:require [mcp.server.core :as sut]
            [mcp.server.spec-helper :as server-helper]
            [mcp.server.tool :as tool]
            [speclj.core :refer :all]))

(declare spec)
(declare server)
(declare req)

(describe "MCP Server core"

  (with spec {:name             "Test Server"
              :server-version   "1.0.0"
              :protocol-version "2025-06-18"
              :handlers         {"experimental/foo" {:handler (fn [_req] :handled)}}})
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

      (it "invalid id type returns invalid request"
        (let [resp (sut/handle @server (assoc @req :id true))]
          (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      ; will need to enforce parameter schema

      (it "invalid params"
        (let [resp (sut/handle @server (assoc @req :params true))]
          (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object" 1)))
      )

    (it "returns a number id"
      (let [resp (sut/handle @server (assoc @req :method ""))]
        (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object" 1)))

    (it "returns a string id"
      (let [resp (sut/handle @server (assoc @req :method "" :id "abc"))]
        (server-helper/should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object" "abc")))

    ; format edge cases
    ; make params optional
    )

  (context "it sends invalid request error when id has been used before"
    (before (server-helper/initialize! @server))
    (it "returns invalid request when id has been sent from client before"
      (let [resp (sut/handle @server {:jsonrpc "2.0" :id 1 :method "resources/list"})]
        (server-helper/should-respond-invalid-req resp "Request ID: 1 used previously during this session" 1))))

  (context "request handlers"

    (before (server-helper/initialize! @server))

    ; catch when handlers throw

    (it "sends invalid request when initialization comes through while already initialized"
      (let [resp (sut/handle @server (assoc server-helper/init-req :id 2))]
        (server-helper/should-respond-invalid-req resp "Connection already initialized" 2)))

    (it "undefined method"
      (let [resp (sut/handle @server (server-helper/->req {:method "foo/bar" :id 2}))]
        (server-helper/should-respond-unknown-method resp "Method 'foo/bar' is not supported" 2))
      )
    )

  (context "defines methods"

    (it "tools/list"
      (let [tool   {:name        "foo"
                    :title       "I'm to foo tool, the fool!"
                    :description "a foolish tool"
                    :handler     (fn [_req] :handled!)
                    :inputSchema {}}
            server (-> @spec
                       (tool/with-tool tool)
                       sut/->server)
            _      (server-helper/initialize! server)
            {:keys [result] :as resp} (sut/handle server (server-helper/->req {:method "tools/list" :id 2}))
            tools  (:tools result)]
        (should= "2.0" (:jsonrpc resp))
        (should= 2 (:id resp))
        (should= [(-> tool
                      (dissoc :handler)
                      (assoc :inputSchema {:type "object" :properties {}}))]
          tools)))

    (it "tools/call"
      (let [tool   {:name        "foo"
                    :title       "I'm to foo tool, the fool!"
                    :description "a foolish tool"
                    :handler     (fn [req] (prn (str "foo-" (:id req))))
                    :inputSchema {}}
            server (-> @spec
                       (tool/with-tool tool)
                       sut/->server)
            _      (server-helper/initialize! server)
            req    {:method "tools/call"
                    :id     2
                    :params {:name "foo"}}]
        (should-contain "foo-2" (with-out-str (sut/handle server (server-helper/->req req)))))))
  )