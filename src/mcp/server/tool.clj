(ns mcp.server.tool
  (:require [c3kit.apron.doc :as doc]
            [c3kit.apron.utilc :as utilc]
            [mcp.core :as core]))

(def empty-object-schema
  {:type       "object"
   :properties {}})

(defn- ->openapi-schema [apron-schema]
  (if (seq apron-schema)
    (doc/apron->openapi-schema apron-schema)
    empty-object-schema))

(defn- ->openapi-schema-or-nil [apron-schema]
  (when (seq apron-schema)
    (doc/apron->openapi-schema apron-schema)))

(defn- ->listable-tool [{:keys [inputSchema outputSchema] :as tool}]
  (cond-> (dissoc tool :handler)
    true         (assoc :inputSchema (->openapi-schema inputSchema))
    outputSchema (assoc :outputSchema (->openapi-schema-or-nil outputSchema))))

(defn ->tools-for-list [tools]
  (mapv ->listable-tool tools))

(defn ->tools-by-name [tools]
  (reduce (fn [acc tool] (assoc acc (:name tool) tool)) {} tools))

(defn ->list-handler [tools-for-list]
  (fn [req]
    (core/->success (:id req) {:tools tools-for-list})))

(defn with-tool
  "Adds a tool to the server specification.

  Tool spec keys:
    :name        - (required) unique identifier for the tool
    :description - (required) description shown to the LLM
    :handler     - (required) (fn [req] ...) invoked on tools/call
    :inputSchema  - apron schema for tool arguments
    :outputSchema - apron schema for structuredContent response
    :title        - human-readable display name
    :annotations  - additional MCP metadata

  Handler return formats:

    Plain value (legacy) - wrapped as text content:
      (fn [req] {:files [\"a.txt\"]})

    Structured response - explicit control:
      (fn [req]
        {:structured {:files [\"a.txt\"]}   ; -> structuredContent
         :content [{:type \"text\" :text \"Found 1 file\"}]
         :error? true})                     ; -> isError

  Content block types:
    {:type \"text\"     :text \"...\"}
    {:type \"image\"    :data \"base64...\" :mimeType \"image/png\"}
    {:type \"resource\" :resource {:uri \"...\" :mimeType \"...\"}}"
  [server-spec tool-spec]
  (update server-spec :tools conj tool-spec))

(defn- unknown-tool-error [tool-name]
  {:code -32602 :message (format "Unknown tool: %s" tool-name)})

(defn- structured-result? [{:keys [structured content error?]}]
  (or structured content error?))

(defn- ->structured-result [{:keys [structured content error?]}]
  (cond-> {}
    structured (assoc :structuredContent structured)
    content    (assoc :content content)
    error?     (assoc :isError true)))

(defn- ->text-result [value]
  {:content [{:type "text" :text (utilc/->json value)}]})

(defn- ->tool-result [handler-result]
  (if (structured-result? handler-result)
    (->structured-result handler-result)
    (->text-result handler-result)))

(defn ->call-handler [tools-by-name]
  (fn [{:keys [params] :as req}]
    (if-let [{:keys [handler]} (get tools-by-name (:name params))]
      (core/->success (:id req) (->tool-result (handler req)))
      (core/->error (:id req) (unknown-tool-error (:name params))))))