(ns mcp.server.shell
  (:require [c3kit.apron.schema :as schema]
            [clojure.java.shell :as shell]
            [clojure.stacktrace :as st]))

(defn log-and-return [e]
  (let [error-message (str "Error: " (.getMessage e))
        stack-trace (with-out-str (st/print-stack-trace e))]
    (spit "mcp.log" (str error-message "\n" stack-trace "\n\n") :append true)
    error-message))

(defn handler [req]
  (try
    (let [cmd (get-in req [:params :arguments :cmd])
          result (shell/sh "sh" "-c" cmd)]
      (or (:out result) ""))
    (catch Exception e (log-and-return e))))

(def tool
  {:name "use-shell"
   :title "Shell Tool"
   :description "Interact with the shell for the system running this MCP server, which is on the same system as claude desktop."
   :handler handler
   :inputSchema {:type {:cmd {:type :string :validate schema/present?}}}})