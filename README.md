# eternal-db
Eternal DB is an embedded time series data base/data store with data retention policy based on a disk space, thus allowing to collect data eternally.

NOTE: This project is a work-in-progress.

## Project goals
1. Embedded data base/data store. This is not meant to run as a stand alone server enabling multiple clients to connect and use.
2. Disk space based data retention policy. This is meant to keep as much data as possible based on a disk space you defined to be used as a limit.
3. Document database, without a need for a fixed data schema.
4. A record always has a timestamp and time is an indexed attribute.
5. Data are persisted as a disk files in directory tree that is representing periods of time.
6. Data are kept in text files, in JSON-in-lines format, so they can be easily accessed by other tools.
7. The project is developed in Java 21.
8. The project is meant to run in Linux environment.
