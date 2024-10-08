# eternal-db
Eternal DB is an embedded time series database/data storage engine with a data retention policy based on a disk space. 
As oldest records are removed when needed to reclaim disk space, this database allows to collect data eternally. It gives unmatched control
on a disk resource usage. Moreover data are easily accessible for any tools as they are kept in regular disk files on a given file system.
It's a schema-less document database as records are just JSON documents.

Some possible use cases:
- collecting measurement data
- collecting metrics
- collecting market data

In above cases the most important thing for a running system is to keep some time window of latest data. Eternal DB allows to express this 
time window in terms of a disk space.

As this is designed to be used under Linux, it requires Coreutils: https://www.gnu.org/software/coreutils/

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
9. It is fault-tolerant. A single error should not block you from reading all your data.

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
2. Records are kept formatted as single line JSON, thus a single data file has a format similar to JSON Lines: https://jsonlines.org/
3. Each line in a data file is beginning with timestamp being number of millis from the Epoch (as given by `System.currentTimeMillis()`)
   and formatted with radix of 32 (to use less disk space), followed by the tab character, followed by JSON.
4. Data are persisted as a disk files in directory tree that is representing periods of time.
5. Project is avoiding unnecessary dependencies. No Vavr, Project Reactor, Guava, etc.
6. The database should be easy to use in Spring Boot based application.

## Contributions
Contributions are welcome. If you want to contribute, just make a pull request. Please contact me before to discuss your idea:
krzysztof.tomaszewski (at) gmail.com
