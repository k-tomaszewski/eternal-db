package io.github.k_tomaszewski.util;

public record FileSystemInfo(
        /**
         * In other words: file system. Eg. /dev/sda1
         */
        String volume,

        /**
         * File system type. Eg. ext4
         */
        String type,

        /**
         * Free disk space for this file system, given in megabytes.
         */
        float freeSpaceMB) {
}
