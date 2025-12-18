(ns mcp.server.resource-spec
  (:require [c3kit.apron.time :as time]
            [mcp.fs :as fs]
            [mcp.server.core :as server]
            [mcp.server.resource :as sut]
            [mcp.server.spec-helper :as server-helper]
            [speclj.core :refer :all]))

(declare handler)
(declare spec)

(def files
  {"/foo/bar.clj" {:content       (.getBytes "baz")
                   :mime-type     "text/html"
                   :name          "bar.clj"
                   :last-modified (str (time/now))}})

(describe "resources"

  (with-stubs)
  (with spec {:name             "Test Server"
              :server-version   "1.0.0"
              :protocol-version "2025-06-18"})
  (redefs-around [time/now (constantly (time/now))])

  (it "undefined by default"
    (let [server (server/->server @spec)
          _      (server-helper/initialize! server)
          resp   (server/handle server (server-helper/->req {:method "resources/list" :id 2}))]
      (server-helper/should-respond-unknown-method resp "Method 'resources/list' is not supported" 2)))

  (context "->list-handler"

    (it "returns resources"
      (with-redefs [fs/->file (partial fs/->mem-file files)]
        (let [resources-for-list (sut/->resources-for-list [{:kind :file :path "/foo/bar.clj"}])
              handler (sut/->list-handler resources-for-list)
              req  {:jsonrpc "2.0"
                    :id      1
                    :method  "resources/list"}
              resp (handler req)
              resource (-> resp :result :resources first)]
          (should= "2.0" (:jsonrpc resp))
          (should= 1 (:id resp))
          (should= "file:///foo/bar.clj" (:uri resource))
          (should= "bar.clj" (:name resource))
          (should= "text/html" (:mimeType resource))))))

  (context "->read-handler"

    (it "fails when resource not registered"
      (let [resources-by-uri (sut/->resources-by-uri [{:kind :file :path "/foo/bar.clj"}])
            handler (sut/->read-handler resources-by-uri)
            req {:jsonrpc "2.0"
                 :id      1
                 :method  "resources/read"
                 :params  {:uri "file:///foo/baz.clj"}}
            {:keys [error] :as resp} (handler req)]
        (should= "2.0" (:jsonrpc resp))
        (should= 1 (:id resp))
        (should= -32002 (:code error))
        (should= "Resource not registered" (:message error))
        (should= "file:///foo/baz.clj" (:uri (:data error)))))

    (it "returns resource when found"
      (with-redefs [fs/->file (partial fs/->mem-file files)]
        (let [resources-by-uri (sut/->resources-by-uri [{:kind :file :path "/foo/bar.clj"}])
              handler (sut/->read-handler resources-by-uri)
              req  {:jsonrpc "2.0"
                    :id      1
                    :method  "resources/read"
                    :params  {:uri "file:///foo/bar.clj"}}
              resp (handler req)
              file (-> resp :result :contents first)]
          (should= "2.0" (:jsonrpc resp))
          (should= 1 (:id resp))
          (should= "file:///foo/bar.clj" (:uri file)))))
    )
  )