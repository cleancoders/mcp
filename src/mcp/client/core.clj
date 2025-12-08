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

(defn raw-json-request! [transport jrpc-payload]
  (let [req-id (:id (utilc/<-json-kw jrpc-payload))]
    (send! transport jrpc-payload)
    (delay (fetch-response! transport responses-atom req-id))))

(defn raw-request! [transport rpc-payload]
  (send! transport (utilc/->json rpc-payload))
  (delay (utilc/<-json-kw (fetch-response! transport responses-atom (:id rpc-payload)))))

(defn ->initialize-request [{:keys [next-id-fn client] :as _config}]
  (build-request (next-id-fn) "initialize" client))

(defn request-initialize! [{:keys [transport] :as config}]
  (raw-request! transport (->initialize-request config)))

(defn notify-initialized! [transport]
  (send! transport (utilc/->json initialized-notification))
  nil)

(defn initialize! [{:keys [transport] :as config}]
  (let [init-resp (request-initialize! config)]
    (notify-initialized! transport)
    init-resp))