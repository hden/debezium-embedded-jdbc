(ns io.debezium.contrib.jdbc.offset
  (:require [clojure.data.json :as json]
            [io.debezium.contrib.jdbc.core :as core])
  (:import [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets])
  (:gen-class
    :name io.debezium.contrib.jdbc.JDBCOffsetBackingStore
    :extends org.apache.kafka.connect.storage.MemoryOffsetBackingStore
    :exposes {data {:get getData :set setData}}
    :exposes-methods {start superStart
                      stop superStop
                      get superGet
                      configure superConfigure}
    :init init
    :state state))

(defn string-from-byte-buffer [x]
  (when x
    (String. (.array x))))

(defn string-to-byte-buffer [x]
  (when x
    (ByteBuffer/wrap (.getBytes x StandardCharsets/UTF_8))))

(defn map-kv [f m]
  (into {}
        (map (fn [[k v]]
               [(f k) (f v)]))
        m))

(defn initiate-storage! [connection-pool table-name instance-id]
  (try
    (core/execute! connection-pool {:insert-into (keyword table-name)
                                    :columns [:id :content]
                                    :values [[instance-id "{}"]]})
    (catch Exception e
      nil)))

(defn read-offset [connection-pool table-name instance-id]
  (when-let [m (first (core/query connection-pool {:select [:content]
                                                   :from [(keyword table-name)]
                                                   :where [:= :id instance-id]}))]
    (json/read-str (get m :content "{}"))))

(defn write-offset! [connection-pool table-name instance-id offset]
  (core/execute! connection-pool {:update (keyword table-name)
                                  :where [:= :id instance-id]
                                  :set {:content (json/write-str offset)}}))

(defn -init []
  [[] (atom {})])

(defn -configure [this config]
  (.superConfigure this config)
  (let [state (.state this)
        config (into {} (.originals config))
        table-name (get config "offset.storage.postgres.table" "offsets")
        instance-id (get config "offset.storage.postgres.instance.id" "default")]
    (swap! state merge {:table-name table-name
                        :instance-id instance-id})))

(defn -start [this]
  (.superStart this)
  (let [state (.state this)
        {:keys [table-name instance-id]} @state
        options {:pool-name (format "JDBCOffsetBackingStore (%s)" instance-id)}
        connection-pool (core/create-connection-pool options)]
    (swap! state assoc :connection-pool connection-pool)
    (initiate-storage! connection-pool table-name instance-id)))

(defn -stop [this]
  (.superStop this)
  (let [{:keys [connection-pool]} @(.state this)]
    (core/disconnect connection-pool)))

(defn -get [this keys callback]
  (let [{:keys [connection-pool table-name instance-id]} @(.state this)]
    ;; Restore offset.
    (.setData this (java.util.HashMap. (map-kv string-to-byte-buffer (read-offset connection-pool table-name instance-id))))
    (.superGet this keys callback)))

(defn -save [this]
  (let [{:keys [connection-pool table-name instance-id]} @(.state this)
        offet (map-kv string-from-byte-buffer (.getData this))]
    (write-offset! connection-pool table-name instance-id offet)))
