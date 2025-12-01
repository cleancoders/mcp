(ns mcp.server.resource
  (:require [mcp.fs :as fs]))

(defn with-resource [server-spec resource-spec]
  (update server-spec :resources conj resource-spec))

(defn ->list-handler [resources]
  (let [coll (reduce
               (fn [coll {:keys [path]}]
                 (let [f (fs/->file path)]
                   (conj coll
                         {:uri      (format "file://%s" path)
                          :name     (name f)
                          :mimeType (fs/mime-type f)})))
               [] resources)]
    (fn [req]
      {:jsonrpc "2.0" :id (:id req) :result {:resources coll}})))

(defn- read-resource [uri]
  (try
    (let [f (fs/->file (subs uri 7))]
      {:result
       {:contents [{:uri      uri
                    :mimeType (fs/mime-type f)
                    :text     (fs/content f)}]}})
    (catch Exception e
      {:error
       {:code    -32002
        :message "Resource not found"
        :data    {:uri uri}}})))

; needs to support different resource types
(defn ->read-handler [resources]
  (fn [req]
    (merge
      {:jsonrpc "2.0"
       :id      (:id req)}
      (read-resource (:uri (:params req)))))
  )