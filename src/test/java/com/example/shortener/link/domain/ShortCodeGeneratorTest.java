package com.example.shortener.link.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTest {

    @Test
    void generatesUrlSafeFixedLengthCodes() {
        ShortCodeGenerator generator = new ShortCodeGenerator();
        var generated = new HashSet<String>();
        for (int index = 0; index < 100; index++) {
            String code = generator.generate();
            assertThat(code).hasSize(10).matches("[A-Za-z0-9]{10}");
            generated.add(code);
        }
        assertThat(generated).hasSize(100);
    }
}
