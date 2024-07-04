package io.github.k_tomaszewski.eternaldb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAdder;

public class DiskSpaceReclaimerTest {

    @Test
    void shouldDeleteFile() throws IOException {
        // given
        Path filePath = Files.createTempFile("test-file", ".txt");
        Files.write(filePath, "ABC".getBytes(StandardCharsets.UTF_8));

        // when
        double reclaimedMB = DiskSpaceReclaimer.remove(filePath);

        // then
        Assertions.assertFalse(Files.exists(filePath));
        Assertions.assertTrue(reclaimedMB > 0.0);
    }

    @Test
    void shouldDeleteEmptyDirectory() throws IOException {
        // given
        Path dirPath = Files.createTempDirectory("test-dir");

        // when
        double reclaimedMB = DiskSpaceReclaimer.remove(dirPath);

        // then
        Assertions.assertFalse(Files.exists(dirPath));
        Assertions.assertTrue(reclaimedMB > 0.0);
    }

    @Test
    void shouldDeleteFilesOnReclaiming() throws IOException {
        // given
        final Path dataDir = Path.of("target/test_db_" + UUID.randomUUID());
        DiskSpaceReclaimer reclaimer = new DiskSpaceReclaimer(dataDir, 1.0, new AtomicBoolean(), new DoubleAdder());

        // when
        try (Database<Object> db = new Database<>(dataDir, 1, new JacksonSerialization(), new BasicFileNaming())) {
            db.write(Map.of("foo", 1, "bar", 2), System.currentTimeMillis());
        }
        reclaimer.run();

        // then
        Assertions.assertEquals(0, Files.list(dataDir).filter(Files::isRegularFile).count());
    }
}
