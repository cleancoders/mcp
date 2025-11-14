(ns mcp.core
  (:require [c3kit.apron.schema :as schema]))

(def protocol-version "2025-06-18")
(def rpc-version "2.0")

(defn with-version [base]
  (merge base {:jsonrpc rpc-version}))

(def required {:validate schema/present? :message "is required"})