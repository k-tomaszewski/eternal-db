package io.github.k_tomaszewski.eternaldb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatabaseTest {

    @Test
    void shouldCreateDatabase() {
        // given
        final Path dataDir = Path.of("target/test_db");

        // when
        Database<Object> db = new Database<>(dataDir, 1, new JacksonSerialization(), new BasicFileNaming());
        db.close();

        // then no exception
    }

    @Test
    void shouldWriteRecordToDatabase() {
        // given
        final Path dataDir = Path.of("target/test_db_2");
        Database<Object> db = new Database<>(dataDir, 1, new JacksonSerialization(), new BasicFileNaming());

        // when
        db.write(Map.of("foo", "bar"), System.currentTimeMillis());
        db.close();

        // then no exception
    }

    @Test
    void shouldReadWrittenRecord() {
        // given
        final Path dataDir = Path.of("target/test_db_" + UUID.randomUUID());
        Database<Object> db = new Database<>(dataDir, 1, new JacksonSerialization(), new BasicFileNaming());

        final var obj1 = new TestEntity(123, "abc");
        final var obj2 = new TestEntity(456, "XYZ");

        // when
        db.write(obj1, System.currentTimeMillis());
        db.write(obj2, System.currentTimeMillis());
        List<TestEntity> entities = db.read(TestEntity.class, null, null).map(Timestamped::record).toList();
        db.close();

        // then
        Assertions.assertEquals(2, entities.size());
        Assertions.assertEquals(obj1, entities.getFirst());
        Assertions.assertEquals(obj2, entities.getLast());
    }

    @Test
    void shouldNoticeFileGrowth() {
        // given
        final Path dataDir = Path.of("target/test_db_3");
        Database<TestEntity> db = new Database<>(dataDir, 1, new JacksonSerialization(), new BasicFileNaming());

        // when
        for (int i = 0; i < 100; ++i) {
            db.write(new TestEntity(i, "ABCDEFGHIJ0123456789ABCDEFGHIJ0123456789"), System.currentTimeMillis());
        }
        db.close();

        // then?
    }

    record TestEntity (int number, String text) {
    }
}
