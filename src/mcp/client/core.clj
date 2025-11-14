(ns mcp.client.core
  (:require [c3kit.apron.corec :as ccc]
            [mcp.core :as core]))

(def client-info {:name    "ExampleClient"
                  :title   "Example Client Display Name"
                  :version "1.0.0"})

(def params {:protocolVersion core/protocol-version
             :capabilities    {:roots       {:listChanged true}
                               :sampling    {}
                               :elicitation {}}
             :clientInfo      client-info})

;---^^ example code while we build, probably needs to move to spec ^^ ----
;--- Tested code below ---;

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