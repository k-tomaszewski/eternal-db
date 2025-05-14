package io.github.k_tomaszewski.eternaldb;

import io.github.k_tomaszewski.util.FileUtils;
import io.github.k_tomaszewski.util.StreamUtil;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import static io.github.k_tomaszewski.util.StreamUtil.closeSafely;

/**
 * Read-only database. There may be many instances of this class, in one or many JVMs, using the same data directory.
 */
public class ReadOnlyDatabase implements Closeable {

    public static final char SEPARATOR = '\t';

    private static final Logger LOG = LoggerFactory.getLogger(ReadOnlyDatabase.class);
    protected static final int TIMESTAMP_RADIX = 32;

    protected final Path dataDir;
    protected final FileNamingStrategy fileNaming;
    protected final SerializationStrategy serialization;
    private final Closeable closeable;

    public ReadOnlyDatabase(DatabaseProperties<?> config) {
        this(config, null);
    }

    public ReadOnlyDatabase(DatabaseProperties<?> config, Closeable closeable) {
        dataDir = prepareDirectory(config.getDataDir(), config.getCreateDirs());
        fileNaming = config.getFileNaming();
        serialization = config.getSerialization();
        this.closeable = closeable;
    }

    /**
     * Uses a default file naming strategy, mainly to detect a database root directory inside provided ZIP file.
     */
    public static ReadOnlyDatabase fromZip(Path zipFilePath) throws IOException {
        return fromZip(zipFilePath, new DatabaseProperties<>().getFileNaming());
    }

    /**
     * Uses the given file naming strategy, mainly to detect a database root directory inside provided ZIP file.
     */
    public static ReadOnlyDatabase fromZip(Path zipFilePath, FileNamingStrategy fileNaming) throws IOException {
        Validate.isTrue(zipFilePath.getFileName().toString().toLowerCase().endsWith(".zip"), "ZIP file without ZIP extention");
        FileSystem zipFs = FileSystems.newFileSystem(zipFilePath);
        try {
            var config = new DatabaseProperties<>()
                    .setDataDir(findDbRootPath(zipFs, fileNaming.maxDirectoryDepth()))
                    .setFileNaming(fileNaming)
                    .setCreateDirs(false);
            return new ReadOnlyDatabase(config, zipFs);
        } catch (Exception e) {
            closeSafely(zipFs);
            throw new RuntimeException("Cannot open database from ZIP file %s".formatted(zipFilePath), e);
        }
    }

    private static Path findDbRootPath(FileSystem zipFs, int maxDirectoryDepth) throws IOException {
        Path zipRootPath = zipFs.getPath("/");
        if (maxDirectoryDepth < 1) {
            return zipRootPath;
        }
        try (var files = Files.find(zipRootPath, Integer.MAX_VALUE, FileUtils.IS_FILE_PREDICATE, FileVisitOption.FOLLOW_LINKS)) {
            Path firstFile = files.findFirst().orElseThrow();
            if (firstFile.getNameCount() > maxDirectoryDepth) {
                return firstFile.subpath(0, firstFile.getNameCount() - maxDirectoryDepth);

            }
            return zipRootPath;
        }
    }

    /**
     * Basic data search.
     * @param type Data type records are deserialized to.
     * @param minMillis Optional (nullable) parameter with the earliest timestamp of data to search.
     * @param maxMillis Optional (nullable) parameter with the latest timestamp of data to search.
     * @return Stream object that must be closed after use.
     */
    public <T> Stream<Timestamped<T>> read(Class<T> type, Long minMillis, Long maxMillis) {
        try {
            var spliterator = new FileLinesSpliterator(dataDir, minMillis, maxMillis, fileNaming);
            return filter(StreamUtil.stream(spliterator, false), minMillis, maxMillis)
                    .map(line -> readRecordLine(line, type, spliterator))
                    .filter(Objects::nonNull);
        } catch (IOException e) {
            throw new RuntimeException("Database read failed.", e);
        }
    }

    /**
     * This is equivalent of calling <code>read(type, minMillis, maxMillis).map(Timestamped::record)</code>.
     * @return Stream object that must be closed after use.
     * @see #read(Class, Long, Long) 
     */
    public <T> Stream<T> readRecords(Class<T> type, Long minMillis, Long maxMillis) {
        return read(type, minMillis, maxMillis).map(Timestamped::record);
    }

    @Override
    public void close() {
        closeSafely(closeable);
    }

    private static Path prepareDirectory(Path dir, boolean createDirs) {
        Objects.requireNonNull(dir, "Data directory cannot be null");
        if (!Files.exists(dir)) {
            if (createDirs) {
                LOG.info("Data directory '{}' doesn't exist. Creating...", dir);
                try {
                    Files.createDirectories(dir);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot create data directory: %s".formatted(dir), e);
                }
            } else {
                throw new RuntimeException("Data directory %s doesn't exist.".formatted(dir));
            }
        }
        Validate.isTrue(Files.isDirectory(dir), "Path for data directory is not a directory: %s", dir);
        return dir;
    }

    private static Stream<String> filter(Stream<String> lineStream, Long minMillis, Long maxMillis) {
        if (minMillis != null || maxMillis != null) {
            lineStream = lineStream.filter(line -> {
                long recordMillis = Long.parseLong(line, 0, line.indexOf(SEPARATOR), TIMESTAMP_RADIX);
                return (minMillis == null || minMillis <= recordMillis) && (maxMillis == null || maxMillis >= recordMillis);
            });
        }
        return lineStream;
    }

    private <T> Timestamped<T> readRecordLine(String line, Class<T> type, ReadContext ctx) {
        try {
            int tabPos = line.indexOf(SEPARATOR);
            return new Timestamped<>(serialization.deserialize(line.substring(tabPos + 1), type),
                    Long.parseLong(line, 0, tabPos, TIMESTAMP_RADIX));
        } catch (RuntimeException e) {
            LOG.warn("Record reading failed (file: {}). Line: `{}`", ctx.getCurrentPath(), line, e);
            return null;
        }
    }
}
