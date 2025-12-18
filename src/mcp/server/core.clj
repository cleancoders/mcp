(ns mcp.server.core
  (:require [c3kit.apron.schema :as schema]
            [mcp.core :as core]
            [mcp.server.resource :as resource]
            [mcp.server.errors :as errors]
            [mcp.server.initialize :as init]
            [mcp.server.tool :as tool]
            [medley.core :as medley]))

(defn ->default-handlers [spec state]
  {"initialize"                {:handler (partial init/initialize! spec state)}
   "notifications/initialized" {:handler (partial init/confirm! state)}})

(defn with-resource-handlers [handlers {:keys [resources-by-uri resources-for-list]}]
  (if (seq resources-by-uri)
    (-> handlers
        (assoc "resources/list" {:handler (resource/->list-handler resources-for-list)})
        (assoc "resources/read" {:handler (resource/->read-handler resources-by-uri)}))
    handlers))

(defn with-tool-handlers [handlers {:keys [tools-by-name tools-for-list]}]
  (if (seq tools-by-name)
    (-> handlers
        (assoc "tools/list" {:handler (tool/->list-handler tools-for-list)})
        (assoc "tools/call" {:handler (tool/->call-handler tools-by-name)}))
    handlers))

(defn- index-tools [spec]
  (if-let [tools (seq (:tools spec))]
    (assoc spec
      :tools-by-name (tool/->tools-by-name tools)
      :tools-for-list (tool/->tools-for-list tools))
    spec))

(defn- index-resources [spec]
  (if-let [resources (seq (:resources spec))]
    (assoc spec
      :resources-by-uri (resource/->resources-by-uri resources)
      :resources-for-list (resource/->resources-for-list resources))
    spec))

(defn ->server [spec]
  (let [state   (atom {:seen-ids #{}})
        indexed (-> spec index-tools index-resources)]
    (medley/deep-merge
      indexed
      {:state    state
       :handlers (-> (->default-handlers indexed state)
                     (with-resource-handlers indexed)
                     (with-tool-handlers indexed))})))

(defn =to [x] {:validate #(= x %) :message (format "must be equal to %s" x)})
(defn string-or-long? [x] (or (string? x) (int? x) (char? x)))

(def rpc-request-schema
  {:jsonrpc {:type :string :validations [core/required (=to "2.0")]}
   :method  {:type :string :validations [core/required]}
   :id      {:type #{:long :string} :validate string-or-long?}
   :params  {:type :map}})

(defn maybe-bad-request [req]
  (let [req (schema/validate rpc-request-schema req)
        id  (delay (if (schema/error? (:id req)) nil (:id req)))]
    (when (schema/error? req)
      (errors/invalid-request @id "The JSON sent is not a valid JSON-RPC request object"))))

(defn maybe-uninitialized [server req]
  (when (not (or (init/initializing? req)
                 (init/notifying? req)
                 (init/initialized? server)))
    (errors/uninitialized (:id req))))

(defn maybe-incomplete-init [server req]
  (when (and (init/initializing? req)
             (= :requested (init/initialization server)))
    (errors/invalid-request (:id req) "Already received initialization request")))

(defn maybe-already-initialized [server req]
  (when (and (init/initialized? server) (init/initializing? req))
    (errors/invalid-request (:id req) "Connection already initialized")))

(defn update-seen-ids! [server req]
  (when-let [id (:id req)]
    (swap! (:state server) update :seen-ids conj id)))

(defn -handle [server req]
  (update-seen-ids! server req)
  (if-let [handler (-> server :handlers (get (:method req)) :handler)]
    (handler req)
    (errors/invalid-method (:id req) (:method req))))

(defn maybe-previously-used-id [{:keys [state]} {:keys [id]}]
  (when (-> @state :seen-ids (contains? id))
    (errors/invalid-request id (format "Request ID: %s used previously during this session" id))))

(defn handle [server req]
  (let [conformed (schema/conform rpc-request-schema req)]
    (or (maybe-bad-request req)
        (maybe-uninitialized server conformed)
        (maybe-incomplete-init server conformed)
        (maybe-already-initialized server req)
        (maybe-previously-used-id server req)
        (-handle server req))))