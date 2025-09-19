(ns mcp.server-spec
  (:require [mcp.server :as sut]
            [mcp.client.core :as client]
            [mcp.core :as core]
            [speclj.core :refer :all]))

; TODO - should init fail if server protocolVersion is out of sync with client?

(describe "MCP server"

  (it "protocol version"
    (should= "2025-06-18" core/protocol-version))

  (it "server version"
    (should= "2.0" core/rpc-version))

  (context "handler"

    (it "initialization"
      (let [params      {:protocolVersion core/protocol-version
                         :capabilities    {}
                         :sampling        {}
                         :elicitation     {}}
            client-info {:name    "FooClient"
                         :title   "Foo Client Title"
                         :version "0.1.0"}
            base-req    (client/build-request 1 :initialize params)
            req         (merge base-req {:clientInfo client-info})
            {:keys [result] :as resp} (sut/handle req)
            {:keys [capabilities serverInfo]} result]
        (should= core/rpc-version (:jsonrpc resp))
        (should= 1 (:id resp))
        (should= core/protocol-version (:protocolVersion result))
        (should= true (:listChanged (:resources capabilities)))
        (should= "foo-server" (:name serverInfo))
        (should= "0.1.0" (:version serverInfo))))

    (context "resources"

      (it "list"
        (let [req       (client/build-request 2 :resources/list)
              {:keys [result] :as resp} (sut/handle req)
              resources (:resources result)]
          (should= core/rpc-version (:jsonrpc resp))
          (should= 2 (:id resp))
          (should-contain
           {:uri         "resource://foo"
            :name        "Foo Resource"
            :description "The foo resource"
            :mimeType    "text/plain"}
           resources)
          (should-contain
           {:uri         "resource://bar"
            :name        "Bar Resource"
            :description "The bar resource"
            :mimeType    "text/plain"}
           resources)
          (should-contain
           {:uri         "resource://baz"
            :name        "Baz Resource"
            :description "The baz resource"
            :mimeType    "text/plain"}
           resources)))

      (it "reads foo"
        (let [req (client/build-request 3 :resources/read {:uri "resource://foo"})
              {:keys [result] :as resp} (sut/handle req)]
          (should= core/rpc-version (:jsonrpc resp))
          (should= 3 (:id resp))
          (should-contain
           {:uri      "resource://foo",
            :mimeType "text/plain",
            :text     "This is the content of foo"}
           (:contents result))
          (should= 1 (count (:contents result)))))

      (it "reads bar"
        (let [req (client/build-request 3 :resources/read {:uri "resource://bar"})
              {:keys [result] :as resp} (sut/handle req)]
          (should= core/rpc-version (:jsonrpc resp))
          (should= 3 (:id resp))
          (should-contain
           {:uri      "resource://bar",
            :mimeType "text/plain",
            :text     "This is the content of bar"}
           (:contents result))
          (should= 1 (count (:contents result)))))
      )
    )
  )