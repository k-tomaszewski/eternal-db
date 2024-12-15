package io.github.k_tomaszewski.eternaldb;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.function.Consumer;

public class JacksonSerialization implements SerializationStrategy {

    private final ObjectMapper objectMapper;

    public JacksonSerialization() {
        this(null);
    }

    public JacksonSerialization(Consumer<ObjectMapper> objectMapperCustomizer) {
        objectMapper = new JsonMapper();
        if (objectMapperCustomizer != null) {
            objectMapperCustomizer.accept(objectMapper);
        }
        objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    @Override
    public void serialize(Object obj, Writer writer) {
        try {
            objectMapper.writeValue(writer, obj);
        } catch (IOException e) {
            throw new UncheckedIOException("Record serialization or write to file failed", e);
        }
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Record deserialization failed", e);
        }
    }
}
