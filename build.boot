(set-env!
 :source-paths   #{"src"}
 :resource-paths #{"src"}
 :dependencies '[[org.danielsz/detijd "0.1.0-SNAPSHOT" :scope "test"]
                 [adzerk/boot-test "1.2.0" :scope "test"]
                 [metosin/boot-alt-test "0.3.2" :scope "test"]
                 [org.clojure/tools.logging "0.3.1" :scope "test"]])

(task-options!
 push {:repo-map {:url "https://clojars.org/repo/"}}
 pom {:project 'org.danielsz/benjamin
      :version "0.1.0-SNAPSHOT"
      :scm {:name "git"
            :url "https://github.com/danielsz/benjamin"}})

(require '[adzerk.boot-test :refer [test run-tests]]
         '[metosin.boot-alt-test :refer (alt-test)])

(deftask testing
  "Profile setup for running tests."
  []
  (set-env! :source-paths #{"test"})
  (comp
   (watch)
   (notify :visual true)
   (test :exclusions #{'benjamin.core})))

(deftask dev
  "Profile setup for running tests."
  []
  (set-env! :source-paths #(conj % "test"))
  (comp
   (watch)
   (notify :visual true)
   (repl :server true)))

(deftask build
  []
  (comp (pom) (jar) (install)))

(deftask push-release
  []
  (comp
   (build)
   (push)))
