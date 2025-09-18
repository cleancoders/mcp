(ns mcp.client.core-spec
  (:require [mcp.client.core :as sut]
            [speclj.core :refer :all]))

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
      (should= {:test 12} (:params (sut/build-request 2 :test :test 12)))))
  )
