(ns mcp.server.tool
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.doc :as doc]
            [c3kit.apron.utilc :as utilc]
            [mcp.core :as core]))

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

(defn ->call-handler [tools]
  (fn [{:keys [params] :as req}]
    (let [{:keys [handler] :as tool} (ccc/ffilter #(= (:name params) (:name %)) tools)]
      (if tool
        (core/with-version
          {:id (:id req)
           :result {:content [{:type "tool_use" :text (utilc/->json (handler req))}]}})
        (core/with-version
          {:id (:id req)
           :error
           {:code    -32602
            :message (format "Unknown tool: %s" (:name params))}})))))