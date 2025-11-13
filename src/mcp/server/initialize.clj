(ns mcp.server.initialize
  (:require [c3kit.apron.schema :as schema]
            [c3kit.apron.time :as time]
            [mcp.core :as core]
            [mcp.server.errors :as errors]))

(defn maybe-unsupported-version [server req]
  (let [supported (time/parse :webform (:protocol-version server))
        requested (time/parse :webform (:protocolVersion (:params req)))]
    (when (time/after? requested supported)
      (errors/invalid-params (:id req)))))

(def initialize-params-schema
  {:protocolVersion {:type :string :validations [core/required]}})

(defn -initialize! [spec ratom req]
  (swap! ratom assoc :initialization :requested)
  {:jsonrpc "2.0"
   :id      (:id req)
   :result  {:protocolVersion (:protocol-version spec)
             :serverInfo      {:name    (:name spec)
                               :title   (:title spec)
                               :version (:server-version spec)}
             :capabilities    (merge {}
                                     (when (seq (:resources spec)) {:resources {}}))}})

(defn maybe-invalid-params [req id]
  (when (schema/error? req)
    (let [id (if (schema/error? id) nil id)]
      (errors/invalid-params id))))

(defn initialize! [spec ratom req]
  (let [conformed (schema/conform initialize-params-schema (:params req))]
    (or (maybe-invalid-params conformed (:id req))
        (maybe-unsupported-version spec req)
        (-initialize! spec ratom req))))

(defn confirm! [ratom _req]
  (when (= :requested (:initialization @ratom))
    (swap! ratom assoc :initialization :confirmed)
    nil))

(defn initializing? [req]
  (or (= "initialize" (:method req))))
(defn notifying? [req]
  (= "notifications/initialized" (:method req)))
(defn initialization [server]
  (-> server :state deref :initialization))
(defn initialized? [server]
  (= :confirmed (initialization server)))