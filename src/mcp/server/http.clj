(ns mcp.server.http
  (:require [c3kit.apron.utilc :as utilc]
            [mcp.server.core :as server]
            [mcp.server.errors :as errors]))

(def localhost-regex #"^https?://(localhost|127\.0\.0\.1|\[::1\])(:\d+)?$")

(defn- matches? [re s]
  (boolean (seq (re-matches re s))))

(defn- http-req->rpc [request]
  (try
    (utilc/<-json-kw (slurp (:body request)))
    (catch Exception _e)))

(defn- handle [rpc-req server]
  (if rpc-req
    (server/handle server rpc-req)
    (errors/invalid-request "Request is not a valid JSON string")))

(defn- rpc->http-resp [resp]
  (cond-> {:status 200}
          resp (merge {:headers {"Content-Type" "application/json"}
                       :body (utilc/->json resp)})))

(defn- handled-response [server request]
  (-> (http-req->rpc request)
      (handle server)
      rpc->http-resp))

;; should this be plain text?
(def forbidden-response
  {:status 403
   :headers {"Content-Type" "text/plain"}
   :body "Forbidden: Invalid origin"})

(defn- allowed-origin? [origin http-config]
  (when origin
    (some #(matches? % origin) (:allowed-origins http-config))))

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

