package io.github.k_tomaszewski.eternaldb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class ReadOnlyDatabaseTest {

    @Test
    void shouldWorkWithEmptyDatabase() throws IOException {
        // given
        final Path dataDir = Path.of("target/test_db_" + UUID.randomUUID());
        var config = new DatabaseProperties<>().setDataDir(dataDir);

        // when
        long recordCount;
        try (var db = new ReadOnlyDatabase(config)) {
            recordCount = db.read(Object.class, null, null).count();
        }

        // then
        Assertions.assertEquals(0L, recordCount);
    }

    @Test
    void shouldThrowWhenDirectoryDoesNotExistAndCreateDirsIsFalse() {
        // given
        final Path dataDir = Path.of("target/test_db_" + UUID.randomUUID());
        var config = new DatabaseProperties<>().setDataDir(dataDir)
                .setCreateDirs(false);

        // when
        Executable testCase = () -> {
            try(var db = new ReadOnlyDatabase(config)) {
            }
        };

        // then
        Assertions.assertThrows(RuntimeException.class, testCase);
    }

    @Test
    void shouldReadRecordsWrittenUsingDatabase() {
        // given
        final Path dataDir = Path.of("target/test_db_" + UUID.randomUUID());
        var config = new DatabaseProperties<>().setDataDir(dataDir);
        List<String> expectedRecords = List.of("aaaa", "bbbb", "cccc");

        // when
        try (var db = new Database<>(config)) {
            expectedRecords.forEach(data -> db.write(data, System.currentTimeMillis()));
        }
        List<String> records;
        try (var roDb = new ReadOnlyDatabase(config)) {
            records = roDb.readRecords(String.class, null, null).toList();
        }

        // then
        Assertions.assertEquals(expectedRecords, records);
    }
}
