(ns benjamin.core-test
  (:require [benjamin.core :refer [with-logbook set-persistence-fn! set-success-fn! set-events!]]
            [clojure.test :refer [testing deftest is use-fixtures with-test test-ns]]
            [detijd.core :as time]
            [clj-time.core :as t]))

(def clean-slate {:name "Benjamin Peirce" :logbook []})
(def user (atom clean-slate))

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

(deftest configuration
  (testing "Sanity check"
    (reset! user clean-slate)
    (is (= (:name @user) "Benjamin Peirce"))
    (is (empty? (:logbook @user)))
    (testing "Throws error when success-fn is not set"
      (is (thrown? java.util.concurrent.ExecutionException
                   @(with-logbook @user :subscribed
                      (do))))
      (is (thrown? java.util.concurrent.ExecutionException #"Please 'set-success-fn!` with a function of one argument (result)"
                   @(with-logbook @user :subscribed
                      (do)))))
    (testing "Doesn't throw error when success-fn is set"
      (set-persistence-fn! (constantly true))
      (set-success-fn! (constantly true))
      (is (future? (with-logbook @user :subscribed
                     (do)))))))

(deftest persistence
  (testing "Persistence doesn't occur when success is denied"
    (reset! user clean-slate)
    (set-persistence-fn! (fn [entity event] (swap! user (fn [entity] (let [logbook (conj (:logbook entity) {event (t/now)})]
                                                                   (assoc entity :logbook logbook))))))
    (set-success-fn! (constantly false))
    @(with-logbook @user :subscribed
      (do))
    (is (empty? (:logbook @user))))
  (testing "Persistence occurs when success is confirmed"
    (reset! user clean-slate)
    (set-success-fn! (constantly true))
    @(with-logbook @user :subscribed
      (do))
    (is (contains? @user :logbook))
    (is (boolean (some :subscribed (:logbook @user))))))

(deftest predicates
  (testing "Predicates determine if operation is done, and if logbook gets written."
    (reset! user clean-slate)
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
    (testing "If no predicate is associated with the event, we execute the operation and write to logbook"
      (is (= "I have done something" @(with-logbook @user :subscribed
                                        (identity "I have done something"))))
      (is (= "I have done something" @(with-logbook @user :subscribed
                                        (identity "I have done something"))))
      (is (= 2 (count (filter :subscribed (:logbook @user))))))))

(defn test-ns-hook []
  (configuration)
  (persistence)
  (predicates))

(println benjamin.core/events)
