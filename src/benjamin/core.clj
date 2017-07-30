(ns benjamin.core)

(def ^:dynamic persistence-fn (fn [_ _] (throw (Exception.  "Please 'set-persistence-fn!` with a function of two arguments (entity and event)"))))  
(def ^:dynamic success-fn (fn [_] (throw (Exception. "Please 'set-success-fn!` with a function of one argument (result)"))))   
(def ^:dynamic get-logbook :logbook)
(def ^:dynamic events #(throw (Exception. "Please set event and predicate map")))

(defn set-persistence-fn! [f]
  (alter-var-root #'persistence-fn (constantly f)))
(defn set-success-fn! [f]
  (alter-var-root #'success-fn (constantly f)))
(defn set-events! [xs]
  (alter-var-root #'events (constantly xs)))

(defn validate [entity event]
  (when (fn? events) (events))
  (let [pred (event events (constantly false))]
    (if (get-logbook entity)
      (not (some pred (filter event (get-logbook entity))))
      true)))

(defmacro with-logbook [entity event & body]
  `(let [logbook# (delay (persistence-fn ~entity ~event))]
     (when (validate ~entity ~event)
       (future (let [result# ~@body]
                 (when (success-fn result#)
                   (force logbook#))
                 result#)))))
