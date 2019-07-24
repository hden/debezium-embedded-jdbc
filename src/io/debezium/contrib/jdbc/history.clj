(ns io.debezium.contrib.jdbc.history
  (:require [io.debezium.contrib.jdbc.core :as core])
  (:import [java.nio.charset StandardCharsets]
           [io.debezium.document DocumentReader DocumentWriter]
           [io.debezium.relational.history HistoryRecord])
  (:gen-class
    :name io.debezium.contrib.jdbc.JDBCDatabaseHistory
    :extends io.debezium.relational.history.AbstractDatabaseHistory
    :exposes-methods {configure superConfigure}
    :init init
    :state state))

(def charset StandardCharsets/UTF_8)
(def reader (DocumentReader/defaultReader))
(def writer (DocumentWriter/defaultWriter))

(defn serialize-record [x]
  (.write writer (.document x)))

(defn deserialize-record [x]
  (new HistoryRecord (.read reader x)))

(defn count-records [connection-pool table-name instance-id]
  (:count (first (core/query connection-pool {:select [[:%count.* :count]]
                                              :from [(keyword table-name)]
                                              :where [:= :instance_id instance-id]}))))

(defn read-records [connection-pool table-name instance-id]
  (into []
        (comp (map :content)
              (map deserialize-record))
        (core/query connection-pool {:select [:content]
                                     :from [(keyword table-name)]
                                     :where [:= :instance_id instance-id]
                                     :order-by [:id]})))

(defn write-record! [connection-pool table-name instance-id record]
  (let [content (serialize-record record)]
    (core/execute! connection-pool {:insert-into (keyword table-name)
                                    :columns [:instance_id :content]
                                    :values [[instance-id content]]})))

(defn -init []
  [[] (atom {})])

(defn -configure [this config comparator listener]
  (.superConfigure this config comparator listener)
  (let [state (.state this)
        database-url (.getString config "database.history.jdbc.url" "jdbc:sqlite:")
        username (.getString config "database.history.jdbc.username")
        password (.getString config "database.history.jdbc.password")
        table-name (.getString config "database.history.jdbc.table" "schema")
        instance-id (.getString config "database.history.jdbc.instance.id" "default")]
    (swap! state merge {:database-url database-url
                        :username username
                        :password password
                        :table-name table-name
                        :instance-id instance-id})))

(defn -start [this]
  (let [state (.state this)
        {:keys [database-url username password instance-id]} @state
        options {:pool-name (format "PostgresDatabaseHistory (%s)" instance-id)
                 :username username
                 :password password
                 :jdbc-url database-url}]
    (swap! state merge {:connection-pool (core/create-connection-pool options)})))

(defn -stop [this]
  (let [{:keys [connection-pool]} @(.state this)]
    (when connection-pool
      (core/disconnect connection-pool))))

(defn -exists [this]
  (let [{:keys [connection-pool table-name instance-id]} @(.state this)]
    (> (count-records connection-pool table-name instance-id)
       0)))

(defn -storeRecord [this record]
  (when record
    (let [{:keys [connection-pool table-name instance-id]} @(.state this)]
      (write-record! connection-pool table-name instance-id record))))

(defn -recoverRecords [this consumer]
  (let [{:keys [connection-pool table-name instance-id]} @(.state this)]
    (doseq [record (read-records connection-pool table-name instance-id)]
      (.accept consumer record))))
