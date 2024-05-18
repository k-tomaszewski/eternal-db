package io.github.k_tomaszewski.eternaldb;

import java.io.Writer;

public interface SerializationStrategy {

    /**
     * This must serialize `obj` into a single text line.
     */
    void serialize(Object obj, Writer writer);

    <T> T deserialize(String json, Class<T> type);
}
