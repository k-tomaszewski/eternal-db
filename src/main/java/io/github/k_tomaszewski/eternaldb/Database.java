package io.github.k_tomaszewski.eternaldb;

import io.github.k_tomaszewski.util.DiskUsageUtil;
import io.github.k_tomaszewski.util.StreamUtil;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.IntUnaryOperator;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

public class Database<T> implements Closeable {

    public static final char NEW_LINE_CHAR = '\n';

    private static final Logger LOG = LoggerFactory.getLogger(Database.class);
    private static final int WRITES_TO_CHECK_DISK_USAGE = 10;
    private static final int DEFAULT_BLOCK_SIZE = 4096;
    private static final int TIMESTAMP_RADIX = 32;

    private final Path dataDir;

    // megabytes
    private final long diskUsageLimit;

    // megabytes
    private final DoubleAdder diskUsageActual = new DoubleAdder();

    // TODO migrate space keeping from MBs to block count
    // bytes
    private final long diskBlockSize;

    private final ConcurrentMap<String, FileContext> fileWriters = new ConcurrentHashMap<>();
    private final IntUnaryOperator diskUsageCheckDelayFunction = (n) -> (n > 0) ? --n : WRITES_TO_CHECK_DISK_USAGE;
    private final AtomicBoolean diskSpaceReclaiming = new AtomicBoolean(false);
    private final FileNamingStrategy fileNaming;
    private final SerializationStrategy serialization;
    private final ToLongFunction<T> timestampSupplier;

    public Database(DatabaseProperties<T> config) {
        this.dataDir = validate(config.getDataDir());
        this.diskUsageLimit = config.getDiskUsageLimit();
        this.fileNaming = config.getFileNaming();
        this.serialization = config.getSerialization();
        this.timestampSupplier = config.getTimestampSupplier();

        diskUsageActual.add(DiskUsageUtil.getDiskUsageMB(dataDir.toString()));
        var fileStoreOpt = getFileStore();
        diskBlockSize = getBlockSize(fileStoreOpt);

        LOG.info("Data directory: '{}'. Disk usage limit: {} MB. Disk usage: {} MB. File store type: {}. Block size: {} B.",
                dataDir, diskUsageLimit, getActualDiskUsageMB(), fileStoreOpt.map(FileStore::type).orElse("?"),
                diskBlockSize);
    }

    public double getActualDiskUsageMB() {
        return diskUsageActual.doubleValue();
    }

    public void write(T record) {
        write(record, timestampSupplier.applyAsLong(record));
    }

    public void write(T record, long recordMillis) {
        double fileGrowthMB;
        try {
            FileContext context = getFileContext(recordMillis);
            synchronized (context) {
                BufferedWriter fileWriter = context.getFileWriter();
                fileWriter.append(Long.toString(recordMillis, TIMESTAMP_RADIX)).append('\t');
                serialization.serialize(record, fileWriter);
                fileWriter.append(NEW_LINE_CHAR);
                fileGrowthMB = context.calculateFileGrowthMB();
            }
        } catch (Exception e) {
            throw new RuntimeException("Database write failed", e);
        }
        onDiskUsageChange(fileGrowthMB);
    }

    public <U> Stream<Timestamped<U>> read(Class<U> type, Long minMillis, Long maxMillis) {
        try {
            var spliterator = new FileLinesSpliterator(dataDir, minMillis, maxMillis, fileNaming);
            return filter(StreamUtil.stream(spliterator, false), minMillis, maxMillis)
                    .map(line -> readRecordLine(line, type, spliterator))
                    .filter(Objects::nonNull);
        } catch (IOException e) {
            throw new RuntimeException("Database read failed.", e);
        }
    }

    @Override
    public void close() {
        fileWriters.values().forEach(context -> {
            synchronized (context) {
                try {
                    context.getFileWriter().close();
                } catch (IOException e) {
                    LOG.warn("Closing one of db files failed.", e);
                }
            }
        });
        fileWriters.clear();
        LOG.info("Closed database for directory '{}'.", dataDir);
    }

