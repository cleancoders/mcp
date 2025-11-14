(ns mcp.server
  (:require [mcp.server.core :as server]
            [mcp.server.resource :as resource]
            [mcp.server.stdio :as stdio]))

(def tool
  {:name        "foo"
   :title       "I'm to foo tool, the fool!"
   :description "a foolish tool"
   :handler     (fn [_] (prn "handled!"))                   ; invoke me! witj-out-str
   :inputSchema {}})

(defn -main [& args]
  (let [spec   {:name             "Test Server"
                :server-version   "1.0.0"
                :protocol-version "2025-06-18"
                :capabilities     {"experimental/foo" {:handler (fn [req] :handled)}}}
        server (-> spec
                   (resource/with-resource {:kind :file :path "/foo/bar.clj"})
                   server/->server)]
    (loop []
      (stdio/handle-stdio server)
      (recur))))