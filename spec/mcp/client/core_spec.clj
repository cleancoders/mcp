(ns mcp.client.core-spec
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.utilc :as utilc]
            [mcp.client.core :as sut]
            [mcp.core :as core]
            [speclj.core :refer :all]))

(declare client-info)
(declare client)
(declare request)
(declare response)

(defn- initial-request [client]
  (sut/build-request 1 "initialize" client))

(deftype MockTransport [sent-atom read-atom]
  sut/Transport
  (send! [_ jrpc-payload]
    (swap! sent-atom conj jrpc-payload))
  (read! [_]
    ((stub :read!))
    (let [[first & rest] @read-atom]
      (reset! read-atom rest)
      first)))

(declare transport)
(def sent-atom (atom []))
(def read-atom (atom []))

(defn- reset-transport! []
  (reset! sent-atom [])
  (reset! read-atom []))

(def mock-transport (->MockTransport sent-atom read-atom))
(defn- get-sent [] @sent-atom)
(defn- set-read! [responses] (reset! read-atom responses))

(declare config)

(def current-id (atom 0))
(defn- next-id! []
  (swap! current-id inc))
(defn- reset-id! [] (reset! current-id 0))

(describe "Client"
  (with-stubs)
  (with client-info {:name    "ExampleClient"
                     :title   "Example Client Display Name"
                     :version "1.0.0"})

  (with client (sut/->client @client-info))

  (context "build-notification"
    (it "has jsonrpc version on it"
      (should= "2.0" (:jsonrpc (sut/build-notification :initialized)))) 

    (it "makes an initialized notification"
      (let [{:keys [method]} (sut/build-notification :initialized)]
        (should= "notifications/initialized" method)))

    (it "makes connected notification"
      (let [{:keys [method]} (sut/build-notification :connected)]
        (should= "notifications/connected" method)))

    (it "makes disconnected notification"
      (let [{:keys [method]} (sut/build-notification :disconnected)]
        (should= "notifications/disconnected" method)))

    (it "makes restarted notification"
      (let [{:keys [method]} (sut/build-notification :restarted)]
        (should= "notifications/restarted" method)))
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

  (it "initialized-notification"
    (should= (sut/build-notification "initialized")
             sut/initialized-notification))

  (context "config-dependent fns"
    (before (reset-transport!)
            (reset-id!))

    (with transport mock-transport)
    (with config {:client @client
                  :transport @transport
                  :next-id-fn next-id!})

    (with response (utilc/->json {:jsonrpc "2.0" :id 1}))
    (with request (utilc/->json (sut/->initialize-request @config)))

    (it "->initialize-request"
      (should= (sut/build-request 1 "initialize" @client)
               (sut/->initialize-request @config)))

    (context "raw-json-request!"

      (before (set-read! [@response]))

      (it "sends jrpc-payload through transport"
        (sut/raw-json-request! @transport @request)
        (should= [@request] (get-sent)))

      (it "returns delayed response"
        (let [delay (sut/raw-json-request! @transport @request)]
          (should-not-have-invoked :read!)
          (should= @response @delay)))

      (it "ensures returned response is correct id"
        (let [resp-1 @response
              resp-2 (utilc/->json {:jsonrpc "2.0" :id 2})
              req-1 (utilc/->json (sut/->initialize-request @config))
              d1 (sut/raw-json-request! @transport req-1)
              req-2 (utilc/->json (sut/build-request 2 "tools/list"))
              d2 (sut/raw-json-request! @transport req-2)]
          (set-read! [resp-2 resp-1])
          (should= resp-1 @d1)
          (should= resp-2 @d2)))
      )

    (context "raw-request!"

      (before (set-read! [@response]))

      (it "sends jrpc-payload through transport"
        (sut/raw-request! @transport (utilc/<-json-kw @request))
        (should= [@request] (get-sent)))

      (it "returns delayed response"
        (let [delay (sut/raw-request! @transport (utilc/<-json-kw @request))]
          (should-not-have-invoked :read!)
          (should= (utilc/<-json-kw @response) @delay)))

      (it "ensures returned response is correct id"
        (let [resp-1 @response
              resp-2 (utilc/->json {:jsonrpc "2.0" :id 2})
              req-1 (sut/->initialize-request @config)
              d1 (sut/raw-request! @transport req-1)
              req-2 (sut/build-request 2 "tools/list")
              d2 (sut/raw-request! @transport req-2)]
          (set-read! [resp-2 resp-1])
          (should= (utilc/<-json-kw resp-1) @d1)
          (should= (utilc/<-json-kw resp-2) @d2)))
      )

    (context "request-initialize!"
      (it "sends initialization request"
        (sut/request-initialize! @config)
        (should= [(utilc/->json (initial-request @client))] (get-sent)))

      (it "returns response"
        (set-read! [@response])
        (should= (utilc/<-json-kw @response)
                 @(sut/request-initialize! @config)))
      )

    (context "notify-initialized!"
      (it "sends init notification through transport"
        (sut/notify-initialized! @transport)
        (should= [(utilc/->json sut/initialized-notification)] (get-sent)))

      (it "doesn't expect response"
        (should-be-nil (sut/notify-initialized! @transport)))
      )

    (context "initialize!"
      (it "sends initialization request and notification"
        (sut/initialize! @config)
        (should= [(utilc/->json (initial-request @client))
                  (utilc/->json sut/initialized-notification)]
                 (get-sent)))

      (it "returns response from initialization request"
        (set-read! [@response])
        (should= (utilc/<-json-kw @response)
                 @(sut/initialize! @config)))
      )

    (context "request!"
      (it "sends method request"
        (sut/request! @config "tools/list")
        (should= [(utilc/->json (sut/build-request 1 "tools/list"))]
                 (get-sent)))

      (it "keeps track of the request id"
        (sut/initialize! @config)
        (sut/request! @config "tools/list")
        (should= [(utilc/->json (initial-request @client))
                  (utilc/->json sut/initialized-notification)
                  (utilc/->json (sut/build-request 2 "tools/list"))]
                 (get-sent)))

      (it "optionally accepts parameters"
        (sut/request! @config "tools/call" {:name "get_weather"})
        (should= [(utilc/->json (sut/build-request 1 "tools/call" {:name "get_weather"}))]
                 (get-sent)))

      (it "returns response from initialization request"
        (set-read! [@response])
        (should= (utilc/<-json-kw @response)
                 @(sut/request! @config "tools/list")))
      )
    )

  )
