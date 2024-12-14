package io.github.k_tomaszewski.eternaldb;

import io.github.k_tomaszewski.util.StreamUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileLinesSpliteratorTest {

    @Test
    void shouldProvideLinesFromAllFilesKeepingTimeOrdering() throws IOException {
        // given
        final Path dataDir = Path.of("target/spliterator_db_" + System.currentTimeMillis());
        Database<String> db = new Database<>(new DatabaseProperties<>(dataDir, 1));

        final var now = LocalDateTime.now();
        db.write("A", toMillis(now));
        db.write("B", toMillis(now.minusNanos(1000000)));
        db.write("C", toMillis(now.minusDays(1)));
        db.write("D", toMillis(now.minusDays(1).minusNanos(1000000)));
        db.write("E", toMillis(now.minusMonths(1)));
        db.write("F", toMillis(now.minusMonths(1).minusNanos(1000000)));

        // when
        String result;
        try (Stream<String> linesStream = StreamUtil.stream(new FileLinesSpliterator(dataDir, null, null, new BasicFileNaming()), false)) {
            result = linesStream.map(line -> line.substring(line.indexOf('\t') + 2, line.length() - 1))
                    .collect(Collectors.joining());
        }
        db.close();

        // then
        Assertions.assertEquals("FEDCBA", result);
    }

    private static long toMillis(LocalDateTime dt) {
        return dt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
