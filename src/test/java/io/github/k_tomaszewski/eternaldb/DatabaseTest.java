package io.github.k_tomaszewski.eternaldb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatabaseTest {

    @Test
    void shouldCreateDatabase() {
        // given
        final Path dataDir = Path.of("target/test_db");

        // when
        Database<Object> db = new Database<>(new DatabaseProperties<>(dataDir, 1));
        db.close();

        // then no exception
    }

    @Test
    void shouldWriteRecordToDatabase() {
        // given
        final Path dataDir = Path.of("target/test_db_2");
        Database<Object> db = new Database<>(new DatabaseProperties<>(dataDir, 1));

        // when
        db.write(Map.of("foo", "bar"), System.currentTimeMillis());
        db.close();

        // then no exception
    }

    @Test
    void shouldReadWrittenRecord() {
        // given
        final Path dataDir = Path.of("target/test_db_" + UUID.randomUUID());
        Database<Object> db = new Database<>(new DatabaseProperties<>(dataDir, 1));

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
        Database<TestEntity> db = new Database<>(new DatabaseProperties<>(dataDir, 1));

        // when
        for (int i = 0; i < 100; ++i) {
            db.write(new TestEntity(i, "ABCDEFGHIJ0123456789ABCDEFGHIJ0123456789"), System.currentTimeMillis());
        }
        db.close();

        // then?
    }

    @Test
    void shouldWriteRecordWithUseOfTimeStampSupplier() {
        // given
        final Path dataDir = Path.of("target/test_db_" + UUID.randomUUID());
        Database<TestEntity> db = new Database<>(new DatabaseProperties<TestEntity>(dataDir, 1).setTimestampSupplier(TestEntity::ts));
        final TestEntity recordToWrite = new TestEntity(1, "a");

        // when
        db.write(recordToWrite);
        var records = db.read(TestEntity.class, null, null).map(Timestamped::record).toList();
        db.close();

        // then
        Assertions.assertEquals(1, records.size());
        Assertions.assertTrue(records.stream().allMatch(x -> x.number() == 1 && x.text().equals("a") && x.ts() == recordToWrite.ts()));
    }

    @Test
    void shouldNotPurgeFileWritersWhenNoFileWritersPresent() {
        // given
        final Path dataDir = Path.of("target/test_db");

        // when
        Database<Object> db = new Database<>(new DatabaseProperties<>(dataDir, 1));
        int count = db.purgeFileWriters();
        db.close();

        // then
        Assertions.assertEquals(0, count);
    }

    @Test
    void shouldNotPurgeFileWritersWhenNoFileWritersAreIdle() {
        // given
        final Path dataDir = Path.of("target/test_db");

        // when
        Database<Object> db = new Database<>(new DatabaseProperties<>(dataDir, 1));
        db.write("abc", System.currentTimeMillis());
        int count = db.purgeFileWriters();
        db.close();

        // then
        Assertions.assertEquals(0, count);
    }

    @Test
    void shouldPurgeFileWritersWhenTheyAreIdle() throws InterruptedException {
        // given
        final Path dataDir = Path.of("target/test_db");

        // when
        Database<Object> db = new Database<>(new DatabaseProperties<>(dataDir, 1).setFileMaxIdleTime(Duration.ofSeconds(1)));
        db.write("abc", System.currentTimeMillis());
        Thread.sleep(2000);
        int count = db.purgeFileWriters();
        db.close();

        // then
        Assertions.assertEquals(1, count);
    }

    record TestEntity (int number, String text, long ts) {
        TestEntity(int number, String text) {
            this(number, text, System.currentTimeMillis());
        }
    }
}
