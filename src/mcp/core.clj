(ns mcp.core)

(def protocol-version "2024-11-05")

(defn add-rpc-version [base]
  (merge base {:jsonrpc "2.0"}))
