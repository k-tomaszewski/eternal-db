package io.github.k_tomaszewski.eternaldb;

import java.time.Duration;
import java.util.Optional;

public interface FileNamingStrategy {

    /**
     * Implementations are required to fulfill following contract:
     * "Lexicographic order of paths returned by this method is the same as order of timestamps used to generate these paths."
     */
    String formatRelativePathStr(long recordMillis);

    int maxDirectoryDepth();

    /**
     * File creation interval, if a strategy has one.
     */
    default Optional<Duration> fileCreationInterval() {
        return Optional.empty();
    }
}
