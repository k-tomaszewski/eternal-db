package io.github.k_tomaszewski.eternaldb;

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
    private final long startMills;
    private final IntUnaryOperator diskUsageCheckDelayFunction;
    private long lastUseMillis;
    private long lastDiskUsageKB;
    private int timesToDiskUsageCheck;

    FileContext(Path path, long startMills, IntUnaryOperator diskUsageCheckDelayFunction) throws IOException {
        this.path = path;
        this.fileWriter = Files.newBufferedWriter(path, UTF_8, APPEND, CREATE);
        this.startMills = startMills;
        this.diskUsageCheckDelayFunction = diskUsageCheckDelayFunction;
        lastUseMillis = System.currentTimeMillis();
        lastDiskUsageKB = DiskUsageUtil.getDiskUsageKB(path.toString());
        timesToDiskUsageCheck = diskUsageCheckDelayFunction.applyAsInt(0);
        LOG.trace("File {} opened with initial disk usage of {} KB", path, lastDiskUsageKB);
    }

    BufferedWriter getFileWriter() {
        return fileWriter;
    }

    long getStartMills() {
        return startMills;
    }

    double calculateFileGrowthMB() {
        timesToDiskUsageCheck = diskUsageCheckDelayFunction.applyAsInt(timesToDiskUsageCheck);
        if (timesToDiskUsageCheck == 0) {
            long currentDiskUsageKB = DiskUsageUtil.getDiskUsageKB(path.toString());
            double growth = (currentDiskUsageKB - lastDiskUsageKB) / 1024.0;
            lastDiskUsageKB = currentDiskUsageKB;
            return growth;
        }
        return 0.0;
    }
}
