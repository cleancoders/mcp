(ns mcp.server.spec-helper
  (:require [mcp.server.core :as server]
            [speclj.core :refer :all]))

(defmacro should-respond-invalid-req
  ([resp msg]
   `(should-respond-invalid-req ~resp ~msg nil))
  ([resp msg id]
   `(let [resp# ~resp
          error# (:error resp#)]
      (should= "2.0" (:jsonrpc resp#))
      (should= -32600 (:code error#))
      (should= "Invalid Request" (:message error#))
      (should= ~msg (:data error#))
      (should= ~id (:id resp#)))))

(defmacro should-respond-unknown-method
  ([resp msg id]
   `(let [resp# ~resp
          error# (:error resp#)]
      (should= "2.0" (:jsonrpc resp#))
      (should= -32601 (:code error#))
      (should= "Method not found" (:message error#))
      (should= ~msg (:data error#))
      (should= ~id (:id resp#)))))

(defmacro should-respond-invalid-params
  ([resp msg]
   `(should-respond-invalid-req ~resp ~msg nil))
  ([resp msg id]
   `(let [resp# ~resp
          error# (:error resp#)]
      (should= "2.0" (:jsonrpc resp#))
      (should= -32602 (:code error#))
      (should= "Invalid Parameters" (:message error#))
      (should= ~msg (:data error#))
      (should= ~id (:id resp#)))))

(defn ->req [spec]
  (merge spec
         {:jsonrpc "2.0"}))
(def init-req
  (->req {:method "initialize"
          :id     1
          :params {:protocolVersion "2025-06-18"
                   :capabilities    {}
                   :clientInfo      {:name    "Test Client"
                                     :version "1.0.0"}}}))
(def foo-req
  (->req {:method "experimental/foo" :id 2 :params {}}))

(defn initialize! [server]
  (server/handle server init-req)
  (server/handle server {:jsonrpc "2.0" :method "notifications/initialized"}))