(ns mcp.server.shell-spec
  (:require [c3kit.apron.schema :as schema]
            [clojure.java.shell :as shell]
            [mcp.server.shell :as sut]
            [speclj.core :refer :all]))


(describe "shell spec"
  (with-stubs)

  (context "shell-handler"
    (redefs-around [shell/sh (stub :shell/sh {:invoke (fn [& args] {:out args})})])

    (it "calls shell sh with string that has been seperated"
      (let [request {:params {:arguments {:cmd "ls -a"}}}]
        (should= ["sh" "-c" "ls -a"] (sut/handler request))
        (should-have-invoked :shell/sh {:with ["sh" "-c" "ls -a"]})))
    )

  (context "shell tool"
    (it "name"
      (should= (:name sut/tool) "use-shell"))

    (it "title"
      (should= (:title sut/tool) "Shell Tool"))

    (it "description"
      (should= (:description sut/tool)
               "Interact with the shell for the system running this MCP server."))

    (it "handler"
      (should= (:handler sut/tool) sut/handler))

    (it "input schema"
      (let [{:keys [type validate]} (get-in sut/tool [:inputSchema :type :cmd])]
        (should= :string type)
        (should= schema/present? validate)))

    )
  )
