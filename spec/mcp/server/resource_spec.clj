(ns mcp.server.resource-spec
  (:require [c3kit.apron.time :as time]
            [mcp.server.core :as server]
            [mcp.server.resource :as sut]
            [mcp.server.spec-helper :as server-helper]
            [speclj.core :refer :all])
  (:import (java.nio.file NoSuchFileException)))

(declare handler)
(declare spec)

(describe "resources"

  (with-stubs)
  (with spec {:name             "Test Server"
              :server-version   "1.0.0"
              :protocol-version "2025-06-18"
              :capabilities     {}})
  (redefs-around [time/now (constantly (time/now))])

  (it "undefined by default"
    (let [server (server/->server @spec)
          _      (server-helper/initialize! server)
          resp   (server/handle server (server-helper/->req {:method "resources/list" :id 2}))]
      (server-helper/should-respond-unknown-method resp "Method 'resources/list' is not supported" 2)))

  (context "files"

    (redefs-around [sut/->file (partial sut/->mem-file
                                        {"/foo/bar.clj" {:content       (.getBytes "baz")
                                                         :mime-type     "text/html"
                                                         :name          "bar.clj"
                                                         :last-modified (str (time/now))}})])

    (context "mem"

      (it "file doesn't exist"
        (should-throw NoSuchFileException (sut/->mem-file {} "/foo/bar")))

      (it "mime type"
        (let [file (sut/->mem-file {"/foo/bar" {:mime-type "text/html"}} "/foo/bar")]
          (should= "text/html" (sut/mime-type file))))

      (it "content"
        (let [file (sut/->mem-file {"/foo/bar" {:content (.getBytes "the content")}} "/foo/bar")]
          (should= "the content" (String. ^bytes (sut/content file)))))

      (it "name"
        (let [file (sut/->mem-file {"/foo/bar" {:name "The Name"}} "/foo/bar")]
          (should= "The Name" (sut/name file))))

      (it "last modified"
        (let [file (sut/->mem-file {"/foo/bar" {:last-modified (time/millis-since-epoch (time/now))}} "/foo/bar")]
          (should= (str (time/now)) (sut/last-modified file))))
      )

    ; shows capabilities in initialization
    ; defines resource title
    ; defines resource description
    ; watches file changes (& notifies)
    ; prefers user-defined handler to one defined here
    ; handles file not found

    (it "defines list"
      (let [server    (-> @spec
                          (sut/with-resource {:kind :file :path "/foo/bar.clj"})
                          server/->server)
            _         (server-helper/initialize! server)
            {:keys [result] :as resp} (server/handle server (server-helper/->req {:method "resources/list" :id 2}))
            resources (:resources result)]
        (should= "2.0" (:jsonrpc resp))
        (should= 2 (:id resp))
        (should= [{:uri      "file:///foo/bar.clj"
                   :name     "bar.clj"
                   :mimeType "text/html"}]
          resources)))
    )

  (context "->read-handler"

    (with handler (sut/->read-handler [{:kind :file :path "/foo/bar.clj"}]))

    (it "fails when resource not found"
      (let [req {:jsonrpc "2.0"
                 :id      1
                 :method  "resources/read"
                 :params  {:uri "file:///foo/baz.clj"}}
            {:keys [error] :as resp} (@handler req)]
        (should= "2.0" (:jsonrpc resp))
        (should= 1 (:id resp))
        (should= -32002 (:code error))
        (should= "Resource not found" (:message error))
        (should= "file:///foo/baz.clj" (:uri (:data error)))))

    (it "returns resource when found"
      (with-redefs [sut/->file (partial sut/->mem-file
                                        {"/foo/bar.clj" {:content       (.getBytes "baz")
                                                         :mime-type     "text/html"
                                                         :name          "bar.clj"
                                                         :last-modified (str (time/now))}})]
        (let [req  {:jsonrpc "2.0"
                    :id      1
                    :method  "resources/read"
                    :params  {:uri "file:///foo/bar.clj"}}
              resp (@handler req)
              file (-> resp :result :contents first)]
          (should= "2.0" (:jsonrpc resp))
          (should= 1 (:id resp))
          (should= "file:///foo/bar.clj" (:uri file))
          )))
    )
  )