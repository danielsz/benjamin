(ns benjamin.core-test
  (:require [benjamin.core :refer [with-logbook]]
            [benjamin.configuration :as config :refer [set-config!]]
            [benjamin.predicates :refer [unique? today? last-3-days? last-3-months?]]
            [clojure.test :refer [testing deftest is use-fixtures]]
            [clj-time.core :as t]))

(def clean-slate {:name "Benjamin Peirce"})
(def user (atom clean-slate))

(defn my-fixture [f]
  (reset! user clean-slate)
  (set-config! :events {:account-blocked last-3-months?
                        :end-of-trial unique?
                        :follow-up unique?
                        :categories-change today?
                        :newsletter last-3-days?})
  (set-config! :persistence-fn (fn [entity event] (swap! user (fn [entity] (let [logbook (conj (:logbook entity) {event (t/now)})]
                                                                           (assoc entity :logbook logbook))))))
  (set-config! :success-fn (constantly true))
  (f))

(use-fixtures :each my-fixture)

(deftest configuration
  (testing "Sanity check"
    (is (= (:name @user) "Benjamin Peirce"))
    (is (empty? (:logbook @user)))
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
    (testing "Throws error when `persistence-fn' is not set"
      (is (thrown-with-msg? java.util.concurrent.ExecutionException #"Please run 'set-config! :persistence-fn!` with a function of two arguments"
                            @(with-logbook @user :follow-up
                               (do)))))
    (testing "Doesn't throw error when everything is set"
      (set-config! :persistence-fn (constantly true))
      (is (future? (with-logbook @user :follow-up
                     (do)))))))

(deftest persistence
  (testing "Persistence doesn't occur when success is denied"
    (set-config! :success-fn (constantly false))
    @(with-logbook @user :account-blocked
       (do))
    (is (empty? (:logbook @user))))
  (testing "Persistence occurs when success is confirmed"
    (reset! user clean-slate)
    (set-config! :success-fn (constantly true))
    @(with-logbook @user :account-blocked
       (do))
    (is (contains? @user :logbook))
    (is (boolean (some :account-blocked (:logbook @user))))))

(deftest predicates
  (testing "Predicates determine if operation is done, and if logbook gets written."
    (testing "`Unique` predicate means operation can be executed once only."
      (is (= "I have done something" @(with-logbook @user :end-of-trial
                                        (identity "I have done something"))))
      (is (= nil (with-logbook @user :end-of-trial
                   (identity "I have done something"))))
      (is (= 1 (count (filter :end-of-trial (:logbook @user))))))
    (testing "`Today` predicate means operation can be executed only if no other operation has been executed during the present day."
      (is (= "I have done something" @(with-logbook @user :categories-change
                                        (identity "I have done something"))))
      (is (= nil (with-logbook @user :categories-change
                   (identity "I have done something"))))
      (is (= 1 (count (filter :categories-change (:logbook @user))))))))

(deftest events
  (testing "If the event is unknown, we don't execute the operation and don't write to the logbook (default)."
    (testing "deault setting"
      (set-config! :allow-undeclared-events? false)
      (is (= nil (with-logbook @user :subscribed
                   (identity "I have done something"))))
      (is (= nil (with-logbook @user :got-pwned
                   (identity "I have done something")))))
    (testing "If the event is unknown, but we changed the default, we execute the operation and write to the logbook"
      (set-config! :allow-undeclared-events? true)
      (is (= "I have done something" @(with-logbook @user :got-pwned
                                        (identity "I have done something"))))
      (is (= "I have done something else" @(with-logbook @user :got-pwned
                                             (identity "I have done something else"))))
      (is (= 2 (count (filter :got-pwned (:logbook @user))))))))
