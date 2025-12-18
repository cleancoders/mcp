(ns mcp.server.errors
  (:require [mcp.core :as core]))

(def error-codes
  {:invalid-request {:code -32600 :message "Invalid Request"}
   :invalid-method  {:code -32601 :message "Method not found"}
   :uninitialized   {:code -32002 :message "Server not initialized"}
   :invalid-params  {:code -32602 :message "Invalid Parameters"}})

(defn- ->error-body [kind data]
  (merge (error-codes kind) {:data data}))

(defn uninitialized [id]
  (core/->error id (->error-body :uninitialized "Must initialize connection before invoking methods")))

(defn invalid-request
  ([data] (core/->error (->error-body :invalid-request data)))
  ([id data] (core/->error id (->error-body :invalid-request data))))

(defn invalid-method [id method]
  (core/->error id (->error-body :invalid-method (format "Method '%s' is not supported" method))))

(defn invalid-params [id]
  (core/->error id (->error-body :invalid-params {:supported ["2025-06-18"]})))