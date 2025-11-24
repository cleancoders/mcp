(ns mcp.server.http-spec
  (:require [c3kit.apron.utilc :as utilc]
            [mcp.server.core :as server]
            [mcp.server.http :as sut]
            [mcp.server.spec-helper :as server-helper]
            [speclj.core :refer :all]))

(declare spec)
(declare server)
(declare request)
(declare http-config)
(declare origin)
(declare origin-1)

(defn- with-origin [request origin]
  (assoc-in request [:headers "Origin"] origin))

(defn- init-resp [spec]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (utilc/->json (server-helper/initialized-response spec))})

(def forbidden-resp
  {:status 403
   :headers {"Content-Type" "text/plain"}
   :body "Forbidden: Invalid origin"})

(describe "http"

  (with spec {:name             "Test Server"
              :server-version   "1.0.0"
              :protocol-version "2025-06-18"
              :capabilities     {"experimental/foo" {:handler (fn [_] :handled)}}})
  (with server (server/->server @spec))

  (context "origin validation"
    (with request {:request-method :post
                   :headers {"Origin" "http://localhost"
                             "Accept" "application/json"}
                   :body (utilc/->json server-helper/init-req)})

    (context "default config"
      (it "forbids non-localhost"
        (should= forbidden-resp
                 (sut/handle-request (with-origin @request "http://not-localhost") @server))
        (should= forbidden-resp
                 (sut/handle-request (with-origin @request "https://also-not-localhost:512") @server))
        (should= forbidden-resp
                 (sut/handle-request (with-origin @request "http://0.0.0.0") @server)))

      (it "accepts http://localhost"
        (should= (init-resp @spec) (sut/handle-request @request @server)))

      (it "accepts https://localhost"
        (should= (init-resp @spec)
                 (sut/handle-request (with-origin @request "https://localhost") @server)))

      (it "accepts http://127.0.0.1"
        (should= (init-resp @spec)
                 (sut/handle-request (with-origin @request "http://127.0.0.1") @server)))

      (it "accepts https://127.0.0.1"
        (should= (init-resp @spec)
                 (sut/handle-request (with-origin @request "https://127.0.0.1") @server)))

      (it "accepts http://[::1]"
        (should= (init-resp @spec)
                 (sut/handle-request (with-origin @request "http://[::1]") @server)))

      (it "accepts https://[::1]"
        (should= (init-resp @spec)
                 (sut/handle-request (with-origin @request "https://[::1]") @server)))

      (context "localhost with any port"
        (it "accepts https://localhost:8080"
          (should= (init-resp @spec)
                   (sut/handle-request (with-origin @request "https://localhost:8080") @server)))

        (it "accepts http://127.0.0.1:1234"
          (should= (init-resp @spec)
                   (sut/handle-request (with-origin @request "http://127.0.0.1:1234") @server)))))

    (context "custom origin validation"
      (it "allows an exact match"
        (let [origin "http://my-origin.com"]
          (should= (init-resp @spec)
                   (sut/handle-request
                     (with-origin @request origin)
                     @server
                     {:allowed-origins #{(re-pattern origin)}}))))

      (it "forbids a non-match"
        (let [origin "http://my-origin.com"]
          (should= forbidden-resp
                   (sut/handle-request
                     (with-origin @request "http://not-my-origin.com")
                     @server
                     {:allowed-origins #{(re-pattern origin)}}))))

      (context "allows exact match from multiple options"

        (with origin "http://my-origin.com")
        (with origin-1 "https://other-origin.com")
        (with http-config {:allowed-origins #{(re-pattern @origin) (re-pattern @origin-1)}})

        (it "http://my-origin.com"
          (should= (init-resp @spec)
                   (sut/handle-request (with-origin @request @origin) @server @http-config)))

        (it "https://other-origin.com"
          (should= (init-resp @spec)
                   (sut/handle-request (with-origin @request @origin-1) @server @http-config))))

      (context "allows regex match"

        (with origin "http://my-origin.*")

        (it "http://my-origin.net"
          (should= (init-resp @spec)
                   (sut/handle-request
                     (with-origin @request "http://my-origin.net")
                     @server
                     {:allowed-origins #{(re-pattern @origin)}})))

        (it "http://my-origin.com"
          (should= (init-resp @spec)
                   (sut/handle-request
                     (with-origin @request "http://my-origin.com")
                     @server
                     {:allowed-origins #{(re-pattern @origin)}}))))

      (it "forbids regex non-match"
        (let [origin "http://my-origin.*"]
          (should= forbidden-resp
                   (sut/handle-request
                     (with-origin @request "http://my-or1gin.com")
                     @server
                     {:allowed-origins #{(re-pattern origin)}}))))))

  #_(it "handles POST request"
    (should= {}
             (sut/handle-request @request @server))))