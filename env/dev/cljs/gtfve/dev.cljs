(ns ^:figwheel-no-load gtfve.dev
  (:require [gtfve.core :as core]
            [figwheel.client :as figwheel :include-macros true]
            [weasel.repl :as weasel]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback (fn [] (core/reinstall-om!)))

(weasel/connect "ws://localhost:9001" :verbose true)

(core/setup!)
