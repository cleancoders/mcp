(ns mcp.fs
  (:require [c3kit.apron.time :as time])
  (:import (java.nio.file Files LinkOption NoSuchFileException Paths)))

(defprotocol File
  (mime-type [_this])
  (content [_this])
  (last-modified [_this])
  (name [_this]))

(deftype MemFile [spec path]
  File
  (mime-type [_this]
    (:mime-type (spec path)))
  (content [_this]
    (:content (spec path)))
  (last-modified [_this]
    (str (time/from-epoch (:last-modified (spec path)))))
  (name [_this]
    (:name (spec path))))

(deftype FileResource [path]
  File
  (mime-type [_this]
    (let [path (Paths/get path (into-array String []))]
      (or (Files/probeContentType path)
          "application/octet-stream")))
  (content [_this]
    (Files/readAllBytes (Paths/get path (into-array String []))))
  (name [_this]
    (-> (Paths/get path (into-array String []))
        .getFileName
        .toString))
  (last-modified [_this]
    (-> (Files/getLastModifiedTime
          (Paths/get path (into-array String []))
          (into-array LinkOption []))
        .toInstant
        str)))

(defn ->mem-file [spec path]
  (if (contains? spec path)
    (->MemFile spec path)
    (throw (NoSuchFileException. path))))

(defn ->file [path]
  (->FileResource path))