(ns io.debezium.contrib.jdbc.history_test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :as hikari]
            [io.debezium.contrib.jdbc.history :refer :all])
  (:import [java.util.function Consumer]
           [io.debezium.config Configuration]
           [io.debezium.contrib.jdbc JDBCDatabaseHistory]
           [io.debezium.relational.history DatabaseHistoryListener]))

(def fixture "{\"foo\":\"bar\"}")
(def database-url "jdbc:sqlite:")
(def table-name "histories")
(def instance-id "test")

(defn- create-pool []
  (let [pool (hikari/make-datasource {:maximum-pool-size 1 :jdbc-url database-url})]
    (jdbc/with-db-connection [conn {:datasource pool}]
      (jdbc/execute! conn "create table histories (id integer primary key, instance_id text, content text)")
      (jdbc/insert! conn table-name {:id 1 :instance_id instance-id :content fixture})
      (jdbc/insert! conn table-name {:id 2 :instance_id "foobar" :content fixture}))
    pool))

(deftest document-test
  (testing "serialize & deserialize"
    (is (= fixture
           (serialize-record (deserialize-record fixture))))))

(deftest boundary-test
  (let [pool (create-pool)]
    (testing "count-records"
      (is (= 1 (count-records pool table-name instance-id))))

    (testing "read-records"
      (is (= 1 (count (read-records pool table-name instance-id)))))

    (testing "write-record"
      (write-record! pool table-name instance-id (deserialize-record fixture))
      (is (= 2 (count (read-records pool table-name instance-id)))))))

(defn- create-instance []
  (let [instance (JDBCDatabaseHistory.)
        state (.state instance)]
    (reset! state {:table-name table-name
                   :instance-id instance-id
                   :connection-pool (create-pool)})
    instance))

(defn- create-consumer [records]
  (reify Consumer
    (accept [_ x]
      (swap! records conj x))))

(deftest JDBCDatabaseHistory-test
  (testing "init"
    (let [instance (JDBCDatabaseHistory.)]
      (is (= {} @(.state instance)))))

  (testing "configure"
    (let [instance (JDBCDatabaseHistory.)
          config (Configuration/from {"database.history.jdbc.url" database-url
                                      "database.history.jdbc.table" (name table-name)
                                      "database.history.jdbc.instance.id" instance-id})]
      (.configure instance config nil DatabaseHistoryListener/NOOP true)
      (is (= {:database-url database-url
              :username nil
              :password nil
              :table-name table-name
              :instance-id instance-id}
             @(.state instance)))))

  (testing "exists"
    (let [instance (create-instance)]
      (is (.exists instance))
      (.stop instance)))

  (testing "accessors"
    (let [instance (create-instance)
          records (atom [])
          consumer (create-consumer records)]
      (.storeRecord instance (deserialize-record fixture))
      (.recoverRecords instance consumer)
      (is (= 2 (count @records)))
      (.stop instance))))
