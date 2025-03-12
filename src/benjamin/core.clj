(ns benjamin.core
  (:require [benjamin.configuration :refer [config]]))

(def ^:dynamic persistence-fn nil)
(def ^:dynamic success-fn nil)
(def ^:dynamic logbook-fn nil)
(def ^:dynamic events nil)
(def ^:dynamic allow-undeclared-events? nil)

(defn validate [entity event]
  (with-bindings {#'allow-undeclared-events? (:allow-undeclared-events? config)
                  #'logbook-fn (:logbook-fn config)
                  #'events (:events config)}
    (when (fn? events) (events))
    (let [not-found (if allow-undeclared-events? (constantly false) (constantly true))
          pred (get events event not-found)]
      (if-let [logbook (seq (logbook-fn entity event))]
        (not (some pred logbook))
        (get events event allow-undeclared-events?)))))

(defmacro with-logbook [entity event & body]
  `(with-bindings {#'persistence-fn (:persistence-fn config)
                   #'success-fn (:success-fn config)}
     (let [logbook# (delay (persistence-fn ~entity ~event))]
       (when (validate ~entity ~event)
         (future (let [result# ~@body]
                   (when (success-fn result#)
                     (force logbook#))
                   result#))))))
