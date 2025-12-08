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

(defn- maybe-save-response! [responses-atom resp id]
  (let [resp-id (:id (utilc/<-json-kw resp))]
    (if-not (= resp-id id)
      (do (swap! responses-atom assoc resp-id resp)
          nil)
      resp)))

(defn- read-until-id! [transport responses-atom id]
  (loop [resp (read! transport)]
    (or (maybe-save-response! responses-atom resp id)
        (recur (read! transport)))))

(defn- pluck-response! [responses-atom id]
  (when-let [saved-response (get @responses-atom id)]
    (swap! responses-atom dissoc id)
    saved-response))

(defn- fetch-response! [transport responses-atom id]
  (or (pluck-response! responses-atom id)
      (read-until-id! transport responses-atom id)))

(def responses-atom (atom {}))

(defn raw-request! [transport jrpc-payload]
  (let [req-id (:id (utilc/<-json-kw jrpc-payload))]
    (send! transport jrpc-payload)
    (delay (fetch-response! transport responses-atom req-id))))

(defn request! [transport edn-rpc-payload]
  (send! transport (utilc/->json edn-rpc-payload))
  (delay (utilc/<-json-kw (fetch-response! transport responses-atom (:id edn-rpc-payload)))))

(defn notify-initialized! [transport]
  (send! transport (utilc/->json initialized-notification))
  nil)
