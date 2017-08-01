(ns benjamin.configuration)

(def defaults {:persistence-fn (fn [_ _]
                                 (throw (Exception.  "Please run 'set-config! :persistence-fn!` with a function of two arguments (entity and event)")))
               :success-fn (constantly true)
               :events #(throw (Exception. "Please set event and predicate map"))
               :logbook-fn :logbook
               :allow-undeclared-events? true})

(def ^:dynamic config defaults)

(defn set-config! [k v]
  {:pre [(some #{k} [:persistence-fn :success-fn :events :logbook-fn :allow-undeclared-events?])]}
  (alter-var-root #'config (fn [config] (assoc config k v))))

(def reset! #(alter-var-root #'config (constantly defaults)))
