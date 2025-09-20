(ns mcp.server.core
  (:require [c3kit.apron.schema :as schema]
            [mcp.core :as core]
            [mcp.server.resource :as resource]
            [mcp.server.errors :as errors]
            [mcp.server.initialize :as init]
            [medley.core :as medley]))

(defn ->default-handlers [spec state]
  {"initialize"                {:handler (partial init/initialize! spec state)}
   "notifications/initialized" {:handler (partial init/confirm! state)}})

(defn with-resource-lists [capabilities {:keys [resources]}]
  (if (seq resources)
    (assoc capabilities "resources/list" {:handler (resource/->list-handler resources)})
    capabilities))

(defn ->server [spec]
  (let [state (atom {})]
    (medley/deep-merge
      spec
      {:state        state
       :capabilities (-> (->default-handlers spec state)
                         (with-resource-lists spec))})))

(defn =to [x] {:validate #(= x %) :message (format "must be equal to %s" x)})
(defn string-or-long? [x] (or (string? x) (int? x) (char? x)))

(def rpc-request-schema
  {:jsonrpc {:type :string :validations [core/required (=to "2.0")]}
   :method  {:type :string :validations [core/required]}
   :id      {:type #{:long :string} :validate string-or-long?}
   :params  {:type :map}})

(defn maybe-bad-request [req]
  (let [req (schema/validate rpc-request-schema req)
        id  (delay (if (schema/error? (:id req)) nil (:id req)))]
    (when (schema/error? req)
      (errors/invalid-request @id "The JSON sent is not a valid JSON-RPC request object"))))

(defn maybe-uninitialized [server req]
  (when (not (or (init/initializing? req)
                 (init/notifying? req)
                 (init/initialized? server)))
    (errors/uninitialized (:id req))))

(defn maybe-incomplete-init [server req]
  (when (and (init/initializing? req)
             (= :requested (init/initialization server)))
    (errors/invalid-request "Already received initialization request")))

(defn -handle [server req]
  (if-let [handler (-> server :capabilities (get (:method req)) :handler)]
    (handler req)
    (errors/invalid-method (:id req) (:method req))))

(defn handle [server req]
  (let [conformed (schema/conform rpc-request-schema req)]
    (or (maybe-bad-request req)
        (maybe-uninitialized server conformed)
        (maybe-incomplete-init server conformed)
        (-handle server req))))