(ns benjamin.core)

(def ^:dynamic persistence-fn nil)
(def ^:dynamic success-fn nil)
(def ^:dynamic logbook-fn nil)
(def ^:dynamic events nil)
(def ^:dynamic allow-undeclared-events? nil)

(def ^:dynamic config
  {:persistence-fn (fn [_ _] (throw (Exception.  "Please run 'set-config! :persistence-fn!` with a function of two arguments (entity and event)")))
   :success-fn (constantly true)
   :events #(throw (Exception. "Please set event and predicate map"))
   :logbook-fn :logbook
   :allow-undeclared-events? true})

(defn set-config! [k v]
  {:pre [(some #{k} [:persistence-fn :success-fn :events :logbook-fn :allow-undeclared-events?])]}
  (alter-var-root #'config (fn [config] (assoc config k v))))

(defn validate [entity event]
  (with-bindings {#'allow-undeclared-events? (:allow-undeclared-events? config)
                  #'logbook-fn (:logbook-fn config)
                  #'events (:events config)}
    (when (fn? events) (events))
    (let [not-found (if allow-undeclared-events? (constantly false) (constantly true))
          pred (event events not-found)]
      (if (logbook-fn entity)
        (not (some pred (filter event (logbook-fn entity))))
        true))))

(defmacro with-logbook [entity event & body]
  `(with-bindings {#'persistence-fn (:persistence-fn config)
                   #'success-fn (:success-fn config)}
     (let [logbook# (delay (persistence-fn ~entity ~event))]
       (when (validate ~entity ~event)
         (future (let [result# ~@body]
                   (when (success-fn result#)
                     (force logbook#))
                   result#))))))
