(ns mcp.server.stdio
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.log :as log]
            [c3kit.apron.utilc :as utilc]
            [mcp.server.core :as server]
            [mcp.server.errors :as errors])
  (:import (java.io IOException)))

(defn- without-nils [resp]
  (clojure.walk/postwalk
    #(if (map? %)
       (ccc/remove-nils %)
       %)
    resp))

(defn- send-response! [response]
  (when response
    (-> response without-nils utilc/->json println)
    (flush)))

(defn- safe-read-line []
  (try (read-line)
    (catch IOException _ (throw (ex-info "Input stream from client severed" {})))))

(defn- parse-json-or-send-error [request]
  (try
    (utilc/<-json-kw request)
    (catch Exception _
      (send-response! (errors/invalid-request "Request is not a valid JSON string")))))

(defn- read->json []
  (when-let [line (safe-read-line)]
    (parse-json-or-send-error line)))

(defn handle-stdio [server]
  (try
    (when-let [request (read->json)]
      (-> (server/handle server request)
          send-response!))
    (catch Exception e
      (log/error e))))