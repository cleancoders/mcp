# clj-mcp

A Clojure SDK for building [Model Context Protocol](https://modelcontextprotocol.io/) servers and clients.

MCP protocol version: `2025-06-18`

## Getting Started

Add the dependency to your `deps.edn`:

```clojure
{:deps {io.github.cleancoders/mcp {:git/url "https://github.com/cleancoders/mcp.git" :git/sha "..."}}}
```

## Server

See the [server example](examples/mcp/server/example.clj) for a runnable implementation.

### Defining a server

Build a server spec and pass it to `mcp.server.core/->server`:

```clojure
(require '[mcp.server.core :as server]
         '[mcp.server.tool :as tool]
         '[mcp.server.resource :as resource]
         '[mcp.server.stdio :as stdio])

(def my-tool
  {:name        "greet"
   :description "Greets the user by name"
   :handler     (fn [req]
                  (let [name (get-in req [:params :arguments :name])]
                    (str "Hello, " name "!")))
   :inputSchema {:type {:name {:type :string}}}})

(def my-server
  (-> {:name             "my-server"
       :server-version   "0.1.0"
       :protocol-version "2025-06-18"}
      (tool/with-tool my-tool)
      (resource/with-resource {:kind :file :path "/some/file.txt"})
      server/->server))
```

### Tools

Register tools with `tool/with-tool`. A tool spec has these keys:

| Key             | Required | Description                                    |
|-----------------|----------|------------------------------------------------|
| `:name`         | yes      | Unique identifier                              |
| `:description`  | yes      | Description shown to the LLM                   |
| `:handler`      | yes      | `(fn [req] ...)` invoked on `tools/call`        |
| `:inputSchema`  | no       | Apron schema for tool arguments                |
| `:outputSchema` | no       | Apron schema for structured response           |
| `:title`        | no       | Human-readable display name                    |
| `:annotations`  | no       | Additional MCP metadata                        |

#### Handler return formats

**Plain value** (legacy) -- returned as JSON text content:

```clojure
(fn [req] {:files ["a.txt" "b.txt"]})
```

**Structured response** -- explicit control over content blocks and errors:

```clojure
(fn [req]
  {:structured {:files ["a.txt"]}
   :content    [{:type "text" :text "Found 1 file"}]
   :error?     true})
```

### Resources

Register file resources with `resource/with-resource`:

```clojure
(resource/with-resource spec {:kind :file :path "/path/to/file.txt"})
```

Resources are served via `resources/list` and `resources/read` endpoints.

### Transports

#### Stdio

Run a stdio loop for use with tools like Claude Desktop:

```clojure
(loop []
  (stdio/handle-stdio my-server)
  (recur))
```

#### HTTP

Handle HTTP requests with CORS origin validation:

```clojure
(require '[mcp.server.http :as http])

;; Uses default config (localhost only)
(http/handle-request ring-request my-server)

;; Custom allowed origins
(http/handle-request ring-request my-server
  {:allowed-origins #{#"^https://example\.com$"}})
```

### Built-in tools

`mcp.server.shell/tool` provides a pre-configured shell execution tool:

```clojure
(require '[mcp.server.shell :as shell])

(tool/with-tool spec shell/tool)
```

### Tracing

Enable request/response tracing by adding a `:trace` key to your server spec:

```clojure
(require '[mcp.server.trace :as trace])

{:name "my-server"
 :trace {:enabled? true
         :sink     (trace/->file-sink "/tmp/mcp-trace.jsonl")}}
```

## Client

See the [client example](examples/mcp/client/example.clj) for a runnable implementation.

### Connecting to a server

```clojure
(require '[mcp.client.core :as client]
         '[mcp.client.stdio :as client-stdio]
         '[clojure.java.io :as io]
         '[clojure.java.process :as process])

(let [proc      (process/start {:in :pipe :out :pipe :err :inherit}
                               "clojure" "-Mserve")
      transport (client-stdio/->IOTransport
                  (io/reader (process/stdout proc))
                  (io/writer (process/stdin proc)))
      config    {:transport   transport
                 :client      (client/->client {:name "my-client" :version "1.0.0"})
                 :next-id-fn  (let [id (atom 0)] #(swap! id inc))}]
  @(client/initialize! config)
  @(client/request! config "tools/list"))
```

## MCP Client Configuration

This library implements the open [Model Context Protocol](https://modelcontextprotocol.io/) and works with any MCP-compatible client.

Stdio-based MCP clients like Claude Desktop need a `command` that starts your server. Point it at whatever starts your server's stdio loop:

```json
{
  "mcpServers": {
    "my-server": {
      "command": "/absolute/path/to/your/server/start-command",
      "args": ["arg1", "arg2"]
    }
  }
}
```

## Development

### Running tests

```bash
clojure -M:test:spec
```