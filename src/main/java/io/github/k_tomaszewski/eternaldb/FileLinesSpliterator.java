package io.github.k_tomaszewski.eternaldb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Spliterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.github.k_tomaszewski.util.StreamUtil.closeSafely;

/**
 * Serves as a base for a stream of raw lines from data files. Files are limited by optional `minMillis` and `maxMillis`, but records
 * provided by this spliterator ARE NOT LIMITED. All records from selected files are provided.
 */
class FileLinesSpliterator implements Spliterator<String>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FileLinesSpliterator.class);
    private static final BiPredicate<Path, BasicFileAttributes> DATA_FILE_PREDICATE = (path, attributes) -> attributes.isRegularFile();

    // TODO opcjonalne ograniczenia OD i DO
    // TODO bieżący kontekst

    final Path dataDir;
    final Stream<Path> pathStream;
    final Spliterator<Path> pathSpliterator;
    final FileNamingStrategy fileNamingStrategy;
    volatile Stream<String> fileLineStream;
    volatile Spliterator<String> fileLineSpliterator;


    public FileLinesSpliterator(Path dataDir, Long minMillis, Long maxMillis, FileNamingStrategy fileNamingStrategy) throws IOException {
        this.dataDir = dataDir;
        pathStream = Files.find(dataDir, fileNamingStrategy.maxDirectoryDepth(), toDataFilePredicate(minMillis, maxMillis));
        pathSpliterator = pathStream.sorted().spliterator();
        this.fileNamingStrategy = fileNamingStrategy;
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> dataLineConsumer) {
        for (;;) {
            while (fileLineSpliterator == null) {
                if (!pathSpliterator.tryAdvance(this::openDataFile)) {
                    return false;
                }
            }
            if (fileLineSpliterator.tryAdvance(dataLineConsumer)) {
                return true;
            } else {
                closeDataFile();
            }
        }
    }

    @Override
    public void close() {
        closeSafely(pathStream);
        closeSafely(fileLineStream);
    }

    @Override
    public Spliterator<String> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return ORDERED | IMMUTABLE | NONNULL;
    }

    private void openDataFile(Path path) {
        try {
            fileLineStream = Files.lines(path, StandardCharsets.UTF_8);         // TODO add handling of compressed data files
            fileLineSpliterator = fileLineStream.sorted().spliterator();
        } catch (IOException e) {
            LOG.warn("Cannot read data from file {}", path, e);
            closeSafely(fileLineStream);
            fileLineStream = null;
            fileLineSpliterator = null;
        }
    }

    private void closeDataFile() {
        closeSafely(fileLineStream);
        fileLineStream = null;
        fileLineSpliterator = null;
    }

    private BiPredicate<Path, BasicFileAttributes> toDataFilePredicate(Long minMillis, Long maxMillis) {
        BiPredicate<Path, BasicFileAttributes> predicate = DATA_FILE_PREDICATE;
        if (minMillis != null || maxMillis != null) {
            Predicate<String> fileNamePredicate = toFileNamePredicate(minMillis, maxMillis, fileNamingStrategy);
            predicate = predicate.and((path, attributes) -> fileNamePredicate.test(dataDir.relativize(path).toString()));
        }
        return predicate;
    }

    static Predicate<String> toFileNamePredicate(Long minMillis, Long maxMillis, FileNamingStrategy fileNamingStrategy) {
        Predicate<String> predicate = null;
        if (minMillis != null) {
            final String minFileName = fileNamingStrategy.formatRelativePathStr(minMillis);
            predicate = (fileName) -> fileName.compareTo(minFileName) >= 0;
        }
        if (maxMillis != null) {
            final String maxFileName = fileNamingStrategy.formatRelativePathStr(maxMillis);
            Predicate<String> predicate2 = (fileName) -> fileName.compareTo(maxFileName) <= 0;
            predicate = (predicate != null) ? predicate.and(predicate2) : predicate2;
        }
        return predicate;
    }
}
