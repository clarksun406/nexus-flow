package com.nexusflow.api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyHasherTest {

    private final ApiKeyHasher hasher = new ApiKeyHasher();

    @Test
    void hashReturnsSha256Hex() {
        assertThat(hasher.hash("merchant-key-1"))
                .isEqualTo("d93a4e438e34e766e10af96c7e092aa5130875e0b361ee3c3236009ae9bcaf0e");
    }
}
