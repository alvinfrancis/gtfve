(ns gtfve.macros)

(defmacro <? [ch]
  `(let [e (async/<! ~ch)]
     (if (instance? js/Error e)
       (throw e)
       e)))
