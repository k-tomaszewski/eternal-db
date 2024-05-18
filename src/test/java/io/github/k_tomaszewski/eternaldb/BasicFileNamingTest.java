package io.github.k_tomaszewski.eternaldb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class BasicFileNamingTest {

    private final BasicFileNaming strategy = new BasicFileNaming();

    @Test
    void shouldFormatDataFileNames() {
        Assertions.assertEquals("1970/01/1970-01-01_0000.data", strategy.formatRelativePathStr(0));
        Assertions.assertEquals("2024/03/2024-03-15_1200.data", strategy.formatRelativePathStr(
                LocalDateTime.of(2024, 3, 15, 12, 11, 56).toInstant(ZoneOffset.UTC).toEpochMilli()));
    }
}
