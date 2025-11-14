(ns mcp.server.tool-spec
  (:require [c3kit.apron.doc :as doc]
            [c3kit.apron.schema :as schema]
            [mcp.server.tool :as sut]
            [speclj.core :refer :all]))

(def tool-1
  {:name        "foo"
   :title       "I'm to foo tool, the fool!"
   :description "a foolish tool"
   :inputSchema {}})
(def tool-2
  (assoc tool-1
    :inputSchema {:type {:foo {:type :long :validate schema/present?}}}))

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
        (should= (assoc tool-1 :inputSchema
                               {:type       "object"
                                :properties {}})
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
        (should= (update tool-2 :inputSchema doc/apron->openapi-schema)
          (-> resp :result :tools second))))
    )
  )