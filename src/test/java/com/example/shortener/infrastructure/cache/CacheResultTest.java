package com.example.shortener.infrastructure.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheResultTest {
    @Test
    void exposesMissWithoutAValue() {
        CacheResult<String> result = CacheResult.miss();
        assertThat(result.isMiss()).isTrue();
        assertThat(result.isHit()).isFalse();
        assertThat(result.isNegativeHit()).isFalse();
        assertThat(result.getValue()).isNull();
    }
}
