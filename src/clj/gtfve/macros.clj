(ns gtfve.macros)

(defn- inspect-1 [expr]
  `(let [result# ~expr]
     (js/console.info (str (pr-str '~expr) " => " (pr-str result#)))
     result#))

(defmacro inspect [& exprs]
  `(do ~@(map inspect-1 exprs)))

(defmacro <? [ch]
  `(let [e (async/<! ~ch)]
     (if (instance? js/Error e)
       (throw e)
       e)))
