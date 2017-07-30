(ns benjamin.core-test
  (:require [benjamin.core :refer [with-logbook set-persistence-fn! set-success-fn! set-events! set-allow-undeclared-events!]]
            [clojure.test :refer [testing deftest is use-fixtures with-test test-ns]]
            [detijd.core :as time]
            [clj-time.core :as t]))

(def clean-slate {:name "Benjamin Peirce" :logbook []})
(def user (atom clean-slate))

(defn my-fixture [f]
  (reset! user clean-slate)
  (let [unique? #(some? %)
            today? #(if-let [date (first (vals %))]
                      (time/today? date)
                      false)
            last-3-days? #(if-let [date (first (vals %))]
                            (time/last-days? date 3)
                            false)
            last-3-months? #(if-let [date (first (vals %))]
                              (time/last-months? date 3)
                              false)]
        (set-events! {:on-vacation last-3-months?
                      :account-blocked last-3-months?
                      :trial-has-ended unique?
                      :end-of-trial unique?
                      :follow-up unique?
                      :signed-up unique?
                      :categories-change today?
                      :subscription-reminder today?
                      :subscription-expired today?
                      :newsletter last-3-days?}))
  (set-persistence-fn! (fn [entity event] (swap! user (fn [entity] (let [logbook (conj (:logbook entity) {event (t/now)})]
                                                                   (assoc entity :logbook logbook))))))
  (set-success-fn! (constantly true))
  (f))

(use-fixtures :each my-fixture)

(deftest configuration
  (testing "Sanity check"
    (is (= (:name @user) "Benjamin Peirce"))
    (is (empty? (:logbook @user)))
    (use 'benjamin.core :reload)
    (testing "Throws error when `events' is not set"
      (is (thrown-with-msg? java.lang.Exception #"Please set event and predicate map"
                   (with-logbook @user :subscribed
                      (do)))))
    (testing "Throws error when `success-fn' is not set"
      (let [unique? #(some? %)
            today? #(if-let [date (first (vals %))]
                      (time/today? date)
                      false)
            last-3-days? #(if-let [date (first (vals %))]
                            (time/last-days? date 3)
                            false)
            last-3-months? #(if-let [date (first (vals %))]
                              (time/last-months? date 3)
                              false)]
        (set-events! {:on-vacation last-3-months?
                      :account-blocked last-3-months?
                      :trial-has-ended unique?
                      :end-of-trial unique?
                      :follow-up unique?
                      :signed-up unique?
                      :categories-change today?
                      :subscription-reminder today?
                      :subscription-expired today?
                      :newsletter last-3-days?}))
      (is (thrown-with-msg? java.util.concurrent.ExecutionException #"Please 'set-success-fn!` with a function of one argument"
                   @(with-logbook @user :follow-up
                      (do)))))
    (testing "Throws error when `persistence-fn' is not set"
      (set-success-fn! (constantly true))
      (is (thrown-with-msg? java.util.concurrent.ExecutionException #"Please 'set-persistence-fn!` with a function of two arguments"
                   @(with-logbook @user :follow-up
                      (do)))))
    (testing "Doesn't throw error when everything is set"
      (set-persistence-fn! (constantly true))
      (is (future? (with-logbook @user :follow-up
                     (do)))))))

(deftest persistence
  (testing "Persistence doesn't occur when success is denied"
    (set-persistence-fn! (fn [entity event] (swap! user (fn [entity] (let [logbook (conj (:logbook entity) {event (t/now)})]
                                                                   (assoc entity :logbook logbook))))))
    (set-success-fn! (constantly false))
    @(with-logbook @user :account-blocked
      (do))
    (is (empty? (:logbook @user))))
  (testing "Persistence occurs when success is confirmed"
    (reset! user clean-slate)
    (set-success-fn! (constantly true))
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
      (= 1 (count (filter :end-of-trial (:logbook @user)))))
    (testing "`Today` predicate means operation can be executed only if no other operation has been executed during the present day."
      (is (= "I have done something" @(with-logbook @user :categories-change
                                        (identity "I have done something"))))
      (is (= nil (with-logbook @user :categories-change
                   (identity "I have done something"))))
      (= 1 (count (filter :categories-change (:logbook @user)))))
    (testing "If no predicate is associated with the event, we execute the operation and write to logbook (default)"
      (is (= "I have done something" @(with-logbook @user :subscribed
                                        (identity "I have done something"))))
      (is (= "I have done something" @(with-logbook @user :subscribed
                                        (identity "I have done something"))))
      (is (= 2 (count (filter :subscribed (:logbook @user))))))
    (testing "If no predicate is associated with the event, we don't execute the operation and don't write to the logbook"
      (set-allow-undeclared-events! false)
      (is (= nil (with-logbook @user :subscribed
                   (identity "I have done something")))))))


