(ns mcp.server.initialize-spec
  (:require [mcp.server.core :as server]
            [mcp.server.initialize :as sut]
            [mcp.server.spec-helper :as server-helper]
            [medley.core :as medley]
            [speclj.core :refer :all]))

(declare spec)
(declare server)
(declare req)

(describe "Server Initialization Handshake"

  (with spec {:name             "Test Server"
              :server-version   "1.0.0"
              :protocol-version "2025-06-18"
              :capabilities     {"experimental/foo" {:handler (fn [_req] :handled)}}})
  (with server (server/->server @spec))

  (context "lifecycle"

    (context "rejects messages"

      (it "before initialization"
        (let [{:keys [error] :as resp} (server/handle @server server-helper/foo-req)]
          (should= "2.0" (:jsonrpc resp))
          (should= 2 (:id resp))
          (should= -32002 (:code error))
          (should= "Server not initialized" (:message error))
          (should= "Must initialize connection before invoking methods" (:data error))))

      (it "after notification, but before initialization"
        (server/handle @server {:jsonrpc "2.0" :method "notifications/initialized"})
        (let [{:keys [error] :as resp} (server/handle @server server-helper/foo-req)]
          (should= "2.0" (:jsonrpc resp))
          (should= 2 (:id resp))
          (should= -32002 (:code error))
          (should= "Server not initialized" (:message error))
          (should= "Must initialize connection before invoking methods" (:data error))))

      (it "from clients with no protocol version"
        (let [req (medley/dissoc-in server-helper/init-req [:params :protocolVersion])
              {:keys [error] :as resp} (server/handle @server req)]
          (should= "2.0" (:jsonrpc resp))
          (should= 1 (:id resp))
          (should= -32602 (:code error))
          (should= "Invalid Parameters" (:message error))
          (should= ["2025-06-18"] (:supported (:data error)))))

      (it "from clients with a newer version"
        (let [req (assoc-in server-helper/init-req [:params :protocolVersion] "2025-06-19")
              {:keys [error] :as resp} (server/handle @server req)]
          (should= "2.0" (:jsonrpc resp))
          (should= 1 (:id resp))
          (should= -32602 (:code error))
          (should= "Invalid Parameters" (:message error))
          (should= ["2025-06-18"] (:supported (:data error))))))

    (context "responds to initialization request"

      (it "with server info"
        (let [{:keys [result] :as resp} (server/handle @server server-helper/init-req)
              server-info (:serverInfo result)]
          (should= "2.0" (:jsonrpc resp))
          (should= 1 (:id resp))
          (should= "2025-06-18" (:protocolVersion result))
          (should= "Test Server" (:name server-info))
          (should= "1.0.0" (:version server-info))))

      (it "with title"
        (let [server (server/->server (assoc @spec :title "The Title"))
              resp   (server/handle server server-helper/init-req)]
          (should= "The Title" (-> resp :result :serverInfo :title))))

      (it "only once"
        (server/handle @server server-helper/init-req)
        (let [resp (server/handle @server (assoc server-helper/init-req :id 2))]
          (server-helper/should-respond-invalid-req resp "Already received initialization request" 2)))

      ; with capabilities
      )

    (it "receives initialization notification"
      (server/handle @server server-helper/init-req)
      (should-be-nil (server/handle @server {:jsonrpc "2.0" :method "notifications/initialized"}))
      (should= :confirmed (sut/initialization @server))
      )
    )
  )