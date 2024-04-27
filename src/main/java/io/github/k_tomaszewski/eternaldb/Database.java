package io.github.k_tomaszewski.eternaldb;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.BiConsumer;
import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;


public class Database implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(Database.class);
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH");
    private static final Pattern FILENAME_REGEX = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})(\\d{2})");
    private static final int WRITES_TO_CHECK_DISK_USAGE = 10;
    private static final int DEFAULT_BLOCK_SIZE = 4096;

    private final Path dataDir;

    // megabytes
    private final long diskUsageLimit;

    // megabytes
    private final DoubleAdder diskUsageActual = new DoubleAdder();

    // bytes
    private final long diskBlockSize;

    // must serialize Object to text without using new line sequence and append it to Writer
    private final BiConsumer<Object, Writer> recordSerializer;

    private final ConcurrentMap<String, FileContext> fileWriters = new ConcurrentHashMap<>();
    private final IntUnaryOperator diskUsageCheckDelayFunction = (n) -> (n > 0) ? --n : WRITES_TO_CHECK_DISK_USAGE;
    private final AtomicReference<DiskSpaceReclaimer> diskSpaceReclaimerRef = new AtomicReference<>();


    public Database(Path dataDir, long diskUsageLimit, BiConsumer<Object, Writer> recordSerializer) {
        this.dataDir = validate(dataDir);
        this.diskUsageLimit = diskUsageLimit;
        this.recordSerializer = recordSerializer;
        diskUsageActual.add(DiskUsageUtil.getDiskUsageMB(dataDir.toString()));
        var fileStoreOpt = getFileStore();
        diskBlockSize = getBlockSize(fileStoreOpt);

        LOG.info("Data directory: '{}'. Disk usage limit: {} MB. Disk usage: {} MB. File store type: {}. Block size: {} B.",
                dataDir, diskUsageLimit, getActualDiskUsageMB(), fileStoreOpt.map(FileStore::type).orElse("?"),
                diskBlockSize);
    }

    private long getBlockSize(Optional<FileStore> fileStore) {
        try {
            return fileStore.orElseThrow().getBlockSize();
        } catch (Exception e) {
            LOG.warn("Cannot establish disk block size: {}. Assuming default of {}.", e.getMessage(),
                    DEFAULT_BLOCK_SIZE);
            return DEFAULT_BLOCK_SIZE;
        }
    }

    public double getActualDiskUsageMB() {
        return diskUsageActual.doubleValue();
    }

    public void write(Object record, long recordMillis) {
        try {
            FileContext context = getFileContext(recordMillis);
            synchronized (context) {
                BufferedWriter fileWriter = context.getFileWriter();
                fileWriter.append(Long.toString(recordMillis - context.getStartMills())).append('\t');
                recordSerializer.accept(record, fileWriter);
                fileWriter.newLine();

                onDiskUsageChange(context.calculateFileGrowthMB());
            }
        } catch (Exception e) {
            throw new RuntimeException("Database write failed", e);
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
    }

    /**
     * TODO: Here is a good place to use a strategy for deciding when perform deleting old data files.
     */
    private void onDiskUsageChange(double change) {
        if (change != 0.0) {
            diskUsageActual.add(change);

            final double leftDiskSpace = diskUsageLimit - diskUsageActual.sum();
            final double minDiskSpace = getMinDiskSpace();
            if (leftDiskSpace < minDiskSpace) {
                LOG.info("Left disk space below minimum: {} MB.", leftDiskSpace);
                diskSpaceReclaimerRef.updateAndGet(existing -> createNewIfNull(existing, minDiskSpace)).start();
            }
        }
    }

    // returns minimal free disk space in megabytes
    private double getMinDiskSpace() {
        if (diskUsageLimit <= 10) {
            return 10.0 * diskBlockSize / (1024.0 * 1024.0);
        }
        if (diskUsageLimit <= 100) {
            return diskUsageLimit / 100.0;
        }
        return 1.0;
    }

    private DiskSpaceReclaimer createNewIfNull(DiskSpaceReclaimer existing, double minDiskSpace) {
        return (existing != null) ? existing : new DiskSpaceReclaimer(dataDir, minDiskSpace);
    }

    static String relativePathStr(long recordMillis) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(recordMillis / 1000, 1000 * (int) (recordMillis % 1000),
                ZoneOffset.UTC);
        return dateTime.getYear() + "/" + dateTime.format(FILENAME_FORMATTER) + "00.data";
    }

    static long parseMillisFromName(String fileName) {
        var matcher = FILENAME_REGEX.matcher(fileName);
        if (matcher.find()) {
            try {
                return LocalDateTime.of(parseInt(matcher.group(1)), parseInt(matcher.group(2)),
                        parseInt(matcher.group(3)), parseInt(matcher.group(4)), parseInt(matcher.group(5)))
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli();
            } catch (Exception e) {
                throw new IllegalArgumentException("Path %s doesn't contain date".formatted(fileName), e);
            }
        }
        throw new IllegalArgumentException("Path %s doesn't match to pattern".formatted(fileName));
    }

    private FileContext getFileContext(long recordMillis) {
        return fileWriters.compute(relativePathStr(recordMillis), (relativeFilePath, context) -> {
            if (context == null) {
                context = createFileContext(relativeFilePath);
            }
            return context;
        });
    }

    private FileContext createFileContext(String relativeFilePath) {
        Path dataFilePath = dataDir.resolve(relativeFilePath);
        long startMillis = parseMillisFromName(dataFilePath.getFileName().toString());
        try {
            long fileSize = 0;
            if (Files.exists(dataFilePath)) {
                fileSize = Files.size(dataFilePath);
            } else {
                Path parentDir = dataFilePath.getParent();
                if (!Files.exists(parentDir)) {
                    LOG.info("Creating data subdirectory {}...", parentDir);
                    Files.createDirectories(parentDir);
                }
            }
            return new FileContext(dataFilePath, startMillis, diskUsageCheckDelayFunction);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open data file %s".formatted(dataFilePath), e);
        }
    }

    private static Path validate(Path dir) {
        Objects.requireNonNull(dir, "Data directory cannot be null");
        if (!Files.exists(dir)) {
            LOG.info("Data directory '{}' doesn't exist. Creating...", dir);
            try {
                dir = Files.createDirectories(dir);
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
