# eternal-db
Eternal DB is an embedded time series database/data store with a data retention policy based on a disk space, 
thus allowing to collect data eternally.

NOTE: This project is a work-in-progress (WIP).

## Project goals
1. It is an embedded database (data store) that is easy to add to a Java-based program.
2. It has a disk space based data retention policy. This is meant to keep as much data as possible based on a disk space limit that you defined.
3. When reclaiming disk space, oldest data records are removed.
4. Records are kept in a text format in files on a given filesystem and these files are easily readable by other programs.
5. Records are considered documents, like in document databases.
6. A record always has a timestamp and time is considered an indexed attribute.
7. It is meant to run in a common Linux environment.
8. It is thread-safe.

## Non-goals
1. This is not meant to run as a stand alone server enabling multiple clients to connect and use.
2. Ability to configure other data retention policy is not required.
3. Support for any other operating system is not requred if such an operating system cannot provide Linux-compatible runtime environment.
4. Strict schema validation for data is not a goal.
5. Strict transactions support is not a goal.
6. Data access control is not a goal. A user is responsible to configure desired access rights to a directory defined for data files.
7. Valid use of a single database storage from many database instances is not a goal.

## Design decisions
1. Project is developed in Java 21.
2. Records are kept formated as single line JSON, thus a single data file has JSON Lines format: https://jsonlines.org/
3. Data are persisted as a disk files in directory tree that is representing periods of time.
4. Project is avoiding unnecessary dependencies. No Vavr, Project Reactor, Guava, etc.
5. The database should be easy to use in Spring Boot based application.
