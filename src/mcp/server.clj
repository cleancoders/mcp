(ns mcp.server
  (:require [c3kit.apron.utilc :as utilc]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [mcp.core :as core]))

(def server-version "0.1.0")

(defmulti method-result :method)

(defmethod method-result :initialize [_req]
  {:protocolVersion core/protocol-version
   :capabilities    {:resources {:listChanged true}
                     :logging   {}
                     :tools     {:listChanged true}
                     :prompts   {}}
   :serverInfo      {:name    "foo-server"
                     :version server-version}})

(def resources
  [{:uri         "resource://foo"
    :name        "Foo Resource"
    :description "The foo resource"
    :mimeType    "text/plain"
    :text        "This is the content of foo"}
   {:uri         "resource://bar"
    :name        "Bar Resource"
    :description "The bar resource"
    :mimeType    "text/plain"
    :text        "This is the content of bar"}
   {:uri         "resource://baz"
    :name        "Baz Resource"
    :description "The baz resource"
    :mimeType    "text/plain"
    :text        "This is the content of baz"}])

(defmethod method-result :resources/list [_req]
  {:resources (map #(dissoc % :text) resources)})
(defmethod method-result :resources/read [req]
  (let [path (str/replace (:uri (:params req)) #"resource://" "")]
    {:contents (->> resources
                    (filter #(str/includes? (:uri %) path))
                    (map #(dissoc % :name :description)))}))
(defmethod method-result "notifications/initialized" [_req])

(def tools
  [{:name        "foo"
    :description "who's asking?"
    :inputSchema {:type "object"
                  :properties
                  {:foo {:type        "string"
                         :description "bar"}}}}
   {:name        "shell"
    :description "Use a shell command. Be careful or I'll cancel my Claude subscription."
    :inputSchema {:type "object"
                  :properties
                  {:cmd {:type        "string"
                         :description "never use rm"}}}}])

(defmethod method-result "tools/list" [_req]
  {:tools tools})

(defmethod method-result "tools/call" [req]
  (let [tool (-> req :params :name)]
    (if (= "foo" tool)
      {:content [{:type :text :text "baz"}] :isError false}
      (try
        (let [result (apply shell/sh (-> req :params :arguments :cmd (str/split #" ")))]
          {:content [{:type :text :text (:out result)}] :isError false})
        (catch Exception e
          {:content [{:type :text :text (.getMessage e)}] :isError true})))))

(defmethod method-result "prompts/list" [_req]
  {:prompts []})

(defn handle [req]
  {:jsonrpc core/rpc-version
   :id      (:id req)
   :result  (method-result req)})

(defn -main [& args]
  (loop []
    (let [message (read-line)]
      (try
        (let [req (utilc/<-json-kw message)]
          (try
            (spit "out.log" (str "<<< " req "\n") :append true)
            (when-not (str/starts-with? (:method req) "notifications/")
              (let [resp (handle req)]
                (spit "out.log" (str ">>> " resp "\n") :append true)
                (println (utilc/->json resp))))
            (catch Exception e
              (spit "out.log" (str (.getMessage e) "\n") :append true)
              (spit "out.log" (apply str (interpose "\n" (.getStackTrace e))) :append true)
              (System/exit 1))))
        (catch Exception e
          (spit "out.log" (str (.getMessage e) "\n") :append true)
          (spit "out.log" "message: " message :append true))))
    (recur)))