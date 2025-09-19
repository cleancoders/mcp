(ns mcp.server.spec-helper
  (:require [speclj.core :refer :all]))

(defmacro should-respond-invalid-req [resp msg]
  `(let [resp#  ~resp
         error# (:error resp#)]
     (should= "2.0" (:jsonrpc resp#))
     (should= -32600 (:code error#))
     (should= "Invalid Request" (:message error#))
     (should= ~msg (:data error#))))

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