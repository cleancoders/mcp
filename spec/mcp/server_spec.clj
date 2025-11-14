(ns mcp.server-spec
  (:require [mcp.server :as sut]
            [mcp.client.core :as client]
            [mcp.core :as core]
            [speclj.core :refer :all]))

; TODO - should init fail if server protocolVersion is out of sync with client?
