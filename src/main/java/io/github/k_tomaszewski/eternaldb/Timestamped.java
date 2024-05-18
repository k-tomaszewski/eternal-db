package io.github.k_tomaszewski.eternaldb;

public record Timestamped<T>(T record, long millis) {
}
