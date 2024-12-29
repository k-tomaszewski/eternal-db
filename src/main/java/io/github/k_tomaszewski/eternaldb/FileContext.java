package io.github.k_tomaszewski.eternaldb;

import io.github.k_tomaszewski.util.DiskUsageUtil;
import io.github.k_tomaszewski.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntUnaryOperator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

class FileContext {

    private static final Logger LOG = LoggerFactory.getLogger(FileContext.class);

    private final Path path;
    private final BufferedWriter fileWriter;
    private final IntUnaryOperator diskUsageCheckDelayFunction;
    private long lastUseMillis;
    private long lastDiskUsageKB;
    private int timesToDiskUsageCheck;

    FileContext(Path path, IntUnaryOperator diskUsageCheckDelayFunction) throws IOException {
        this.path = path;
        boolean appendNewLine = FileUtils.isNewLineMissingAtTheEndOfFile(path);
        fileWriter = Files.newBufferedWriter(path, UTF_8, APPEND, CREATE);
        if (appendNewLine) {
            fileWriter.append(Database.NEW_LINE_CHAR);
            LOG.warn("File {} was present and missing a new line at the end (corruption). Some data from previous run may be lost!", path);
        }
        this.diskUsageCheckDelayFunction = diskUsageCheckDelayFunction;
        lastDiskUsageKB = DiskUsageUtil.getDiskUsageKB(path.toString());
        timesToDiskUsageCheck = diskUsageCheckDelayFunction.applyAsInt(0);
        LOG.trace("File {} opened with initial disk usage: {} KB", path, lastDiskUsageKB);
    }

    BufferedWriter getFileWriter() {
        lastUseMillis = System.currentTimeMillis();
        return fileWriter;
    }

    double calculateFileGrowthMB() {
        timesToDiskUsageCheck = diskUsageCheckDelayFunction.applyAsInt(timesToDiskUsageCheck);
        if (timesToDiskUsageCheck == 0) {
            long currentDiskUsageKB = DiskUsageUtil.getDiskUsageKB(path.toString());
            LOG.trace("File {} current disk usage: {} KB", path, currentDiskUsageKB);
            double growthMB = (currentDiskUsageKB - lastDiskUsageKB) / 1024.0;
            lastDiskUsageKB = currentDiskUsageKB;
            return growthMB;
        }
        return 0.0;
    }
}
