package io.github.k_tomaszewski.util;

import io.github.k_tomaszewski.eternaldb.Database;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtilsTest {

    @Test
    void shouldDetectNewLineAtTheEndOfFile() throws IOException {
        // given
        File file = File.createTempFile("test", ".txt");
        file.deleteOnExit();
        try (var writer = new FileWriter(file)) {
            writer.write("abc" + Database.NEW_LINE_CHAR);
        }
        // when
        boolean result = FileUtils.isNewLineMissingAtTheEndOfFile(file.toPath());
        // then
        Assertions.assertFalse(result);
    }

    @Test
    void shouldDetectMissingNewLineAtTheEndOfFile() throws IOException {
        // given
        File file = File.createTempFile("test2", ".txt");
        file.deleteOnExit();
        try (var writer = new FileWriter(file)) {
            writer.write("abc" + Database.NEW_LINE_CHAR + "xyz");
        }
        // when
        boolean result = FileUtils.isNewLineMissingAtTheEndOfFile(file.toPath());
        // then
        Assertions.assertTrue(result);
    }
}
