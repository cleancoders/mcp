(ns mcp.server.core-spec
  (:require [c3kit.apron.utilc :as utilc]
            [mcp.server.core :as sut]
            [speclj.core :refer :all]))

(declare spec)
(declare server)
(declare req)

(defmacro should-respond-invalid-req [resp msg]
  `(let [resp#  ~resp
         error# (:error resp#)]
     (should= "2.0" (:jsonrpc resp#))
     (should= -32600 (:code error#))
     (should= "Invalid Request" (:message error#))
     (should= ~msg (:data error#))))

(defn ->req [spec]
  (merge spec
         {:jsonrpc "2.0"}))
(def init-req
  (->req {:method "initialize"
          :id     1
          :params {:protocolVersion "2025-06-18"
                   :capabilities    {}
                   :clientInfo      {:name    "Test Client"
                                     :version "1.0.0"}}}))
(def foo-req
  (->req {:method "experimental/foo" :id 2 :params {}}))

(describe "MCP Server core"

  (with spec {:name             "Test Server"
              :server-version   "1.0.0"
              :protocol-version "2025-06-18"
              :capabilities     {"experimental/foo" {:handler (fn [req] :handled)}}})
  (with server (sut/->server @spec))

  (with req init-req)

  (context "message format"

    ; will need to check for non-json at higher level

    (context "rejects bad JSON-RPC"

      (it "junk json"
        (let [req  {:foo :bar}
              resp (sut/handle @server req)]
          (should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      (it "missing JSON-RPC version"
        (let [resp (sut/handle @server (dissoc @req :jsonrpc))]
          (should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      (it "wrong JSON-RPC version"
        (let [req  (assoc @req :jsonrpc "foo")
              resp (sut/handle @server req)]
          (should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      (it "missing method"
        (let [resp (sut/handle @server (dissoc @req :method))]
          (should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      (it "empty method"
        (let [resp (sut/handle @server (assoc @req :method ""))]
          (should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      (it "missing parameters"
        (let [resp (sut/handle @server (dissoc @req :params))]
          (should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      (it "invalid id"
        (let [resp (sut/handle @server (assoc @req :id true))]
          (should-respond-invalid-req resp "The JSON sent is not a valid JSON-RPC request object")))

      ; will need to enforce parameter schema
      )
    )

  (context "lifecycle"

    (context "rejects messages"

      (it "before initialization"
        (let [{:keys [error] :as resp} (sut/handle @server foo-req)]
          (should= "2.0" (:jsonrpc resp))
          (should= 2 (:id resp))
          (should= -32002 (:code error))
          (should= "Server not initialized" (:message error))
          (should= "Must initialize connection before invoking methods" (:data error))))

      (it "after notification, but before initialization"
        (sut/handle @server {:jsonrpc "2.0" :method "notification/initialized"})
        (let [{:keys [error] :as resp} (sut/handle @server foo-req)]
          (should= "2.0" (:jsonrpc resp))
          (should= 2 (:id resp))
          (should= -32002 (:code error))
          (should= "Server not initialized" (:message error))
          (should= "Must initialize connection before invoking methods" (:data error))))

      #_(it "from clients with a newer version"
        (let [req (assoc-in @req [:params :protocolVersion] "2025-06-19")
              {:keys [error] :as resp} (sut/handle @server req)]
          (should= "2.0" (:jsonrpc resp))
          (should= 1 (:id resp))
          (should= -32602 (:code error))
          (should= "Unsupported protocol version" (:message error))
          (should= ["2025-06-18"] (:supported (:data error))))))

    (context "responds to initialization request"

      (it "with server info"
        (let [{:keys [result] :as resp} (sut/handle @server init-req)
              server-info (:serverInfo result)]
          (should= "2.0" (:jsonrpc resp))
          (should= 1 (:id resp))
          (should= "2025-06-18" (:protocolVersion result))
          (should= "Test Server" (:name server-info))
          (should= "1.0.0" (:version server-info))))

      (it "with title"
        (let [server (sut/->server (assoc @spec :title "The Title"))
              resp   (sut/handle server init-req)]
          (should= "The Title" (-> resp :result :serverInfo :title))))

      (it "only once"
        (sut/handle @server init-req)
        (let [resp (sut/handle @server init-req)]
          (should-respond-invalid-req resp "Already received initialization request"))))
    )
  )