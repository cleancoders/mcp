(ns mcp.server.core
  (:require [c3kit.apron.schema :as schema]
            [mcp.server.errors :as errors]
            [mcp.server.initialize :as init]))

(defn ->server [spec]
  (let [state (atom {})]
    (merge spec
           {:state        state
            :capabilities {"initialize" {:handler (partial init/initialize! spec state)}}})))

(def required {:validate schema/present? :message "is required"})
(defn =to [x] {:validate #(= x %) :message (format "must be equal to %s" x)})

(def rpc-request-schema
  {:jsonrpc {:type :string :validations [required (=to "2.0")]}
   :method  {:type :string :validations [required]}
   :id      {:type :long}
   :params  {:type :map :validations [required]}})

(defn maybe-bad-request [req]
  (when (schema/error? req)
    (errors/bad-request "The JSON sent is not a valid JSON-RPC request object")))

(defn maybe-uninitialized [server req]
  (when (and (not (init/initialize? req)))
    (errors/uninitialized (:id req))))

(defn maybe-incomplete-init [server req]
  (when (and (init/initialize? req) (= :requested (init/initialization server)))
    (errors/bad-request "Already received initialization request")))

(defn -handle [server req]
  (let [handler (-> server :capabilities (get (:method req)) :handler)]
    (handler req)))

(defn handle [server req]
  (let [conformed (schema/conform rpc-request-schema req)]
    (or (maybe-bad-request conformed)
        (maybe-uninitialized server conformed)
        (maybe-incomplete-init server conformed)
        (-handle server req))))