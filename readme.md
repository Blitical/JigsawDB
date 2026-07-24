![JigsawDB banner](./quickstart/assets/banner.png)

> [!WARNING]
> **JigsawDB is currently in public beta.**
> If you run into issues, please report them in our Discord: https://discord.gg/nKAZa796ua

# JigsawDB

JigsawDB is a Java library for building and executing SQL through a type-safe API. It is designed to reduce hand-written SQL and catch as much table/query misuse as possible at compile time.

## Features

- **Database initialization** for SQLite, MySQL, MariaDB, and PostgreSQL through JDBC drivers.
- **Automatic table creation and migration** for added, removed, and modified columns.
- **Type-safe querying** through generated field classes.
- **Configurable caching** with eager, lazy, disabled, mapped, and field-specific policies.
- **Data encoding** for JSON, enums, timestamps, UUIDs, Java serialization, binary values, and common primitive types.
- **Buckets and queued execution** for batching database work.

## Installation

JigsawDB is published on Maven Central.

For Gradle examples, see:

- [Gradle](./quickstart/gradle.quickstart.md)
- [Gradle Kotlin DSL](./quickstart/gradle.kts.quickstart.md)

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>dev.blitical</groupId>
        <artifactId>JigsawDB</artifactId>
        <version><!--VERSION-->1.0.0-beta.19<!--END_VERSION--></version>
    </dependency>
</dependencies>
```

If your Maven build does not automatically discover annotation processors from dependencies, add JigsawDB to `maven-compiler-plugin` annotation processor paths as well.

## Database Providers

JigsawDB does not bundle JDBC database providers in its ShadowJar. Install the provider for whichever database you use.

### SQLite

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.53.2.0</version>
</dependency>
```

### MySQL

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>9.7.0</version>
</dependency>
```

### MariaDB

```xml
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.5.9</version>
</dependency>
```

### PostgreSQL

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.13</version>
</dependency>
```

## Quickstart

Create a table by extending `Table<YOUR_TABLE, PRIMARY_KEY_TYPE>`:

```java
import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.annotations.PrimaryColumn;
import dev.blitical.jigsawDB.table.Table;

import java.util.UUID;

public class MessageTable extends Table<MessageTable, UUID> {
    @PrimaryColumn
    @Column("uuid")
    UUID uuid;

    @Column("message")
    String message;
}
```

Connect to the database and register the table:

```java
import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.DatabaseBuilder;
import dev.blitical.jigsawDB.drivers.SQLiteDriver;

public final class Database {
    public static final ConnectedDatabase DATABASE =
            new DatabaseBuilder(new SQLiteDriver("./messages.sqlite"))
                    .addTable(new MessageTable())
                    .connect()
                    .complete();
}
```

Use the generated fields class to query and update values:

```java
import java.util.UUID;

public static void main(String[] args) {
    var entry = Database.DATABASE
            .getOrCreateEntry(MessageTable.class, UUID.randomUUID())
            .complete();

    entry.set(MessageTableFields.message, "Hello World!").complete();

    entry.get(MessageTableFields.message).queue(System.out::println);
}
```

`complete()` blocks until the operation finishes. `queue(...)` schedules the operation and runs the callback once the result is available.

## License

JigsawDB is licensed under the [GPL v3.0 license](./LICENSE).

```text
Copyright Blitical 2026

Licensed under GNU GENERAL PUBLIC LICENSE version 3.
We are not liable for any damages caused by the usage,
distribution, or modification of this project.
WE PROVIDE NO WARRANTY OF ANY KIND.
```

## Social Links

- Discord: https://discord.gg/nKAZa796ua
