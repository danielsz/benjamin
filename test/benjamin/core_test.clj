(ns benjamin.core-test
  (:require [benjamin.core :refer [with-logbook]]
            [benjamin.configuration :as config :refer [set-config!]]
            [detijd.predicates :refer [today? last-days? last-months?]]
            [clojure.test :refer [testing deftest is use-fixtures]])
  (:import [java.time Instant]))

(def clean-slate {:name "Benjamin Peirce"})
(def user (atom clean-slate))

(defn persistence-fn [entity event]
  (if-let [entry-index (first (keep-indexed (fn [idx entry] (when (= (:event entry) event) idx)) (:logbooks @entity)))]                       
    (swap! entity update-in [:logbooks entry-index :timestamps] conj (Instant/now))
    (swap! entity update :logbooks (fnil #(conj % {:event event :timestamps [(Instant/now)]}) []))))

(defn logbook-fn [entity event]
  (if-let [logbook (first (filter #(= event (:event %)) (:logbooks @entity)))]
    (:timestamps logbook)
    []))

(def unique? #(some? %))

(def last-3-days? #(last-days? % 3))

(def last-3-months? #(last-months? % 3))

(defn my-fixture [f]
  (reset! user clean-slate)
  (set-config! :events {:account-blocked last-3-months?
                        :end-of-trial unique?
                        :follow-up unique?
                        :categories-change today?
                        :newsletter last-3-days?
                        :always (constantly false)
                        :never (constantly true)})
  (set-config! :persistence-fn persistence-fn)
  (set-config! :logbook-fn logbook-fn)
  (set-config! :success-fn (constantly true))
  (f))

(use-fixtures :each my-fixture)

(deftest configuration
  (testing "Doesn't throw error when everything is set"
      (is (future? (with-logbook user :follow-up
                     (do)))))
  (testing "Sanity check"
    (is (= (:name @user) "Benjamin Peirce"))
    (is (seq (:logbooks @user)))
    (config/reset!)
    (testing "Throws error when `events' is not set"
      (is (thrown-with-msg? java.lang.Exception #"Please set event and predicate map"
                            (with-logbook @user :subscribed
                              (do)))))
    (set-config! :events {:account-blocked last-3-months?
                          :end-of-trial unique?
                          :follow-up unique?
                          :categories-change today?
                          :newsletter last-3-days?})
    (testing "Throws error when `logbook-fn' is not set"
      (is (thrown-with-msg? java.lang.Exception #"Please run 'set-config! :logbook-fn!` with a function of two arguments, entity and event"
                            @(with-logbook user :follow-up
                               (do)))))))

(deftest persistence
  (testing "Persistence doesn't occur when success is denied"
    (set-config! :success-fn (constantly false))
    @(with-logbook user :account-blocked
       (do))
    (is (empty? (:logbooks @user))))
  (testing "Persistence occurs when success is confirmed"
    (reset! user clean-slate)
    (set-config! :success-fn (constantly true))
    @(with-logbook user :account-blocked
       (do))
    (is (contains? @user :logbooks))
    (is (boolean (some #(= (:event %) :account-blocked) (:logbooks @user))))))

(deftest predicates
  (testing "Predicates determine if operation is done, and if logbook gets written."
    (testing "`Unique` predicate means operation can be executed once only."
      (is (= "I have done something" @(with-logbook user :end-of-trial
                                        (identity "I have done something"))))
      (is (= nil (with-logbook user :end-of-trial
                   (identity "I have done something"))))
      (is (= 1 (count (:timestamps (first (filter #(= (:event %) :end-of-trial) (:logbooks @user))))))))
    (testing "`Today` predicate means operation can be executed only if no other operation has been executed during the present day."
      (is (= "I have done something" @(with-logbook user :categories-change
                                        (identity "I have done something"))))
      (is (= nil (with-logbook user :categories-change
                   (identity "I have done something"))))
      (is (= 1 (count (:timestamps (first (filter #(= (:event %) :categories-change) (:logbooks @user)))))))
      (is (= nil (with-logbook user :categories-change
                   (identity "I have done something"))))
      (is (= 1 (count (:timestamps (first (filter #(= (:event %) :categories-change) (:logbooks @user)))))))
      (is (= "I have done something" @(with-logbook user :always
                                        (identity "I have done something"))))
      (is (= "I have done something" @(with-logbook user :always
                                        (identity "I have done something"))))
      (is (= "I have done something" @(with-logbook user :always
                                        (identity "I have done something"))))
      (is (= 3 (count (:timestamps (first (filter #(= (:event %) :always) (:logbooks @user))))))))))

(deftest events
  (testing "If the event is unknown, we don't execute the operation and don't write to the logbook (default)."
    (testing "default setting"
      (set-config! :allow-undeclared-events? false)
      (is (= nil (with-logbook user :subscribed
                   (identity "I have done something"))))
      (is (= nil (with-logbook user :got-pwned
                   (identity "I have done something")))))
    (testing "If the event is unknown, but we changed the default, we execute the operation and write to the logbook"
      (set-config! :allow-undeclared-events? true)
      (is (= "I have done something" @(with-logbook user :got-pwned
                                        (identity "I have done something"))))
      (is (= "I have done something else" @(with-logbook user :got-pwned
                                             (identity "I have done something else"))))
      (is (= 2 (count (:timestamps (first (filter #(= (:event %) :got-pwned) (:logbooks @user))))))))))
