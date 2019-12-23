(defproject hden/debezium-embedded-jdbc "0.2.0-SNAPSHOT"
  :description "Debezium DatabaseHistory and OffsetBackingStore implementations backed by JDBC."
  :url "https://github.com/hden/debezium-embedded-jdbc"
  :license {:name "Apache-2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.clojure/data.json "0.2.7"]
                 [org.apache.kafka/connect-runtime "2.3.0"]
                 [io.debezium/debezium-core "0.10.0.Final"]
                 [hikari-cp "2.10.0"]
                 [honeysql "0.9.8"]]
  :plugins [[lein-cloverage "1.1.2"]]
  :repl-options {:init-ns io.debezium.contrib.jdbc.core}
  :aot [io.debezium.contrib.jdbc.history
        io.debezium.contrib.jdbc.offset]
  :profiles
  {:uberjar {:aot :all}
   :dev {:dependencies [[org.xerial/sqlite-jdbc "3.30.1"]
                        [org.slf4j/slf4j-nop "1.8.0-beta4"]]}})
