(ns mcp.client.core
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.utilc :as utilc]
            [mcp.core :as core]))

(defn ->notifications-method [method]
  (str "notifications/" (name method)))

(defn build-notification [method]
  (-> {:method (->notifications-method method)}
      core/with-version))

(defn build-request
  ([id method] (build-request id method {}))
  ([id method & params]
   (-> {:id     id
        :method method
        :params (ccc/->options params)}
       core/with-version)))

(defn ->client [client-info]
  {:protocolVersion core/protocol-version
   :capabilities {:roots       {:listChanged true}
                  :sampling    {}
                  :elicitation {}}
   :clientInfo client-info})

(defn ->initialize-request [client]
  (build-request 1 "initialize" client))

(def initialized-notification (build-notification "initialized"))

(defprotocol Transport
  (send! [this jrpc-payload])
  (read! [this]))

(defn raw-request! [transport jrpc-payload]
  (send! transport jrpc-payload)
  (read! transport))

(defn request! [transport edn-rpc-payload]
  (->> edn-rpc-payload
       utilc/->json
       (raw-request! transport)
       utilc/<-json-kw))

(defn request-initialize! [transport client]
  (request! transport (->initialize-request client)))

(defn notify-initialized! [transport]
  (send! transport (utilc/->json initialized-notification))
  nil)
