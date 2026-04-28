## Quickstart guide (Gradle)

JigsawDB is published on MavenCentral:<br>
<table>
  <tr>
    <th><a href="../readme.md">Maven</a></th>
    <th><b>Gradle</b></th>
    <th><a href="./gradle.kts.quickstart.md">Gradle (kts)</a></th>
  </tr>
  <tr>
    <td colspan="3">
        <pre lang="groovy"><code>repositories {
    mavenCentral()
}</code></pre>
    </td>
  </tr>
</table>
And can easily be installed as a dependency with:<br>
<table>
  <tr>
    <th><a href="../readme.md">Maven</a></th>
    <th><b>Gradle</b></th>
    <th><a href="./gradle.kts.quickstart.md">Gradle (kts)</a></th>
  </tr>
  <tr>
    <td colspan="3">
      <pre lang="groovy"><code>dependencies {
    implementation "dev.blitical:JigsawDB:<!--VERSION-->1.0.0-beta.3<!--END_VERSION-->"
    annotationProcessor "dev.blitical:JigsawDB:<!--VERSION-->1.0.0-beta.3<!--END_VERSION-->"
}</code></pre>
    </td>
  </tr>
</table>

<br>And to use it, simply create a new Table:

```java
// Define it as a table by ensuring it extends Table<YOUR_CLASS, PRIMARY_FIELD_TYPE>
public class JigsawDBTable extends Table<JigsawDBTable, UUID> {
    // There can only be one primary column, annotate it with @PrimaryColumn
    @PrimaryColumn
    @Column("UUID")
    UUID UUID;

    // Add as many additional columns as needed
    @Column("message")
    String message;
}
```

And connect to it with a database driver:

```java
public static final ConnectedDatabase DATABASE =
        new DatabaseBuilder(new SQLiteDriver("./myDatabase.sqlite"))
                .addTable(new JigsawDBTable()) // Add your table here
                .connect() // And connect with it
                .complete();
```

And now you can execute queries with ease:

```java
public static void main(String[] args) {
    // entry is never null as we are creating it if it doesn't exist
    var entry = DATABASE.getOrCreateEntry(JigsawDBTable.class, UUID.randomUUID()).complete();
    // Set the value of message to "Hello World!"
    // JigsawDBTableFields is a generated class; you may need to run your repository to be able to use it
    entry.set(JigsawDBTableFields.message, "Hello World!").complete(); // #complete() is thread-blocking
    // Get the value of string asynchronously and queue the result to be logged once received
    entry.get(JigsawDBTableFields.string).queue(result -> {
        System.out.println(result);
    });
}
```

For more information on generated classes (`JigsawDBTableFields`) or the ExecutableFuture queue system (`queue()` and
`complete()`), please take a read of our wiki [here]()
