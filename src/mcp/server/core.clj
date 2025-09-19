(ns mcp.server.core
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.schema :as schema]
            [c3kit.apron.time :as time]))

(defn initialize! [spec ratom req]
  (swap! ratom assoc :initialization :requested)
  {:jsonrpc "2.0"
   :id      (:id req)
   :result  {:protocolVersion (:protocol-version spec)
             :serverInfo      {:name    (:name spec)
                               :title   (:title spec)
                               :version (:server-version spec)}}})
(defn initialize? [req]
  (= "initialize" (:method req)))
(defn initialization [server]
  (-> server :state deref :initialization))

(defn ->server [spec]
  (let [state (atom {})]
    (merge spec
           {:state        state
            :capabilities {"initialize" {:handler (partial initialize! spec state)}}})))

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

(defn uninitialized [id]
  (->rpc-error id -32002 "Server not initialized" "Must initialize connection before invoking methods"))
(defn bad-request [msg]
  (->rpc-error -32600 "Invalid Request" msg))
(defn unsupported-protocol [id]
  (->rpc-error id -32602 "Unsupported protocol version" {:supported ["2025-06-18"]}))

(defn maybe-bad-request [req]
  (when (schema/error? req)
    (bad-request "The JSON sent is not a valid JSON-RPC request object")))

(defn maybe-uninitialized [server req]
  (when (and (not (initialize? req)))
    (uninitialized (:id req))))

(defn maybe-incomplete-init [server req]
  (when (and (initialize? req) (= :requested (initialization server)))
    (bad-request "Already received initialization request")))

(defn maybe-unsupported-version [server req]
  (let [supported (time/parse "YYYY-MM-DD" (:protocol-version server))
        ;requested (time/parse "YYYY_MM_DD" )
        ]
    (prn "supported: " supported)
    ))

(defn -handle [server req]
  (let [handler (-> server :capabilities (get (:method req)) :handler)]
    (handler req)))

(defn handle [server req]
  (let [conformed (schema/conform rpc-request-schema req)]
    (or (maybe-bad-request conformed)
        (maybe-uninitialized server conformed)
        (maybe-incomplete-init server conformed)
        (-handle server req))))