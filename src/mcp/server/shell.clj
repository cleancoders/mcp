(ns mcp.server.shell
  (:require [c3kit.apron.schema :as schema]
            [clojure.java.shell :as shell]
            [clojure.stacktrace :as st]))

(defn handler [req]
  (try
    (let [cmd (get-in req [:params :arguments :cmd])
          result (shell/sh "sh" "-c" cmd)]
      (or (:out result) ""))
    (catch Exception e
      (spit "mcp.log" (str "ERROR: " (.getMessage e) "\n" (with-out-str (st/print-stack-trace e)) "\n\n") :append true)
      (str "Error: " (.getMessage e)))))

(def tool
  {:name "use-shell"
   :title "Shell Tool"
   :description "Interact with the shell for the system running this MCP server."
   :handler handler
   :inputSchema {:type {:cmd {:type :string :validate schema/present?}}}})