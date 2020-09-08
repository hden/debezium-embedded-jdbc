(defproject hden/debezium-embedded-jdbc "0.2.0-SNAPSHOT"
  :description "Debezium DatabaseHistory and OffsetBackingStore implementations backed by JDBC."
  :url "https://github.com/hden/debezium-embedded-jdbc"
  :license {:name "Apache-2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [org.clojure/data.json "1.0.0"]
                 [org.apache.kafka/connect-runtime "2.6.0"]
                 [io.debezium/debezium-core "1.2.3.Final"]
                 [hikari-cp "2.13.0"]
                 [honeysql "1.0.444"]]
  :plugins [[lein-cloverage "1.2.0"]]
  :repl-options {:init-ns io.debezium.contrib.jdbc.core}
  :aot [io.debezium.contrib.jdbc.history
        io.debezium.contrib.jdbc.offset]
  :profiles
  {:uberjar {:aot :all}
   :dev {:dependencies [[org.xerial/sqlite-jdbc "3.32.3.2"]
                        [org.slf4j/slf4j-nop "1.8.0-beta4"]]}})
