(ns mcp.server.errors)

(def errors
  {:invalid-request {:code -32600 :message "Invalid Request"}
   :invalid-method  {:code -32601 :message "Method not found"}
   :uninitialized   {:code -32002 :message "Server not initialized"}
   :invalid-params  {:code -32602 :message "Invalid Parameters"}})

(defn ->rpc-error
  ([kind data] (->rpc-error kind nil data))
  ([kind id data] {:jsonrpc "2.0"
                   :id      id
                   :error   (merge (errors kind) {:data data})}))

(defn uninitialized [id]
  (->rpc-error :uninitialized id "Must initialize connection before invoking methods"))
(defn invalid-request
  ([data] (->rpc-error :invalid-request data))
  ([id data] (->rpc-error :invalid-request id data)))
(defn invalid-method [id method]
  (->rpc-error :invalid-method id (format "Method '%s' is not supported" method)))
(defn invalid-params [id]
  (->rpc-error :invalid-params id {:supported ["2025-06-18"]}))