package io.github.k_tomaszewski.util;

import io.github.k_tomaszewski.eternaldb.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
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

    public static boolean isNewLineMissingAtTheEndOfFile(Path path) {
        if (!exists(path, false)) {
            return false;
        }
        try (var file = new RandomAccessFile(path.toFile(), "r")) {
            long fileLen = file.length();
            if (fileLen > 0) {
                file.seek(fileLen - 1);
                return Database.NEW_LINE_CHAR != (char) file.readUnsignedByte();
            }
        } catch (FileNotFoundException e) {
            // nop
        } catch (IOException e) {
            LOG.warn("Cannot verify if already existing file {} has a new line at the end.", path, e);
        }
        return false;
    }
}
