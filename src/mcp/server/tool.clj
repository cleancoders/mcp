(ns mcp.server.tool
  (:require [c3kit.apron.doc :as doc]))

(def default-schema
  {:type       "object"
   :properties {}})

(defn ->list-handler [tools]
  (let [tools (reduce
                (fn [tools {:keys [inputSchema] :as tool}]
                  (let [schema (if (empty? inputSchema)
                                 default-schema
                                 (doc/apron->openapi-schema inputSchema))
                        tool   (-> (assoc tool :inputSchema schema)
                                   (dissoc :handler))]
                    (conj tools tool)))
                []
                tools)]
    (fn [req]
      {:jsonrpc "2.0"
       :id      (:id req)
       :result  {:tools tools}})))

(defn with-tool [server-spec tool-spec]
  (update server-spec :tools conj tool-spec))