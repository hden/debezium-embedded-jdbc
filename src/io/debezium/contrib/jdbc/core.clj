(ns io.debezium.contrib.jdbc.core
  (:require [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :as hikari]
            [honeysql.core :as honeysql]))

(defn get-jdbc-url []
  (or (System/getenv "JDBC_DATABASE_URL")
      "jdbc:sqlite:"))

(defn create-connection-pool [x]
  (let [jdbc-url (get-jdbc-url)]
    (hikari/make-datasource (merge {:maximum-pool-size 1 :jdbc-url jdbc-url}
                                   x))))

(defn query [pool q]
  (jdbc/with-db-connection [conn {:datasource pool}]
    (jdbc/query conn (honeysql/format q))))

(defn execute! [pool q]
  (jdbc/with-db-connection [conn {:datasource pool}]
    (jdbc/execute! conn (honeysql/format q))))

(defn disconnect [pool]
  (hikari/close-datasource pool))
