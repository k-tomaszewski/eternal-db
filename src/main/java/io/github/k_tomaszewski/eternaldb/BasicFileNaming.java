package io.github.k_tomaszewski.eternaldb;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class BasicFileNaming implements FileNamingStrategy {

    /**
     * Mode controls how granular are files created by eternal-db.
     */
    public enum Mode {

        /**
         * This mode is better for databases collecting data with higher speed.
         */
        HOURLY("yyyy-MM-dd_HH00"),

        /**
         * This mode is better for databases collecting data with lower speed, like once every minute.
         */
        DAILY("yyyy-MM-dd");

        private final DateTimeFormatter formatter;

        Mode(String dateTimeFormatStr) {
            formatter = DateTimeFormatter.ofPattern(dateTimeFormatStr);
        }
    }

    static final String PATH_TEMPLATE = "%d/%02d/%s.data";

    private final Mode mode;

    public BasicFileNaming(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "BasicFileNaming mode must be not null");
    }

    public BasicFileNaming() {
        this(Mode.HOURLY);
    }

    @Override
    public String formatRelativePathStr(long recordMillis) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(recordMillis / 1000, 0, ZoneOffset.UTC);
        return PATH_TEMPLATE.formatted(dateTime.getYear(), dateTime.getMonth().getValue(), dateTime.format(mode.formatter));
    }

    @Override
    public int maxDirectoryDepth() {
        return 3;                       // basically 1 + number of "/" chars in FILENAME_TEMPLATE
    }
}
