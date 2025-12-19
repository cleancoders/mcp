(ns mcp.server.trace-spec
  (:require [c3kit.apron.time :as time]
            [mcp.server.trace :as sut]
            [speclj.core :refer :all]))

(describe "Trace"

  (context "->event"

    (it "builds a trace event with all fields"
      (let [correlation-id "abc-123"
            data           {:method "test"}
            event          (sut/->event :request correlation-id data)]
        (should= :request (:type event))
        (should= "abc-123" (:correlation-id event))
        (should= {:method "test"} (:data event))
        (should-not-be-nil (:timestamp event))))

    (it "uses current time for timestamp"
      (let [now (time/now)]
        (with-redefs [time/now (constantly now)]
          (let [event (sut/->event :response "id" {})]
            (should= now (:timestamp event))))))
    )

  (context "->file-sink"

    (with tmp-file (str "/tmp/mcp-trace-test-" (System/currentTimeMillis) ".log"))

    (after (clojure.java.io/delete-file @tmp-file true))

    (it "returns a function"
      (should (fn? (sut/->file-sink @tmp-file))))

    (it "writes event as JSON line to file"
      (let [sink  (sut/->file-sink @tmp-file)
            event {:type :request :correlation-id "abc" :data {:foo "bar"}}]
        (sink event)
        (let [content (slurp @tmp-file)]
          (should-contain "\"type\":\"request\"" content)
          (should-contain "\"correlation-id\":\"abc\"" content)
          (should-contain "\"foo\":\"bar\"" content)
          (should-contain "\n" content))))

    (it "appends multiple events"
      (let [sink (sut/->file-sink @tmp-file)]
        (sink {:type :request :correlation-id "1" :data {}})
        (sink {:type :response :correlation-id "1" :data {}})
        (let [lines (clojure.string/split-lines (slurp @tmp-file))]
          (should= 2 (count lines)))))

    (it "creates parent directory if it does not exist"
      (let [nested-path (str "/tmp/mcp-trace-test-" (System/currentTimeMillis) "/nested/trace.log")
            sink        (sut/->file-sink nested-path)]
        (sink {:type :request :correlation-id "1" :data {}})
        (should (clojure.java.io/as-file nested-path))
        (clojure.java.io/delete-file nested-path true)
        (clojure.java.io/delete-file (.getParent (clojure.java.io/file nested-path)) true)
        (clojure.java.io/delete-file (.getParent (.getParentFile (clojure.java.io/file nested-path))) true)))
    )

  (context "noop-sink"

    (it "does nothing and returns nil"
      (should-be-nil (sut/noop-sink {:type :request})))
    )

  (context "trace!"

    (with-stubs)
    (with sink (stub :sink))

    (it "calls sink with event when enabled"
      (let [config {:trace {:enabled? true :sink @sink}}]
        (sut/trace! config :request "abc" {:method "test"})
        (should-have-invoked :sink {:times 1})))

    (it "does not call sink when disabled"
      (let [config {:trace {:enabled? false :sink @sink}}]
        (sut/trace! config :request "abc" {:method "test"})
        (should-not-have-invoked :sink)))

    (it "does not call sink when trace config missing"
      (let [config {}]
        (sut/trace! config :request "abc" {:method "test"})
        (should-not-have-invoked :sink)))

    (it "passes well-formed event to sink"
      (let [captured (atom nil)
            config   {:trace {:enabled? true :sink #(reset! captured %)}}]
        (sut/trace! config :request "abc" {:method "test"})
        (should= :request (:type @captured))
        (should= "abc" (:correlation-id @captured))
        (should= {:method "test"} (:data @captured))))
    )
  )