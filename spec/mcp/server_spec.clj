(ns mcp.server-spec
  (:require [mcp.server :as sut]
            [speclj.core :refer :all]))

; TODO - should init fail if server protocolVersion is out of sync with client?

(describe "MCP server"

  (it "protocol version"
    (should= "2024-11-05" sut/protocol-version))

  (it "server version"
    (should= "2.0" sut/rpc-version))

  (context "handler"

    (it "initialization"
      (let [req {:jsonrpc    sut/rpc-version
                 :id         1
                 :method     "initialize"
                 :params     {:protocolVersion sut/protocol-version
                              :capabilities    {}
                              :sampling        {}
                              :elicitation     {}}
                 :clientInfo {:name    "FooClient"
                              :title   "Foo Client Title"
                              :version "0.1.0"}}
            {:keys [result] :as resp} (sut/handle req)
            {:keys [capabilities serverInfo]} result]
        (should= sut/rpc-version (:jsonrpc resp))
        (should= 1 (:id resp))
        (should= sut/protocol-version (:protocolVersion result))
        (should= true (:listChanged (:resources capabilities)))
        (should= "foo-server" (:name serverInfo))
        (should= "0.1.0" (:version serverInfo))))

    (context "resources"

      (it "list"
        (let [req       {:jsonrpc sut/rpc-version
                         :id      2
                         :method  "resources/list"
                         :params  {}}
              {:keys [result] :as resp} (sut/handle req)
              resources (:resources result)]
          (should= sut/rpc-version (:jsonrpc resp))
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
        (let [req {:jsonrpc sut/rpc-version
                   :id      3
                   :method  "resources/read"
                   :params  {:uri "resource://foo"}}
              {:keys [result] :as resp} (sut/handle req)]
          (should= sut/rpc-version (:jsonrpc resp))
          (should= 3 (:id resp))
          (should-contain
            {:uri      "resource://foo",
             :mimeType "text/plain",
             :text     "This is the content of foo"}
            (:contents result))
          (should= 1 (count (:contents result)))))

      (it "reads bar"
        (let [req {:jsonrpc sut/rpc-version
                   :id      3
                   :method  "resources/read"
                   :params  {:uri "resource://bar"}}
              {:keys [result] :as resp} (sut/handle req)]
          (should= sut/rpc-version (:jsonrpc resp))
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