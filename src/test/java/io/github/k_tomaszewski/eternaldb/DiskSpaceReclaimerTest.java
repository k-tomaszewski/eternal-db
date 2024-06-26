package io.github.k_tomaszewski.eternaldb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiskSpaceReclaimerTest {

    @Test
    void shouldDeleteFile() throws IOException {
        // given
        Path filePath = Files.createTempFile("test-file", ".txt");
        System.out.println("Test file crated to test removing: %s".formatted(filePath));
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
}
