(ns io.debezium.contrib.jdbc.offset_test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :as hikari]
            [io.debezium.contrib.jdbc.core :as core]
            [io.debezium.contrib.jdbc.offset :refer :all])
  (:import [org.apache.kafka.common.config ConfigDef]
           [org.apache.kafka.connect.runtime WorkerConfig]
           [io.debezium.contrib.jdbc JDBCOffsetBackingStore]))

(deftest helper-test
  (testing "round trip"
    (let [fixture "foobar"]
      (is (= fixture (string-from-byte-buffer (string-to-byte-buffer fixture)))))))

(def database-url "jdbc:sqlite:")
(def table-name "offsets")
(def instance-id "test")

(defn- create-pool []
  (let [pool (hikari/make-datasource {:maximum-pool-size 1 :jdbc-url database-url})]
    (jdbc/with-db-connection [conn {:datasource pool}]
      (jdbc/execute! conn "create table offsets (id text primary key, content text)")
      (jdbc/insert! conn table-name {:id "foobar" :content "{\"x\":\"y\"}"}))
    pool))

(deftest boundary-test
  (let [pool (create-pool)]
    (testing "init"
      (is (initiate-storage! pool table-name instance-id))
      (is (= nil (initiate-storage! pool table-name instance-id)))
      (is (= {}
             (read-offset pool table-name instance-id))))

    (testing "read-after-write-offset"
      (let [fixture {"key" "value"}]
        (write-offset! pool table-name instance-id fixture)
        (is (= fixture (read-offset pool table-name instance-id)))))))

(defn create-config []
  (proxy [WorkerConfig] [(ConfigDef.) {}]
    (originals []
      {"offset.storage.jdbc.url" database-url
       "offset.storage.jdbc.table" (name table-name)
       "offset.storage.jdbc.instance.id" instance-id})))

(defn- create-instance []
  (let [instance (JDBCOffsetBackingStore.)
        state (.state instance)]
    (.configure instance (create-config))
    instance))

(deftest JDBCOffsetBackingStore-test
  (testing "init"
    (let [instance (JDBCOffsetBackingStore.)]
      (is (= {} @(.state instance)))))

  (testing "configure"
    (let [instance (JDBCOffsetBackingStore.)]
      (.configure instance (create-config))
      (is (= {:database-url database-url
              :username nil
              :password nil
              :table-name table-name
              :instance-id instance-id}
             @(.state instance)))))

  (testing "start"
    (let [instance (create-instance)]
      (.start instance)
      (is (= {}
             (into {} (.getData instance))))
      (.stop instance)))

  (testing "save"
    (let [fixture {"foo" "bar"}
          instance (create-instance)
          state (.state instance)
          pool (create-pool)]
      (initiate-storage! pool table-name instance-id)
      (swap! state merge {:connection-pool pool
                          :table-name table-name
                          :instance-id instance-id})
      (doto instance
        (.setData (java.util.HashMap. (map-kv string-to-byte-buffer fixture)))
        (.save))
      (is (= fixture
             (read-offset pool table-name instance-id)))
      (.stop instance)))

  (testing "get"
    (let [fixture {"foo" "bar"}
          instance (create-instance)
          state (.state instance)
          pool (create-pool)]
      (initiate-storage! pool table-name instance-id)
      (write-offset! pool table-name instance-id fixture)
      (.start instance)
      (swap! state merge {:connection-pool pool
                          :table-name table-name
                          :instance-id instance-id})
      (is (= fixture
             (map-kv string-from-byte-buffer @(.get instance [(string-to-byte-buffer "foo")])))))))
