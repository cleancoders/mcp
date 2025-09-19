(ns mcp.server.stdio
  (:require [c3kit.apron.utilc :as utilc]
            [mcp.server.core :as server]
            [mcp.server.errors :as errors])
  (:import (java.io IOException)))

(defn- send-response! [response]
  (-> response utilc/->json println))

(defn- maybe-send-response! [response]
  (when response (send-response! response)))

(defn- safe-read-line []
  (try
    (read-line)
    (catch IOException _
      (throw (ex-info "Input stream from client severed" {})))))

(defn- parse-json [request]
  (try
    (utilc/<-json-kw request)
    (catch Exception _
      (send-response! (errors/invalid-request "Request is not a valid JSON string")))))

(defn handle-stdio [server]
  (->> (safe-read-line)
       parse-json
       (server/handle server)
       maybe-send-response!))
