(defproject pundit "0.3.0"
  :description "A REST client for Parse.com in Clojure"
  :url "http://github.com/steerio/pundit"
  :java-source-paths ["java"]
  :jar-exclusions [#"(^|/)\."]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.4"]
                 [delayed-map "1.0.0"]
                 [clj-http "0.9.1"]
                 [clj-time "0.7.0"]])
