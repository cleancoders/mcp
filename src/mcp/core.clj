(ns mcp.core)

(def protocol-version "2024-11-05")
(def rpc-version "2.0")

(defn add-rpc-version [base]
  (merge base {:jsonrpc rpc-version}))
