(defproject hden/debezium-embedded-jdbc "0.2.0-SNAPSHOT"
  :description "Debezium DatabaseHistory and OffsetBackingStore implementations backed by JDBC."
  :url "https://github.com/hden/debezium-embedded-jdbc"
  :license {:name "Apache-2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.clojure/data.json "2.4.0"]
                 [org.apache.kafka/connect-runtime "3.4.0"]
                 [io.debezium/debezium-core "1.9.7.Final"]
                 [hikari-cp "3.0.1"]
                 [honeysql "1.0.461"]]
  :plugins [[lein-cloverage "1.2.4"]]
  :repl-options {:init-ns io.debezium.contrib.jdbc.core}
  :aot [io.debezium.contrib.jdbc.history
        io.debezium.contrib.jdbc.offset]
  :profiles
  {:uberjar {:aot :all}
   :dev {:dependencies [[org.xerial/sqlite-jdbc "3.41.2.2"]
                        [org.slf4j/slf4j-nop "2.0.7"]]}})
