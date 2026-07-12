package com.nexusflow.api.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserIdMapperTest {

    @Test
    void deterministicUuidFromMerchantId() {
        UUID first = UserIdMapper.toUuid("merchant-1");
        UUID second = UserIdMapper.toUuid("merchant-1");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentMerchantIdsProduceDifferentUuids() {
        UUID a = UserIdMapper.toUuid("merchant-1");
        UUID b = UserIdMapper.toUuid("merchant-2");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toUuidReturnsValidUuid() {
        UUID uuid = UserIdMapper.toUuid("any-merchant");
        assertThat(uuid).isNotNull();
        assertEquals(36, uuid.toString().length());
    }

    @Test
    void opsUserIdIsFixed() {
        assertThat(UserIdMapper.OPS_USER_ID)
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }
}
