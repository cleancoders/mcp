(ns mcp.server.core
  (:require [c3kit.apron.schema :as schema]
            [c3kit.apron.time :as time]
            [mcp.server.errors :as errors]))

(defn maybe-unsupported-version [server req]
  (let [supported (time/parse :webform (:protocol-version server))
        requested (time/parse :webform (:protocolVersion (:params req)))]
    (when (time/after? requested supported)
      (errors/unsupported-protocol (:id req)))))

(defn -initialize! [spec ratom req]
  (swap! ratom assoc :initialization :requested)
  {:jsonrpc "2.0"
   :id      (:id req)
   :result  {:protocolVersion (:protocol-version spec)
             :serverInfo      {:name    (:name spec)
                               :title   (:title spec)
                               :version (:server-version spec)}}})
(defn initialize! [spec ratom req]
  (or (maybe-unsupported-version spec req)
      (-initialize! spec ratom req)))

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

(defn maybe-bad-request [req]
  (when (schema/error? req)
    (errors/bad-request "The JSON sent is not a valid JSON-RPC request object")))

(defn maybe-uninitialized [server req]
  (when (and (not (initialize? req)))
    (errors/uninitialized (:id req))))

(defn maybe-incomplete-init [server req]
  (when (and (initialize? req) (= :requested (initialization server)))
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