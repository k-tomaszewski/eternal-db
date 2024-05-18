package io.github.k_tomaszewski.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {

    private static final Logger LOG = LoggerFactory.getLogger(StreamUtil.class);

    public static <T> Stream<T> stream(Spliterator<T> spliterator, boolean parallel) {
        var stream = StreamSupport.stream(spliterator, parallel);
        if (spliterator instanceof AutoCloseable closeableSpliterator) {
            stream = stream.onClose(() -> closeSafely(closeableSpliterator));
        }
        return stream;
    }

    public static void closeSafely(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOG.warn("Resource closing failure", e);
            }
        }
    }
}
