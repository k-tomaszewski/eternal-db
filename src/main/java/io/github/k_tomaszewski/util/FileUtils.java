package io.github.k_tomaszewski.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    public static boolean isEmptyDir(Path path) {
        try (Stream<Path> dirContentStream = Files.list(path)) {
            return dirContentStream.findAny().isEmpty();
        } catch (Exception e) {
            LOG.warn("Cannot check if directory [{}] is empty.", path, e);
            return false;
        }
    }

    public static boolean exists(Path path, boolean fileExistenceAssumedOnError) {
        try {
            return Files.exists(path);
        } catch (RuntimeException e) {
            LOG.warn("Cannot check if path [{}] exists.", path, e);
            return fileExistenceAssumedOnError;
        }
    }
}
