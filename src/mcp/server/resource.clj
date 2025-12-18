(ns mcp.server.resource
  (:require [mcp.fs :as fs]))

(defn with-resource [server-spec resource-spec]
  (update server-spec :resources conj resource-spec))

(defn- ->listable-resource [{:keys [path]}]
  (let [f (fs/->file path)]
    {:uri      (format "file://%s" path)
     :name     (fs/name f)
     :mimeType (fs/mime-type f)}))

(defn ->resources-for-list [resources]
  (mapv ->listable-resource resources))

(defn ->resources-by-uri [resources]
  (reduce (fn [m {:keys [path] :as resource}]
            (assoc m (format "file://%s" path) resource))
          {} resources))

(defn ->list-handler [resources-for-list]
  (fn [req]
    {:jsonrpc "2.0" :id (:id req) :result {:resources resources-for-list}}))

(defn- read-resource [uri]
  (try
    (let [f (fs/->file (subs uri 7))]
      {:result
       {:contents [{:uri      uri
                    :mimeType (fs/mime-type f)
                    :text     (fs/content f)}]}})
    (catch Exception _
      {:error
       {:code    -32002
        :message "Resource not found"
        :data    {:uri uri}}})))

(defn- resource-not-registered [uri]
  {:error
   {:code    -32002
    :message "Resource not registered"
    :data    {:uri uri}}})

(defn ->read-handler [resources-by-uri]
  (fn [req]
    (let [uri (:uri (:params req))]
      (merge
        {:jsonrpc "2.0"
         :id      (:id req)}
        (if (contains? resources-by-uri uri)
          (read-resource uri)
          (resource-not-registered uri))))))