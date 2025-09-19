(ns mcp.server.initialize
  (:require [c3kit.apron.time :as time]
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