package io.github.k_tomaszewski.eternaldb;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

public class JacksonSerializationTest {

    @Test
    void shouldSerializeObjectWithZonedDateTimeWhenCustomizedProperly() {
        // given
        Consumer<ObjectMapper> customizer = objectMapper -> objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .registerModule(new JavaTimeModule());

        var serialization = new JacksonSerialization(customizer);
        StringWriter writer = new StringWriter();
        ZonedDateTime zdt = ZonedDateTime.of(2024, 12, 15, 13, 55, 59, 333123, ZoneId.of("CET"));

        // when
        serialization.serialize(new SomeRecord(zdt), writer);
        SomeRecord record = serialization.deserialize(writer.toString(), SomeRecord.class);

        // then
        Assertions.assertEquals(zdt, record.time());
    }

    private record SomeRecord(ZonedDateTime time) {
    }
}
