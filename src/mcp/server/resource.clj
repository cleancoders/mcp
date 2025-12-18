(ns mcp.server.resource
  (:require [mcp.core :as core]
            [mcp.fs :as fs]))

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
    (core/->success (:id req) {:resources resources-for-list})))

(defn- resource-not-found-error [uri]
  {:code -32002 :message "Resource not found" :data {:uri uri}})

(defn- resource-not-registered-error [uri]
  {:code -32002 :message "Resource not registered" :data {:uri uri}})

(defn- read-resource [uri]
  (try
    (let [f (fs/->file (subs uri 7))]
      {:contents [{:uri      uri
                   :mimeType (fs/mime-type f)
                   :text     (fs/content f)}]})
    (catch Exception _
      nil)))

(defn ->read-handler [resources-by-uri]
  (fn [req]
    (let [uri (:uri (:params req))]
      (if-not (contains? resources-by-uri uri)
        (core/->error (:id req) (resource-not-registered-error uri))
        (if-let [result (read-resource uri)]
          (core/->success (:id req) result)
          (core/->error (:id req) (resource-not-found-error uri)))))))