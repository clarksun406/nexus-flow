package com.nexusflow.permission.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

@Slf4j
public class PermissionClient {

    private final PermissionClientProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Cache<String, Set<String>> cache;

    public PermissionClient(PermissionClientProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(properties.getCacheMaxSize())
                .build();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean check(UUID userId, String permission, String scopeType, UUID scopeId) {
        if (!properties.isEnabled()) {
            return true;
        }

        Set<String> effective = getEffectivePermissions(userId, scopeType, scopeId);
        return effective.contains(permission);
    }

    public Set<String> getEffectivePermissions(UUID userId, String scopeType, UUID scopeId) {
        String cacheKey = userId + ":" + scopeType + ":" + (scopeId != null ? scopeId : "null");
        return cache.get(cacheKey, key -> loadEffectivePermissions(userId, scopeType, scopeId));
    }

    public void evictCache(UUID userId) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(userId.toString()));
    }

    public void evictAll() {
        cache.invalidateAll();
    }

    /**
     * Looks up a role by its code via the permission server's role list API.
     * Returns the role's UUID if found, empty otherwise.
     */
    public Optional<UUID> getRoleByCode(String roleCode) {
        try {
            String url = properties.getServerUrl() + "/api/v1/role/list";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getServiceToken())
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<?> roles = objectMapper.readValue(response.body(), List.class);
                for (Object obj : roles) {
                    if (obj instanceof Map<?, ?> role) {
                        if (roleCode.equals(role.get("code"))) {
                            Object id = role.get("id");
                            if (id instanceof String s) return Optional.of(UUID.fromString(s));
                        }
                    }
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to lookup role by code '{}': {}", roleCode, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Grants a role to a user in the given scope via the permission server.
     * Idempotent — if the mapping already exists, the server returns the existing one.
     *
     * @return true if the call succeeded (201 or 409), false on error
     */
    public boolean grantRole(UUID userId, UUID roleId, String scopeType, UUID scopeId) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("userId", userId.toString());
            body.put("roleId", roleId.toString());
            body.put("scopeType", scopeType);
            if (scopeId != null) {
                body.put("scopeId", scopeId.toString());
            }

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getServerUrl() + "/api/v1/user/grant-role"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getServiceToken())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201 || response.statusCode() == 409) {
                return true;
            }
            log.warn("grantRole returned {}: {}", response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            log.error("Failed to grant role {} to user {}: {}", roleId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether a user has any roles in the given scope.
     * Used to detect whether provisioning is needed.
     */
    public boolean hasRoles(UUID userId, String scopeType) {
        try {
            String url = properties.getServerUrl() + "/api/v1/user/" + userId + "/roles"
                    + "?scopeType=" + scopeType;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getServiceToken())
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<?> roles = objectMapper.readValue(response.body(), List.class);
                return !roles.isEmpty();
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to check roles for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> loadEffectivePermissions(UUID userId, String scopeType, UUID scopeId) {
        try {
            String url = properties.getServerUrl() + "/api/v1/permission/eff"
                    + "?userId=" + userId
                    + "&scopeType=" + scopeType;
            if (scopeId != null) {
                url += "&scopeId=" + scopeId;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getServiceToken())
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
                Object perms = body.get("permissions");
                if (perms instanceof List<?> list) {
                    return new HashSet<>(list.stream().map(Object::toString).toList());
                }
            }

            log.warn("Permission service returned {}: {}", response.statusCode(), response.body());
            return Set.of();

        } catch (Exception e) {
            log.error("Failed to load permissions from service: {}", e.getMessage());
            return Set.of();
        }
    }
}
