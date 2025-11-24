(ns mcp.server.http
  (:require [c3kit.apron.utilc :as utilc]
            [mcp.server.core :as server]))

(def localhost-regex #"^https?://(localhost|127\.0\.0\.1|\[::1\])(:\d+)?$")

(defn- matches? [re s]
  (boolean (seq (re-matches re s))))

(defn- handled-response [server request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (utilc/->json (server/handle server (utilc/<-json-kw (:body request))))})

;; should this be plain text?
(def forbidden-response
  {:status 403
   :headers {"Content-Type" "text/plain"}
   :body "Forbidden: Invalid origin"})

(defn- allowed-origin? [origin http-config]
  (some #(matches? % origin) (:allowed-origins http-config)))

(defn- ->origin [req]
  (get-in req [:headers "Origin"]))

(def default-http-config {:allowed-origins #{localhost-regex}})

;; TODO - support text/event-stream content-type & lots more
(defn handle-request
  ([request server]
   (handle-request request server default-http-config))
  ([request server http-config]
   (if (allowed-origin? (->origin request) http-config)
     (handled-response server request)
     forbidden-response)))

