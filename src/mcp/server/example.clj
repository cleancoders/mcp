(ns mcp.server.example
  (:require [mcp.server.core :as server]
            [mcp.server.resource :as resource]
            [mcp.server.stdio :as stdio]
            [mcp.server.tool :as tool]
            [mcp.server.shell :as shell]))

(def tool
  {:name        "foo"
   :title       "I'm to foo tool, the fool!"
   :description "a foolish tool"
   :handler     (fn [_] "Hello, Claudius!")
   :inputSchema {}})

(defn -main [& _args]
  (let [spec   {:name             "Clean Code MCP"
                :server-version   "0.0.1"
                :protocol-version "2025-06-18"
                :handlers         {"experimental/foo" {:handler (fn [_req] "handled")}}}
        server (-> spec
                   (resource/with-resource {:kind :file :path "/foo/bar.clj"})
                   (tool/with-tool tool)
                   (tool/with-tool shell/tool)
                   server/->server)]
    (loop []
      (stdio/handle-stdio server)
      (recur))))