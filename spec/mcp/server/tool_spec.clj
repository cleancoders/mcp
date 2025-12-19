(ns mcp.server.tool-spec
  (:require [c3kit.apron.doc :as doc]
            [c3kit.apron.schema :as schema]
            [mcp.server.tool :as sut]
            [speclj.core :refer :all]))

(def base-tool
  {:name        "test-tool"
   :description "A test tool"
   :handler     (fn [_] {:result "default"})
   :inputSchema {}})

(def tool-with-input-schema
  (assoc base-tool
    :name "input-tool"
    :inputSchema {:type {:path {:type :string :validate schema/present?}}}))

(def tool-with-output-schema
  (assoc base-tool
    :name "output-tool"
    :outputSchema {:type {:files {:type [:string]}
                          :count {:type :int}}}))

(defn- list-request [] {:jsonrpc "2.0" :id 1 :method "tools/list" :params {}})
(defn- call-request [tool-name] {:jsonrpc "2.0" :id 1 :method "tools/call" :params {:name tool-name}})

(defn- ->handler [tool-fn]
  (sut/->call-handler (sut/->tools-by-name [(assoc base-tool :handler tool-fn)])))

(describe "Tools"

  (context "listing"

    (it "transforms empty inputSchema to object schema"
      (let [handler (sut/->list-handler (sut/->tools-for-list [base-tool]))
            tool    (-> (handler (list-request)) :result :tools first)]
        (should= {:type "object" :properties {}} (:inputSchema tool))))

    (it "transforms apron inputSchema to openapi schema"
      (let [handler (sut/->list-handler (sut/->tools-for-list [tool-with-input-schema]))
            tool    (-> (handler (list-request)) :result :tools first)]
        (should= (doc/apron->openapi-schema (:inputSchema tool-with-input-schema))
          (:inputSchema tool))))

    (it "transforms apron outputSchema to openapi schema"
      (let [handler (sut/->list-handler (sut/->tools-for-list [tool-with-output-schema]))
            tool    (-> (handler (list-request)) :result :tools first)]
        (should= {:type       "object"
                  :properties {:files {:type "array" :items {:type "string"}}
                               :count {:type "integer"}}}
          (:outputSchema tool))))

    (it "omits outputSchema when not specified"
      (let [handler (sut/->list-handler (sut/->tools-for-list [base-tool]))
            tool    (-> (handler (list-request)) :result :tools first)]
        (should-be-nil (:outputSchema tool))))

    (it "removes handler from listed tools"
      (let [handler (sut/->list-handler (sut/->tools-for-list [base-tool]))
            tool    (-> (handler (list-request)) :result :tools first)]
        (should-be-nil (:handler tool)))))

  (context "calling"

    (it "returns error for unknown tool"
      (let [handler (sut/->call-handler (sut/->tools-by-name [base-tool]))
            {:keys [error]} (handler (call-request "unknown"))]
        (should= -32602 (:code error))
        (should= "Unknown tool: unknown" (:message error))))

    (it "invokes handler"
      (let [invoked? (atom false)
            handler  (->handler (fn [_] (reset! invoked? true) nil))]
        (handler (call-request "test-tool"))
        (should @invoked?)))

    (context "legacy response (plain value)"

      (it "wraps in text content"
        (let [handler (->handler (fn [_] {:data "value"}))
              result  (-> (handler (call-request "test-tool")) :result)]
          (should= "text" (-> result :content first :type))
          (should= "{\"data\":\"value\"}" (-> result :content first :text)))))

    (context "structured response"

      (it "returns structuredContent"
        (let [handler (->handler (fn [_] {:structured {:files ["a.txt"] :count 1}}))
              result  (-> (handler (call-request "test-tool")) :result)]
          (should= {:files ["a.txt"] :count 1} (:structuredContent result))
          (should-be-nil (:content result))))

      (it "returns content array"
        (let [handler (->handler (fn [_] {:content [{:type "text" :text "hello"}]}))
              result  (-> (handler (call-request "test-tool")) :result)]
          (should= [{:type "text" :text "hello"}] (:content result))))

      (it "returns both structured and content"
        (let [handler (->handler (fn [_] {:structured {:count 2}
                                          :content    [{:type "text" :text "Found 2"}]}))
              result  (-> (handler (call-request "test-tool")) :result)]
          (should= {:count 2} (:structuredContent result))
          (should= [{:type "text" :text "Found 2"}] (:content result))))

      (it "sets isError when error? is true"
        (let [handler (->handler (fn [_] {:error? true :content [{:type "text" :text "Failed"}]}))
              result  (-> (handler (call-request "test-tool")) :result)]
          (should= true (:isError result))))

      (it "omits isError on success"
        (let [handler (->handler (fn [_] {:content [{:type "text" :text "OK"}]}))
              result  (-> (handler (call-request "test-tool")) :result)]
          (should-be-nil (:isError result)))))))