(ns mcp.server.shell-spec
  (:require [c3kit.apron.schema :as schema]
            [clojure.java.shell :as shell]
            [clojure.stacktrace :as st]
            [mcp.server.shell :as sut]
            [speclj.core :refer :all]))


(describe "shell spec"
  (with-stubs)

  (context "shell-handler"
    (redefs-around [shell/sh (stub :shell/sh {:invoke (fn [& args] {:out (last args)})})
                    spit (stub :spit)
                    st/print-stack-trace (stub :print-stack-trace {:invoke (fn [_] (prn "stack-trace"))})])

    (it "calls shell sh with string that has been seperated"
      (let [request {:params {:arguments {:cmd "ls -a"}}}]
        (should= "ls -a" (sut/handler request))
        (should-have-invoked :shell/sh {:with ["sh" "-c" "ls -a"]})))

    (it "returns blank string if result of out is nil"
      (let [request {:params {:arguments {:cmd nil}}}]
        (should= "" (sut/handler request))))

    (it "logs errors, and returns that error"
      (with-redefs [shell/sh (stub :shell/sh {:invoke (fn [_ _ _] (throw (Exception. "message")))})]
        (let [expected-error-message "Error: message"
              expected-spit-text (str expected-error-message "\n\"" "stack-trace" "\"\n\n\n")]
          (should= expected-error-message (sut/handler {:params {:arguments {:cmd "blah"}}}))
          (should-have-invoked :spit {:with ["mcp.log" expected-spit-text :append true]})))
      )
    )

  (context "shell tool"
    (it "name"
      (should= (:name sut/tool) "use-shell"))

    (it "title"
      (should= (:title sut/tool) "Shell Tool"))

    (it "description"
      (should= (:description sut/tool)
               "Interact with the shell for the system running this MCP server, which is on the same system as claude desktop."))

    (it "handler"
      (should= (:handler sut/tool) sut/handler))

    (it "input schema"
      (let [{:keys [type validate]} (get-in sut/tool [:inputSchema :type :cmd])]
        (should= :string type)
        (should= schema/present? validate)))

    )
  )
