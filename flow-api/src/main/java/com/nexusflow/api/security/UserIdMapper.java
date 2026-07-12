package com.nexusflow.api.security;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Derives deterministic UUIDs from merchant identity strings for use as userId
 * in the permission system. This avoids creating user records for API key auth.
 */
public final class UserIdMapper {

    /** Fixed UUID for the global ops service account (matches V5 seed in flow-permission-server). */
    public static final UUID OPS_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String MERCHANT_SA_PREFIX = "merchant-sa:";

    private UserIdMapper() {
    }

    /**
     * Derives a deterministic UUID v5 from a merchantId string.
     * The same merchantId always produces the same UUID.
     */
    public static UUID toUuid(String merchantId) {
        return UUID.nameUUIDFromBytes((MERCHANT_SA_PREFIX + merchantId).getBytes(StandardCharsets.UTF_8));
    }
}
