(ns mcp.server.core
  (:require [c3kit.apron.schema :as schema]))

(defn ->server [spec]
  )

(def required {:validate schema/present? :message "is required"})
(defn =to [x] {:validate #(= x %) :message (format "must be equal to %s" x)})

(def rpc-request-schema
  {:jsonrpc {:type :string :validations [required (=to "2.0")]}
   :method  {:type :string :validations [required]}
   :id      {:type :long}
   :params  {:type :map :validations [required]}})

(defn ->rpc-error
  ([id code message data]
   (merge (->rpc-error code message data) {:id id}))
  ([code message data]
   {:jsonrpc "2.0"
    :error   {:code code :message message :data data}}))

(defn bad-request []
  (->rpc-error -32600 "Invalid Request" "The JSON sent is not a valid JSON-RPC request object"))
(defn unsupported-protocol [id]
  (->rpc-error 1 -32602 "Unsupported protocol version" {:supported ["2024-11-05"]}))

(defn handle [server req]
  (let [conformed (schema/conform rpc-request-schema req)]
    (if (schema/error? conformed)
      (bad-request)
      (unsupported-protocol 1))))