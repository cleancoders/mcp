#!/bin/bash
exec 2>/dev/null
cd "/Users/sajensen/code/cc/mcp"
exec clojure -M:serve
