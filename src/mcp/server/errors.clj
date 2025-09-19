(ns mcp.server.errors)

(defn ->rpc-error
  ([id code message data]
   (merge (->rpc-error code message data) {:id id}))
  ([code message data]
   {:jsonrpc "2.0"
    :error   {:code code :message message :data data}}))

(defn uninitialized [id]
  (->rpc-error id -32002 "Server not initialized" "Must initialize connection before invoking methods"))
(defn bad-request [msg]
  (->rpc-error -32600 "Invalid Request" msg))
(defn unsupported-protocol [id]
  (->rpc-error id -32602 "Unsupported protocol version" {:supported ["2025-06-18"]}))