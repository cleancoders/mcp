(ns mcp.server.tool-spec
  (:require [c3kit.apron.doc :as doc]
            [c3kit.apron.schema :as schema]
            [mcp.server.tool :as sut]
            [speclj.core :refer :all]))

(def tool-1
  {:name        "foo"
   :title       "I'm to foo tool, the fool!"
   :description "a foolish tool"
   :handler     (fn [_] (prn "handled!"))                   ; invoke me! witj-out-str
   :inputSchema {}})
(def tool-2
  (assoc tool-1
    :inputSchema {:type {:foo {:type :long :validate schema/present?}}}))

(declare handler)

(describe "Tools"

  (context "->list-handler"

    (it "one tool"
      (let [handler (sut/->list-handler [tool-1])
            req     {:jsonrpc "2.0"
                     :id      1
                     :method  "tools/list"
                     :params  {}}
            resp    (handler req)]
        (should= "2.0" (:jsonrpc resp))
        (should= 1 (:id resp))
        (should= (-> (dissoc tool-1 :handler)
                     (assoc :inputSchema
                            {:type       "object"
                             :properties {}}))
          (-> resp :result :tools first))))

    (it "tools with schemas"
      (let [handler (sut/->list-handler [tool-1 tool-2])
            req     {:jsonrpc "2.0"
                     :id      1
                     :method  "tools/list"
                     :params  {}}
            resp    (handler req)]
        (should= "2.0" (:jsonrpc resp))
        (should= 1 (:id resp))
        (should= (-> (dissoc tool-2 :handler)
                     (update :inputSchema doc/apron->openapi-schema))
          (-> resp :result :tools second))))
    )

  (context "->call-handler"

    (with handler (sut/->call-handler [tool-1]))

    (it "fails when tool not found"
      (let [req {:jsonrpc "2.0"
                 :id      1
                 :method  "tools/call"
                 :params  {:name "baz"}}
            {:keys [error] :as resp} (@handler req)]
        (should= "2.0" (:jsonrpc resp))
        (should= 1 (:id resp))
        (should= -32602 (:code error))
        (should= "Unknown tool: baz" (:message error))
        ))

    (it "calls tool when found"
      (let [req  {:jsonrpc "2.0"
                  :id      1
                  :method  "tools/call"
                  :params  {:name "foo"}}
            resp (with-out-str (@handler req))]
        (should-contain "handled!" resp)))

    (it "returns output of tool"
      (let [tool-handler (fn [req] (str "handled " (:id req)))
            handler      (sut/->call-handler [(assoc tool-1 :handler tool-handler)])
            req          {:jsonrpc "2.0"
                          :id      1
                          :method  "tools/call"
                          :params  {:name "foo"}}
            resp         (-> (handler req) :result :content first)]
        (should= "tool_use" (:type resp))
        (should= "\"handled 1\"" (:text resp))))

    ; todo - enforce tool's given schema on params
    )
  )