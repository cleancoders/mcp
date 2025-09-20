(ns mcp.server.stdio
  (:require [c3kit.apron.utilc :as utilc]
            [mcp.server.core :as server]
            [mcp.server.errors :as errors])
  (:import (java.io IOException)))

(defn- send-response! [response] (when response (-> response utilc/->json println)))

(defn- safe-read-line []
  (try (read-line)
    (catch IOException _ (throw (ex-info "Input stream from client severed" {})))))

(defn- parse-json-or-send-error [request]
  (try (utilc/<-json-kw request)
    (catch Exception _
      (send-response! (errors/invalid-request "Request is not a valid JSON string")))))

(defn- read->json [] (->> (safe-read-line) parse-json-or-send-error))

(defn handle-stdio [server]
  (when-let [request (read->json)]
    (-> (server/handle server request)
        send-response!)))