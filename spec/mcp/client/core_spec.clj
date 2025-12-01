(ns mcp.client.core-spec
  (:require [mcp.client.core :as sut]
            [mcp.core :as core]
            [speclj.core :refer :all]))

(declare client-info)
(declare client)

(describe "Client"
  (context "build-notification"
    (it "has jsonrpc version on it"
      (should= "2.0" (:jsonrpc (sut/build-notification :initialized)))) 

    (it "makes an initialized notification"
      (let [{:keys [method]} (sut/build-notification :initialized)]
        (should= :notifications/initialized method)))

    (it "makes connected notification"
      (let [{:keys [method]} (sut/build-notification :connected)]
        (should= :notifications/connected method)))

    (it "makes disconnected notification"
      (let [{:keys [method]} (sut/build-notification :disconnected)]
        (should= :notifications/disconnected method)))

    (it "makes restarted notification"
      (let [{:keys [method]} (sut/build-notification :restarted)]
        (should= :notifications/restarted method)))
    )

  (context "build-request"
    (it "has jsonrpc version on it"
      (should= "2.0" (:jsonrpc (sut/build-request 1 :test))))

    (it "has an id"
      (should= 1 (:id (sut/build-request 1 :test)))
      (should= 2 (:id (sut/build-request 2 :test))))

    (it "has a method"
      (should= :test (:method (sut/build-request 2 :test)))
      (should= :initialize (:method (sut/build-request 2 :initialize))))

    (it "can optionally take params as a map"
      (should= {} (:params (sut/build-request 2 :test)))
      (should= {:test 12} (:params (sut/build-request 2 :test {:test 12})))
      (should= {:test 12} (:params (sut/build-request 2 :test :test 12))))
    )

  (with client-info {:name    "ExampleClient"
                     :title   "Example Client Display Name"
                     :version "1.0.0"})

  (with client (sut/->client @client-info))

  (context "->client"
    (it "has protocolVersion"
      (should= core/protocol-version (:protocolVersion (sut/->client @client-info))))

    (it "has capabilities"
      (should= {:roots       {:listChanged true}
                :sampling    {}
                :elicitation {}}
               (:capabilities (sut/->client @client-info))))

    (it "has clientInfo"
      (let [other-client-info {:name    "OtherClient"
                               :title   "Other Client Display Name"
                               :version "1.2.3"}]
        (should= @client-info (:clientInfo (sut/->client @client-info)))
        (should= other-client-info (:clientInfo (sut/->client other-client-info)))))
    )

  (it "->initialize-request"
    (should= (sut/build-request 1 :initialize @client)
             (sut/->initialize-request @client)))

  (it "initialized-notification"
    (should= (sut/build-notification :initialized)
             sut/initialized-notification))
  )
