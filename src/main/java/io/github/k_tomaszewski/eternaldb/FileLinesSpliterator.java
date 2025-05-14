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

import static io.github.k_tomaszewski.util.FileUtils.IS_FILE_PREDICATE;
import static io.github.k_tomaszewski.util.StreamUtil.closeSafely;

/**
 * Serves as a base for a stream of raw lines from data files. Files are limited by optional `minMillis` and `maxMillis`, but records
 * provided by this spliterator ARE NOT LIMITED. All records from selected files are provided.
 * NOTE: This is a closeable spliterator. Use {@link io.github.k_tomaszewski.util.StreamUtil#stream(Spliterator, boolean)} to create
 * a Stream object that will close this spliterator. Otherwise, it won't be closed. See: https://bugs.openjdk.org/browse/JDK-8318856
 */
class FileLinesSpliterator implements Spliterator<String>, AutoCloseable, ReadContext {

    private static final Logger LOG = LoggerFactory.getLogger(FileLinesSpliterator.class);

    final Stream<Path> pathStream;
    final Spliterator<Path> pathSpliterator;
    volatile Stream<String> fileLineStream;
    volatile Spliterator<String> fileLineSpliterator;
    volatile Path currentPath;

    public FileLinesSpliterator(Path dataDir, Long minMillis, Long maxMillis, FileNamingStrategy fileNaming) throws IOException {
        pathStream = Files.find(dataDir, fileNaming.maxDirectoryDepth(), toDataFilePredicate(minMillis, maxMillis, fileNaming, dataDir));
        pathSpliterator = pathStream.sorted().spliterator();
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

    @Override
    public Path getCurrentPath() {
        return currentPath;
    }

    private void openDataFile(Path path) {
        try {
            fileLineStream = Files.lines(path, StandardCharsets.UTF_8);         // TODO add handling of compressed data files
            fileLineSpliterator = fileLineStream.sorted().spliterator();
            currentPath = path;
        } catch (IOException e) {
            LOG.warn("Cannot read data from file {}", path, e);
            closeDataFile();
        }
    }

    private void closeDataFile() {
        closeSafely(fileLineStream);
        fileLineStream = null;
        fileLineSpliterator = null;
        currentPath = null;
    }

    private static BiPredicate<Path, BasicFileAttributes> toDataFilePredicate(Long minMillis, Long maxMillis,
            FileNamingStrategy fileNaming, Path dataDir) {
        BiPredicate<Path, BasicFileAttributes> predicate = IS_FILE_PREDICATE;
        if (minMillis != null || maxMillis != null) {
            Predicate<String> fileNamePredicate = toFileNamePredicate(minMillis, maxMillis, fileNaming);
            predicate = predicate.and((path, attributes) -> fileNamePredicate.test(dataDir.relativize(path).toString()));
        }
        return predicate;
    }

    static Predicate<String> toFileNamePredicate(Long minMillis, Long maxMillis, FileNamingStrategy fileNaming) {
        Predicate<String> predicate = null;
        if (minMillis != null) {
            final String minFileName = fileNaming.formatRelativePathStr(minMillis);
            predicate = (fileName) -> fileName.compareTo(minFileName) >= 0;
        }
        if (maxMillis != null) {
            final String maxFileName = fileNaming.formatRelativePathStr(maxMillis);
            Predicate<String> predicate2 = (fileName) -> fileName.compareTo(maxFileName) <= 0;
            predicate = (predicate != null) ? predicate.and(predicate2) : predicate2;
        }
        return predicate;
    }
}
