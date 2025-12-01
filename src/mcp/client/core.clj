(ns mcp.client.core
  (:require [c3kit.apron.corec :as ccc]
            [mcp.core :as core]))

(defn ->notifications-method [method]
  (keyword "notifications" (name method)))

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