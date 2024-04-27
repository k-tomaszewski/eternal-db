package io.github.k_tomaszewski.eternaldb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public class DatabaseTest {

    @Test
    void shouldFormatDataFileNames() {
        Assertions.assertEquals("1970/1970-01-01_0000.data", Database.relativePathStr(0));
        Assertions.assertEquals("2024/2024-03-15_1200.data", Database.relativePathStr(
                LocalDateTime.of(2024, 3, 15, 12, 11, 56).toInstant(ZoneOffset.UTC).toEpochMilli()));
    }

    @Test
    void shouldGetMillisFromFilename() {
        Assertions.assertEquals(0L, Database.parseMillisFromName("1970-01-01_0000.data"));
        Assertions.assertEquals(LocalDateTime.of(2024, 3, 15, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
                Database.parseMillisFromName("2024-03-15_1200.data"));
    }

    @Test
    void shouldCreateDatabase() {
        // given
        final Path dataDir = Path.of("target/test_db");

        // when
        Database db = new Database(dataDir, 1, (record, writer) -> {});
        db.close();

        // then no exception
    }

    @Test
    void shouldWriteRecordToDatabase() {
        // given
        final Path dataDir = Path.of("target/test_db_2");
        var serializer = new JacksonSerializer();
        Database db = new Database(dataDir, 1, serializer);

        // when
        db.write(Map.of("foo", "bar"), System.currentTimeMillis());
        db.close();

        // then no exception
    }
}
