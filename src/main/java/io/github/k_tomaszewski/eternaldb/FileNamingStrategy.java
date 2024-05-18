package io.github.k_tomaszewski.eternaldb;

public interface FileNamingStrategy {

    String formatRelativePathStr(long recordMillis);

    int maxDirectoryDepth();
}
