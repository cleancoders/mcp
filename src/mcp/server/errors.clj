(ns mcp.server.errors)

(def errors
  {:invalid-request      {:code -32600 :message "Invalid Request"}
   :uninitialized        {:code -32002 :message "Server not initialized"}
   :unsupported-protocol {:code -32602 :message "Unsupported protocol version"}})

(defn ->rpc-error
  ([kind data] (->rpc-error kind nil data))
  ([kind id data] {:jsonrpc "2.0"
                   :id      id
                   :error   (merge (kind errors) {:data data})}))

(defn uninitialized [id]
  (->rpc-error :uninitialized id "Must initialize connection before invoking methods"))
(defn invalid-request
  ([data] (->rpc-error :invalid-request data))
  ([id data] (->rpc-error :invalid-request id data)))
(defn unsupported-protocol [id]
  (->rpc-error :unsupported-protocol id {:supported ["2025-06-18"]}))