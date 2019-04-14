# debezium-embedded-jdbc

Debezium DatabaseHistory and OffsetBackingStore implementations backed by JDBC.

## Usage

### JDBCOffsetBackingStore

```connector.properties
offset.storage=io.debezium.contrib.jdbc.JDBCOffsetBackingStore
offset.storage.postgres.table=offsets
offset.storage.postgres.instance.id=debezium
```

### JDBCDatabaseHistory

```connector.properties
database.history=io.debezium.contrib.jdbc.JDBCDatabaseHistory
database.history.postgres.table=schema
database.history.postgres.instance.id=debezium
```

## License
Apache 2.0
