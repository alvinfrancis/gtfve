(ns gtfve.utils
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [gtfve.async :refer [raise!]]
            [cljs.core.async :as async :refer [put! chan <! close! >!]]
            [goog.events :as gevents])
  (:import [goog.events EventType]))

(defn mutation-listen
  ([el config]
   (mutation-listen el config (chan)))
  ([el config c]
   (let [observer (js/MutationObserver. (fn [mutations]
                                          (doseq [mutation mutations]
                                            (put! c mutation))))]
     (.observe observer el config)
     c)))

(defn listen
  ([el type]
   (listen el type (chan)))
  ([el type c]
   (gevents/listen el type #(put! c %))
   c))

(defn edit-input
  "Meant to be used in a react event handler, usually for the :on-change event on input.
  Path is the vector of keys you would pass to assoc-in to change the value in
  state, event is the Synthetic React event. Pulls the value out of the event.
  Optionally takes :value as a keyword arg to override the event's value"
  [owner key event & {:keys [value]
                      :or {value (.. event -target -value)}}]
  (raise! owner [:edited-input {:key key :value value}]))

(defn uuid
  "returns a type 4 random UUID: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx"
  []
  (let [r (repeatedly 30 (fn [] (.toString (rand-int 16) 16)))]
    (apply str (concat (take 8 r) ["-"]
                       (take 4 (drop 8 r)) ["-4"]
                       (take 3 (drop 12 r)) ["-"]
                       [(.toString  (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16)]
                       (take 3 (drop 15 r)) ["-"]
                       (take 12 (drop 18 r))))))

(defn js->clj-kw
  "Same as js->clj, but keywordizes-keys by default"
  [ds]
  (js->clj ds :keywordize-keys true))
