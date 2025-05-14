package io.github.k_tomaszewski.eternaldb;

import org.apache.commons.lang3.Validate;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.ToLongFunction;

/**
 * All configuration properties that can be applied to a {@link Database}. This class is ready to be used with Spring Boot
 * `@ConfigurationProperties` annotation.
 */
public class DatabaseProperties<T> {

    private Path dataDir;
    private long diskUsageLimit;
    private SerializationStrategy serialization = new JacksonSerialization();
    private FileNamingStrategy fileNaming = new BasicFileNaming();
    private ToLongFunction<T> timestampSupplier = (x) -> {
        throw new UnsupportedOperationException("Timestamp supplier not provided in configuration");
    };
    private Duration fileMaxIdleTime = Duration.ofMinutes(5);
    private BooleanSupplier flushCondition;
    private boolean createDirs = true;

    public DatabaseProperties() {
    }

    /**
     * Parameters of this constructor are the only required properties. All other properties have default values.
     * @param dataDir Root directory for data files.
     * @param diskUsageLimit Limit of disk space used by the databased, in megabytes.
     */
    public DatabaseProperties(Path dataDir, long diskUsageLimit) {
        this.dataDir = dataDir;
        this.diskUsageLimit = diskUsageLimit;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public DatabaseProperties<T> setDataDir(Path dataDir) {
        this.dataDir = dataDir;
        return this;
    }

    public long getDiskUsageLimit() {
        return diskUsageLimit;
    }

    public DatabaseProperties<T> setDiskUsageLimit(long diskUsageLimit) {
        this.diskUsageLimit = diskUsageLimit;
        return this;
    }

    public SerializationStrategy getSerialization() {
        return serialization;
    }

    public DatabaseProperties<T> setSerialization(SerializationStrategy serialization) {
        this.serialization = serialization;
        return this;
    }

    public FileNamingStrategy getFileNaming() {
        return fileNaming;
    }

    public DatabaseProperties<T> setFileNaming(FileNamingStrategy fileNaming) {
        this.fileNaming = fileNaming;
        return this;
    }

    public ToLongFunction<T> getTimestampSupplier() {
        return timestampSupplier;
    }

    public DatabaseProperties<T> setTimestampSupplier(ToLongFunction<T> timestampSupplier) {
        this.timestampSupplier = timestampSupplier;
        return this;
    }

    public Duration getFileMaxIdleTime() {
        return fileMaxIdleTime;
    }

    public DatabaseProperties<T> setFileMaxIdleTime(Duration fileMaxIdleTime) {
        this.fileMaxIdleTime = Objects.requireNonNull(fileMaxIdleTime, "File max idle time must be not null");
        Validate.isTrue(fileMaxIdleTime.toSeconds() >= 1L, "File max idle time must be at least 1 second");
        return this;
    }

    public BooleanSupplier getFlushCondition() {
        return flushCondition;
    }

    /**
     * `flushCondition.getAsBoolean()` must be thread safe.
     */
    public DatabaseProperties<T> setFlushCondition(BooleanSupplier flushCondition) {
        this.flushCondition = flushCondition;
        return this;
    }

    public DatabaseProperties<T> withFlushOnEveryWrite() {
        return setFlushCondition(() -> true);
    }

    public boolean getCreateDirs() {
        return createDirs;
    }

    public DatabaseProperties<T> setCreateDirs(boolean createDirs) {
        this.createDirs = createDirs;
        return this;
    }
}
