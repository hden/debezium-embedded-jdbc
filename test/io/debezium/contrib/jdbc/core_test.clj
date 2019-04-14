(ns io.debezium.contrib.jdbc.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [io.debezium.contrib.jdbc.core :refer :all]))

(defn- create-pool []
  (let [pool (create-connection-pool {})]
    (jdbc/with-db-connection [conn {:datasource pool}]
      (jdbc/execute! conn "create table foo (id integer primary key, content text)")
      (jdbc/insert! conn :foo {:id 1 :content "bar"}))
    pool))

(deftest connection-pool-test
  (testing "connect & disconnect"
    (let [pool (create-pool)]
      (is pool)
      (disconnect pool)))

  (testing "query"
    (let [pool (create-pool)]
      (is (= [{:content "bar"}]
             (query pool {:select [:content] :from [:foo]})))
      (disconnect pool)))

  (testing "execute"
    (let [pool (create-pool)]
      (execute! pool {:insert-into :foo
                      :columns [:content]
                      :values [["baz"]]})
      (is (= [{:content "bar"} {:content "baz"}]
             (query pool {:select [:content] :from [:foo] :order-by [:id]})))
      (disconnect pool))))
