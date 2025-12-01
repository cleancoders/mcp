(ns mcp.fs-spec
  (:require [c3kit.apron.time :as time]
            [mcp.fs :as sut]
            [speclj.core :refer :all])
  (:import (java.nio.file NoSuchFileException)))

(declare handler)
(declare spec)

(describe "files"

  (redefs-around [sut/->file (partial sut/->mem-file
                                      {"/foo/bar.clj" {:content       (.getBytes "baz")
                                                       :mime-type     "text/html"
                                                       :name          "bar.clj"
                                                       :last-modified (str (time/now))}})])

  (context "mem"

    (it "file doesn't exist"
      (should-throw NoSuchFileException (sut/->mem-file {} "/foo/bar")))

    (it "mime type"
      (let [file (sut/->mem-file {"/foo/bar" {:mime-type "text/html"}} "/foo/bar")]
        (should= "text/html" (sut/mime-type file))))

    (it "content"
      (let [file (sut/->mem-file {"/foo/bar" {:content (.getBytes "the content")}} "/foo/bar")]
        (should= "the content" (String. ^bytes (sut/content file)))))

    (it "name"
      (let [file (sut/->mem-file {"/foo/bar" {:name "The Name"}} "/foo/bar")]
        (should= "The Name" (sut/name file))))

    (it "last modified"
      (let [file (sut/->mem-file {"/foo/bar" {:last-modified (time/millis-since-epoch (time/now))}} "/foo/bar")]
        (should= (str (time/now)) (sut/last-modified file))))
    )

  ; shows capabilities in initialization
  ; defines resource title
  ; defines resource description
  ; watches file changes (& notifies)
  ; prefers user-defined handler to one defined here
  ; handles file not found
  )