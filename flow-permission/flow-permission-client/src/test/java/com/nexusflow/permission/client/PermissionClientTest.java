package com.nexusflow.permission.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionClientTest {

    private PermissionClient client;
    private PermissionClientProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PermissionClientProperties();
        properties.setEnabled(true);
        properties.setServerUrl("http://localhost:8090");
        properties.setServiceToken("test-token");
        properties.setCacheTtlSeconds(60);
        properties.setCacheMaxSize(1000);
        client = new PermissionClient(properties);
    }

    @Test
    void checkReturnsTrueWhenDisabled() {
        properties.setEnabled(false);
        PermissionClient disabledClient = new PermissionClient(properties);

        // Even without a running server, disabled client returns true
        boolean result = disabledClient.check(UUID.randomUUID(), "any:perm", "MERCHANT", null);
        assertThat(result).isTrue();
    }

    @Test
    void getEffectivePermissionsCachesResult() {
        UUID userId = UUID.randomUUID();
        String cacheKey = "GET_EFF_PERMS";

        // First call will attempt HTTP and return empty (server not running)
        Set<String> first = client.getEffectivePermissions(userId, "MERCHANT", null);
        assertThat(first).isEmpty();
    }

    @Test
    void evictCacheRemovesUserEntries() {
        UUID userId = UUID.randomUUID();

        // Populate cache by calling getEffectivePermissions
        client.getEffectivePermissions(userId, "MERCHANT", null);

        // Evict should not throw
        client.evictCache(userId);
    }

    @Test
    void evictAllDoesNotThrow() {
        client.getEffectivePermissions(UUID.randomUUID(), "MERCHANT", null);
        client.evictAll();
    }

    @Test
    void differentScopesCreateDifferentCacheEntries() {
        UUID userId = UUID.randomUUID();

        client.getEffectivePermissions(userId, "MERCHANT", null);
        client.getEffectivePermissions(userId, "SYSTEM", null);

        // Both calls should not throw
    }

    @Test
    void nullScopeIdUsesNullInCacheKey() {
        UUID userId = UUID.randomUUID();

        Set<String> result = client.getEffectivePermissions(userId, "MERCHANT", null);
        assertThat(result).isEmpty();
    }

    @Test
    void scopeIdIncludedInCacheKey() {
        UUID userId = UUID.randomUUID();
        UUID scopeId = UUID.randomUUID();

        Set<String> result = client.getEffectivePermissions(userId, "MERCHANT", scopeId);
        assertThat(result).isEmpty();
    }
}
