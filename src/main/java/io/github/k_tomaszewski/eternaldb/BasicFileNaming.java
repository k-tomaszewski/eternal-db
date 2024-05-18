package io.github.k_tomaszewski.eternaldb;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class BasicFileNaming implements FileNamingStrategy {

    static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH");
    static final String FILENAME_TEMPLATE = "%d/%02d/%s00.data";

    @Override
    public String formatRelativePathStr(long recordMillis) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(recordMillis / 1000, 1000 * (int) (recordMillis % 1000), ZoneOffset.UTC);
        return FILENAME_TEMPLATE.formatted(dateTime.getYear(), dateTime.getMonth().getValue(), dateTime.format(FILENAME_FORMATTER));
    }

    @Override
    public int maxDirectoryDepth() {
        return 3;                       // basically 1 + number of "/" chars in FILENAME_TEMPLATE
    }
}
