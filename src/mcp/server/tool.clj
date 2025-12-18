(ns mcp.server.tool
  (:require [c3kit.apron.doc :as doc]
            [c3kit.apron.utilc :as utilc]
            [mcp.core :as core]))

(def default-schema
  {:type       "object"
   :properties {}})

(defn- ->listable-tool [{:keys [inputSchema] :as tool}]
  (let [schema (if (empty? inputSchema)
                 default-schema
                 (doc/apron->openapi-schema inputSchema))]
    (-> tool
        (assoc :inputSchema schema)
        (dissoc :handler))))

(defn ->tools-for-list [tools]
  (mapv ->listable-tool tools))

(defn ->tools-by-name [tools]
  (reduce (fn [m tool] (assoc m (:name tool) tool)) {} tools))

(defn ->list-handler [tools-for-list]
  (fn [req]
    {:jsonrpc "2.0"
     :id      (:id req)
     :result  {:tools tools-for-list}}))

(defn with-tool [server-spec tool-spec]
  (update server-spec :tools conj tool-spec))

(defn ->call-handler [tools-by-name]
  (fn [{:keys [params] :as req}]
    (if-let [{:keys [handler]} (get tools-by-name (:name params))]
      (core/with-version
        {:id (:id req)
         :result {:content [{:type "text" :text (utilc/->json (handler req))}]}})
      (core/with-version
        {:id (:id req)
         :error
         {:code    -32602
          :message (format "Unknown tool: %s" (:name params))}}))))