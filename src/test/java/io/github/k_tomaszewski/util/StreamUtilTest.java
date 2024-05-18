package io.github.k_tomaszewski.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamUtilTest {

    @Test
    void shouldCreateStreamProxy() {
        // given
        List<String> data = List.of("a", "b", "c");
        String result;

        // when
        try (Stream<String> stream = StreamUtil.stream(data.spliterator(), false)) {
            result = stream.collect(Collectors.joining());
        }

        // then
        Assertions.assertEquals("abc", result);
    }

    @Test
    void shouldCloseSpliteratorForClosableStream() throws Exception {
        // given
        Spliterator<String> closeableSpliteratorMock = Mockito.mock(Spliterator.class,
                Mockito.withSettings().extraInterfaces(AutoCloseable.class));

        // when
        try (Stream<String> stream = StreamUtil.stream(closeableSpliteratorMock, false)) {
        }

        // then
        Mockito.verify((AutoCloseable) closeableSpliteratorMock).close();
    }
}