    private static Stream<String> filter(Stream<String> lineStream, Long minMillis, Long maxMillis) {
        if (minMillis != null || maxMillis != null) {
            lineStream = lineStream.filter(line -> {
                long recordMillis = Long.parseLong(line, 0, line.indexOf('\t'), TIMESTAMP_RADIX);
                return (minMillis == null || minMillis <= recordMillis) && (maxMillis == null || maxMillis >= recordMillis);
            });
        }
        return lineStream;
    }

    private <U> Timestamped<U> readRecordLine(String line, Class<U> type, ReadContext ctx) {
        try {
            int tabPos = line.indexOf('\t');
            return new Timestamped<>(serialization.deserialize(line.substring(tabPos + 1), type),
                    Long.parseLong(line, 0, tabPos, TIMESTAMP_RADIX));
        } catch (RuntimeException e) {
            LOG.warn("Record reading failed (file: {}). Line: `{}`", ctx.getCurrentPath(), line, e);
            return null;
        }
    }

    /**
     * TODO: Here is a good place to use a strategy for deciding when to perform deleting old data files.
     * TODO: Disk space to reclaim could be adjusted by last dynamics of data writing.
     */
    private void onDiskUsageChange(double change) {
        if (change != 0.0) {
            try {
                diskUsageActual.add(change);
                final double leftDiskSpace = diskUsageLimit - diskUsageActual.sum();
                final double minDiskSpace = getMinDiskSpace();
                if (leftDiskSpace < minDiskSpace) {
                    LOG.info("Left disk space below minimum: {} MB.", leftDiskSpace);
                    if (diskSpaceReclaiming.compareAndSet(false, true)) {
                        try {
                            Thread.ofVirtual().name("etdb-reclaim")
                                    .start(new DiskSpaceReclaimer(dataDir, minDiskSpace - leftDiskSpace, diskSpaceReclaiming,
                                            diskUsageActual));
                        } catch (RuntimeException e) {
                            diskSpaceReclaiming.compareAndSet(true, false);
                            throw e;
                        }
                    }
                }
            } catch (RuntimeException e) {
                LOG.warn("Handling disk usage change failed.", e);
            }
        }
    }

    private static long getBlockSize(Optional<FileStore> fileStore) {
        try {
            return fileStore.orElseThrow().getBlockSize();
        } catch (Exception e) {
            LOG.warn("Cannot establish disk block size: {}. Assuming default of {}.", e.getMessage(), DEFAULT_BLOCK_SIZE);
            return DEFAULT_BLOCK_SIZE;
        }
    }

    // returns minimal disk space in megabytes, that we try to keep free
    private double getMinDiskSpace() {
        if (diskUsageLimit <= 10) {
            return 10.0 * diskBlockSize / (1024.0 * 1024.0);
        }
        if (diskUsageLimit <= 100) {
            return diskUsageLimit / 100.0;
        }
        return 1.0;
    }

    private FileContext getFileContext(long recordMillis) {
        return fileWriters.compute(fileNaming.formatRelativePathStr(recordMillis), (relativeFilePath, context) -> {
            if (context == null) {
                context = createFileContext(relativeFilePath);
            }
            return context;
        });
    }

    private FileContext createFileContext(String relativeFilePath) {
        Path dataFilePath = dataDir.resolve(relativeFilePath);
        try {
            if (!Files.exists(dataFilePath)) {
                Path parentDir = dataFilePath.getParent();
                if (!Files.exists(parentDir)) {
                    LOG.info("Creating data subdirectory {}...", parentDir);
                    Files.createDirectories(parentDir);
                }
            }
            return new FileContext(dataFilePath, diskUsageCheckDelayFunction);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open data file %s".formatted(dataFilePath), e);
        }
    }

    private static Path validate(Path dir) {
        Objects.requireNonNull(dir, "Data directory cannot be null");
        if (!Files.exists(dir)) {
            LOG.info("Data directory '{}' doesn't exist. Creating...", dir);
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create data directory: %s".formatted(dir), e);
            }
        }
        Validate.isTrue(Files.isDirectory(dir), "Path for data directory is not a directory: %s", dir);
        return dir;
    }

    private Optional<FileStore> getFileStore() {
        try {
            return Optional.of(Files.getFileStore(dataDir));
        } catch (Exception e) {
            LOG.warn("Cannot get FileStore for path '{}'", dataDir, e);
            return Optional.empty();
        }
    }
}
