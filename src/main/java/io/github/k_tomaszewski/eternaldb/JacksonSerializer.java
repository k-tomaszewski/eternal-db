package io.github.k_tomaszewski.eternaldb;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.Writer;
import java.util.function.BiConsumer;

public class JacksonSerializer implements BiConsumer<Object, Writer> {

    private final ObjectMapper objectMapper;

    public JacksonSerializer() {
        objectMapper = new JsonMapper()
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    @Override
    public void accept(Object object, Writer writer) {
        try {
            objectMapper.writeValue(writer, object);
        } catch (IOException e) {
            throw new RuntimeException("Record serialization or write to file failed", e);
        }
    }
}
