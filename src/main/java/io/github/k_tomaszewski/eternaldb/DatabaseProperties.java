package io.github.k_tomaszewski.eternaldb;

import java.nio.file.Path;
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
}
