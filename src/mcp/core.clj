(ns mcp.core
  (:require [c3kit.apron.schema :as schema]))

(def protocol-version "2025-06-18")
(def rpc-version "2.0")

(defn with-version [payload]
  (assoc payload :jsonrpc rpc-version))

(defn ->success [id result]
  {:jsonrpc rpc-version :id id :result result})

(defn ->error
  ([error] {:jsonrpc rpc-version :error error})
  ([id error] {:jsonrpc rpc-version :id id :error error}))

(def required {:validate schema/present? :message "is required"})