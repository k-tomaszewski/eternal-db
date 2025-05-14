# eternal-db
[![Unit Tests](https://github.com/k-tomaszewski/eternal-db/actions/workflows/maven.yml/badge.svg)](https://github.com/k-tomaszewski/eternal-db/actions/workflows/maven.yml)

## Overview
Eternal DB is a Java embedded time series database/data storage engine with a data retention policy based on a disk space. 
As oldest records are removed when needed to reclaim disk space, this database allows to collect data eternally. It gives unmatched control
on a disk resource usage. Moreover, data are easily accessible for any tools as they are kept in regular disk files on a given file system.
It's a schema-less document database as records are just JSON documents.

Some possible use cases:
- collecting measurement data
- collecting metrics
- collecting market data

In above cases the most important thing for a running system is to keep some time window of latest data. Eternal DB allows to express this 
time window in terms of a disk space.

**Please note, that this is not a classic database system like RDBMS.**
It doesn't support SQL, users, transactions, constraints, relations, triggers, stored procedures, etc.
Maybe the best term is a "data storage engine".

As this is designed to be used under Linux, it requires Coreutils https://www.gnu.org/software/coreutils/ or a compatible software module
that supports running the command `du -sk`. Most likely it can work with BusyBox as well: https://www.busybox.net/downloads/BusyBox.html#du
(not verified yet).

NOTE: This project is a work-in-progress (WIP).

## Project goals
1. It is an embedded database (data store) that is easy to add to a program written in Java.
2. It has a disk space based data retention policy. This is meant to keep as much data as possible within a defined disk space limit.
3. When reclaiming disk space, oldest data records are removed.
4. Records are kept in a text format in files on a given filesystem and these files are easily readable by other programs.
5. Records are considered documents, like in document databases. No fixed data structure is forced.
6. A record always has a timestamp and time is considered an indexed attribute.
7. It is meant to run in a Linux environment.
8. It is thread-safe.
9. It is fault-tolerant. A single error should not block you from reading all your data. As files are written only by appending a corrupted
   file may only be a result of not closing a database object before JVM exit. In this case some records may be lost, but a single data file
   may have only the last record corrupted. A database can continue using this file when started again.

## Non-goals
1. This is not meant to run as a standalone server enabling multiple clients to connect and use.
2. Ability to configure other data retention policy is not required.
3. Support for any other operating system is not required if such an operating system cannot provide Linux-compatible runtime environment.
4. Strict schema validation for data is not a goal.
5. Strict transactions support is not a goal.
6. Data access control is not a goal. A user is responsible to configure desired access rights to a directory defined for data files.
7. Valid use of a single database storage from many database instances is not a goal.
8. Support for efficient searching of records based on any other criteria than time period is not a goal.

## Design decisions
1. Project is developed in Java 21.
2. Records are kept formatted as single line JSON each.
3. Each line in a data file is beginning with timestamp being number of millis from the Epoch (as given by `System.currentTimeMillis()`)
   and formatted with radix of 32 (to use less disk space), followed by the tab character, followed by JSON one-liner.
4. Unix line endings are used in data files, as it is only 1 byte and it makes it easier to detect corrupted file.
5. Data are persisted as a disk files in directory tree that is representing periods of time.
6. Project is avoiding unnecessary dependencies. No Vavr, Project Reactor, Guava, etc.
7. The database should be easy to use in Spring Boot based application.
8. By design there is no protection against improper use like two Eternal-db instances (class `Database`) writing independently using overlapping directories on disk. Just to keep it simple.
9. A single Eternal-db database folder can be shared by multiple independently used read-only Eternal-db instances (class `ReadOnlyDatabase`).

## Usage
### Dependency
Add dependency to eternal-db library (use the latest version):
```xml
<dependency>
  <groupId>io.github.k_tomaszewski</groupId>
  <artifactId>eternal-db</artifactId>
  <version>1.0.0</version>
</dependency>
```
As of now the library is published only in GiHub Packages repository, so you need to add additional Maven repository in your `pom.xml` like this:
```xml
<repositories>
    <repository>
        <id>github_k-tomaszewski_eternal-db</id>
        <url>https://maven.pkg.github.com/k-tomaszewski/eternal-db</url>
    </repository>
</repositories>
```
It seems that GitHub Packages repository requires authentication even for public
artifacts, so you need to have a GitHub account and you need to set up your credentials
(GitHub username and access token) for Maven in the `settings.xml` file. Ref:
- https://maven.apache.org/guides/mini/guide-multiple-repositories.html
- https://maven.apache.org/guides/mini/guide-deployment-security-settings.html
- https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token

### Opening a database for read-write access
To use it you create a single object of class `io.github.k_tomaszewski.eternaldb.Database`.
Such object gives access to operations on a single data store.

Please note that the `Database` is a generic class and has a type parameter. This is used for type
checking only, so a compiler can detect when a wrong object is passed for writing method.

To create instance of the `Database` class you need to provide a `DatabaseProperties` object, which contains following 
configuration properties:
- path to a directory where data files are going to be stored
- limit of disk space to use, given as integer number of megabytes (MB)
- object being strategy for serialization and deserialization, the library provides `JacksonSerialization` instance as a default
- object being strategy for naming data files, the library provides `BasicFileNaming` instance as a default
- timestamp supplier - object implementing `ToLongFunction<T>` that returns timestamp, possibly based on a record being written. This is optional.

Example:
```java
Database<MyRecord> db = new Database<>(new DatabaseProperties<>(Path.of("/home/db"), 100));
```
You need one such object for a single data storage directory. Instances of `Database` class are
thread-safe. You must not use many instances of `Database` class that are configured to use
the same directory or subdirectories of each other.

### Writing data
A basic method for writing data: `void write(T record, long recordMillis)`. To use it you
need to give record timestamp when writing as this is a time series database. Example:
```java
db.write(myRecord, System.currentTimeMillis());
```

If a database is configured with a timestamp supplier (an object implementing `ToLongFunction<T>` interface),
then another data writting method can be used: `void write(T record)`. Calling `write(x)` is equivalent
of calling `write(x, timestampSupplier.applyAsLong(x))`. This can be useful when your domain model
already contains a timestamp attribute.

### Reading data
There is just one method for reading data: `Stream<Timestamped<U>> read(Class<U> type, Long minMillis, Long maxMillis)`.
This is designed to read a set of records with timestamps matching a given range [minMillis, maxMillis].
Both ends of a time range are optional. Example of reading all records with timestamps starting
with given time:
```java
List<MyRecord> entities = db.read(MyRecord.class, fromMillis, null)
        .map(Timestamped::record).toList();
```
Here the given type (`MyRecord.class` in the example above) is used for deserialization purpose.

### Filtering
The main data filter provided is a time-range, as described above. This can take following variants:
- no time-range filtering: minMillis = null and maxMillis = null
- open-ended time-range: minMillis = null or maxMillis = null
- time-range filtering: minMillis and maxMillis are both not null

Other means of filtering may be applied on a returned `java.util.Stream` using its [filter]( 
https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#filter(java.util.function.Predicate) ) method.

#### Counting
You can use `java.util.Stream::count` method for counting.

### Opening a database for read-only access
You can use `ReadOnlyDatase` class instead of `Database` class to get a read-only access to your database.
Actually `Database` class extends `ReadOnlyDatase` class by adding writing functionality.

#### Reading database from a ZIP file
After you have collected interesting data with Eternal DB you can decide to compress the whole database directory to a ZIP file.
Later on you can get a read-only access to such ZIP-compressed database as follows:
```java
ReadOnlyDatabase roDb = ReadOnlyDatabase.fromZip(Path.of("your/db.zip"));
```

### Closing a database
After all interactions with the database are done, for example at the end of your program, 
you should close the database object:
```java
db.close();
```
You don't need to close a database before the end of your program. It's a lightweight object
and has rather small memory usage.

### Customization of JSON serialization/deserialization
The library provides a default class for serlialization/deserialization strategy: `io.github.k_tomaszewski.eternaldb.JacksonSerialization`.
It uses its own instance of Jackson ObjectMapper (precisely: JsonMapper). One can add customization by
creating this object and passing `Consumer<ObjectMapper>` to the constructor. Example:
```java
Consumer<ObjectMapper> customizer = objectMapper -> objectMapper.registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

Database<MyRecord> db = new Database<>(new DatabaseProperties<>(Path.of("/home/db"), 100)
        .setSerialization(new JacksonSerialization(customizer)));
```

## Contributions
Contributions are welcome. If you want to contribute, just make a pull request. Please contact me before to discuss your idea:
krzysztof.tomaszewski (at) gmail.com
