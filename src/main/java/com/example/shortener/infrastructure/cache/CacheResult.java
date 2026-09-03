package com.example.shortener.infrastructure.cache;

import java.util.Optional;

public record CacheResult<T>(
    ResultType type,
    Optional<T> value
) {
    public enum ResultType {
        HIT,
        NEGATIVE_HIT,
        MISS
    }

    public static <T> CacheResult<T> hit(T value) {
        return new CacheResult<>(ResultType.HIT, Optional.of(value));
    }

    public static <T> CacheResult<T> negativeHit() {
        return new CacheResult<>(ResultType.NEGATIVE_HIT, Optional.empty());
    }

    public static <T> CacheResult<T> miss() {
        return new CacheResult<>(ResultType.MISS, Optional.empty());
    }

    public boolean isHit() {
        return type == ResultType.HIT;
    }

    public boolean isNegativeHit() {
        return type == ResultType.NEGATIVE_HIT;
    }

    public boolean isMiss() {
        return type == ResultType.MISS;
    }

    public T getValue() {
        return value.orElse(null);
    }
}
